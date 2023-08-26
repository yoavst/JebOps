#?type=dexdec-ir
from com.pnfsoftware.jeb.core.units.code.android.ir import AbstractDOptimizer, IDVisitor, DUtil, DOpcodeType
from java.lang import Integer

class DPropagation(AbstractDOptimizer):  
    def perform(self):
        cnt = 0
        self.insns_to_delete = []

        scan_result = list(self.scan())
        if scan_result:
            cnt += self.replace(scan_result)

        # Delete broken stuff
        for insn in self.cfg.instructions():   
            if insn.isAssign():
                if insn.assignDestination == insn.assignSource:
                    self.insns_to_delete.append(insn)
                    
        if self.insns_to_delete:
            for insn in self.insns_to_delete:
                insn.transformToNop()
            cnt += len(self.insns_to_delete)
        return cnt

           
    def scan(self):
        variables_union_find = UnionFind()
        variable_assignments = dict()

        # Step 0: Collect function args
        for reg, reg_type in dict(self.ctx.parametersTypeMap).items():
            if reg != -1:
                variable_assignments.setdefault(self.ctx.createRegisterVar(reg, reg_type), []).append(None) 

        # Step 1: Scan all the instructions for identifier assignments
        for insn in self.cfg.instructions():    
            if insn.isAssign():   
                dest, src = insn.assignDestination, insn.assignSource     
                if dest.isVar():
                    if src.isVar():
                        variables_union_find.union(dest, src)
                    else:
                        variable_assignments.setdefault(dest, []).append(src) 
            
            elif insn.isStoreException():
                identifier = insn.definedIdentifier
                variable_assignments.setdefault(identifier, []).append(insn)

        # Step 2: Collect them to groups
        groups = dict() # Root -> ([elements], [assignments])
        for identifier in variables_union_find:
                root = variables_union_find[identifier]
                elements, assignments = groups.setdefault(root, ([], []))
                elements.append(identifier)
                assignments.extend(variable_assignments.setdefault(identifier, []))
        
        # Step 3: Check whether they are equivalents
        for _, (elements, assignments) in groups.iteritems():
            # as long as there is at most a single assignment, we can make all those variables the same variable
            # and maybe even constant
            if len(elements) > 1 and len(assignments) <= 1:
                if not assignments:
                    print "Should not be here", elements
                    # merge the variables
                    yield elements, None
                else:
                    assignment = assignments[0]
                    if assignment is None:
                        yield elements, None
                    elif assignment.isImm():
                        # Can replace with constant
                        yield elements, assignment
                    else:
                        # Just merge the variables
                        # **Note:**, this is unsafe as there are side effects, so here is a heroistic that should remove the most common case
                        # TODO talk about it with someone, to see if we can define it better
                        visitor = VariablesCollectorVisitor()
                        assignment.visitDepthPost(visitor)
                        if not (set(elements) & visitor.variables):
                            yield elements, None

    def replace(self, scan_result):   
        # create our instruction visitor
        vis = ReplacementVisitor(scan_result)
        # visit all the instructions of the IR CFG
        for insn in self.cfg.instructions():
            insn.visitInstructionPreOrder(vis, False)
        # return the count of replacements
        return vis.cnt
 
class ReplacementVisitor(IDVisitor):
            def __init__(self, groups):
                self.groups = groups
                self.cnt = 0
                self.replacements_cache = dict()
                self.assignments_to_delete = []

            def process(self, e, parent, results):
                if e.isVar():
                    replacement = self.get_replacement(e)
                    if replacement and parent.replaceSubExpression(e, replacement):
                        # success (this visitor is pre-order, we need to report the replaced node)
                        results.setReplacedNode(replacement)
                        self.cnt += 1

                

            def get_replacement(self, var):
                if var in self.replacements_cache:
                    return self.replacements_cache[var]

                for elements, const in self.groups:
                    if var in elements:
                        # TODO improve this
                        # choose the identifier with the definition with the lower address
                        if const:
                            self.replacements_cache[var] = const
                            return const
                        
                        replacement = min(elements, key=lambda v: v.id)
                        result = None if replacement == var else replacement
                        self.replacements_cache[var] = result
                        return result
                return None

class VariablesCollectorVisitor(IDVisitor):
    def __init__(self):
        self.variables = set()

    def process(self, e, parent, results):
        if e.isVar():
            self.variables.add(e.id)


# https://gist.github.com/AntiGameZ/67124a149d4c1d41e20ee82ba2cfdbe7
class UnionFind(object):
    """Union-find data structure.
    Each unionFind instance X maintains a family of disjoint sets of
    hashable objects, supporting the following two methods:
    - X[item] returns a name for the set containing the given item.
      Each set is named by an arbitrarily-chosen one of its members; as
      long as the set remains unchanged it will keep the same name. If
      the item is not yet part of a set in X, a new singleton set is
      created for it.
    - X.union(item1, item2, ...) merges the sets containing each item
      into a single larger set.  If any item is not yet part of a set
      in X, it is added to X as one of the members of the merged set.
    """

    def __init__(self):
        """Create a new empty union-find structure."""
        self.weights = {}
        self.parents = {}

    def __getitem__(self, object):
        """Find and return the name of the set containing the object."""

        # check for previously unknown object
        if object not in self.parents:
            self.parents[object] = object
            self.weights[object] = 1
            return object

        # find path of objects leading to the root
        path = [object]
        root = self.parents[object]
        while root != path[-1]:
            path.append(root)
            root = self.parents[root]

        # compress the path and return
        for ancestor in path:
            self.parents[ancestor] = root
        return root
        
    def __iter__(self):
        """Iterate through all items ever found or unioned by this structure."""
        return iter(self.parents)

    def union(self, *objects):
        """Find the sets containing the objects and merge them all."""
        roots = [self[x] for x in objects]
        heaviest = max([(self.weights[r],r) for r in roots])[1]
        for r in roots:
            if r != heaviest:
                self.weights[heaviest] += self.weights[r]
                self.parents[r] = heaviest