import sys
import traceback
from com.pnfsoftware.jeb.client.api import IScript, IGraphicalClientContext
from com.pnfsoftware.jeb.core import RuntimeProjectUtil
from com.pnfsoftware.jeb.core.actions import ActionXrefsData, ActionContext, Actions
from com.pnfsoftware.jeb.core.units.code.android import IDexUnit
from com.pnfsoftware.jeb.core.units.code.java import IJavaAssignment, ICompound, IJavaCall, IJavaConstant, \
    IJavaStaticField, IJavaReturn, IJavaArithmeticExpression
from com.pnfsoftware.jeb.core.util import DecompilerHelper


class Phenotype(IScript):
    def run(self, ctx):
        assert isinstance(ctx, IGraphicalClientContext)
        class_name = ctx.displayQuestionBox("Phenotype helper", "What is the class name for the phenotype flag builder?", "")
        if not class_name or not class_name.strip():
            return
        elif not class_name.startswith("L"):
            class_name = "L" + class_name.replace(".", "/") + ";"
            print "Class name not in dex format, trying:", class_name

        try:
            unit = RuntimeProjectUtil.findUnitsByType(ctx.enginesContext.projects[0], IDexUnit, False)[0]
            assert isinstance(unit, IDexUnit)

            init_methods = Phenotype.detect_init_methods(unit, class_name)
            if not init_methods:
                return
            print "Found", len(init_methods), "flag initialization methods"

            fields = Phenotype.detect_fields(unit, init_methods)
            print "Found", len(fields), "flag fields"

            first_getters = Phenotype.detect_first_getters(unit, fields)
            print "Found", len(first_getters), "getters"

            secondary_getters = Phenotype.detect_second_getters(unit, first_getters)
            print "Found", len(secondary_getters), "secondary getters"
        except:
            traceback.print_exc(file=sys.stdout)

    @staticmethod
    def detect_init_methods(unit, class_name):
        phenotype_builder_cls = unit.getClass(class_name)

        if not phenotype_builder_cls:
            print "Class not found!"
            return

        if '_' not in phenotype_builder_cls.getName(True):
            phenotype_builder_cls.name = phenotype_builder_cls.getName(False) + "_" + "PhenotypePackage"

        init_methods = []
        for method in phenotype_builder_cls.methods:
            types_of_params = method.parameterTypes
            if "<" not in method.name and len(types_of_params) >= 2 and types_of_params[0].name == "String":
                new_name = method.getName(False) + "_" + \
                           str(BasicTypeMap.get(types_of_params[1].name, types_of_params[1].name)).lower() + "Flag"
                if new_name != method.getName(True):
                    method.name = new_name
                init_methods.append(method)

        return init_methods

    @staticmethod
    def detect_fields(unit, init_methods):
        using_methods = set()
        for method in init_methods:
            for xref in xrefs_for(unit, method):
                using_methods.add(unit.getMethod(xref))

        signatures = {method.getSignature(False) for method in init_methods}

        decompiler = DecompilerHelper.getDecompiler(unit)
        fields = {}
        for method in using_methods:
            cls = method.classType.implementingClass
            decompiled_method = decompile_dex_method(decompiler, method)
            if not decompiled_method:
                continue

            def handle_non_compound(statement):
                if isinstance(statement, IJavaAssignment):
                    left, right = statement.left, statement.right
                    if isinstance(right, IJavaCall):
                        if right.method.signature in signatures:
                            name_element = right.getArgument(1)
                            if isinstance(name_element, IJavaConstant) and name_element.isString():
                                name = name_element.getString()
                                str_name = name.replace(":", "_")
                                if isinstance(left, IJavaStaticField):
                                    field = left.field
                                    dex_field = cls.getField(False, field.name, field.type.signature)
                                    new_name = dex_field.getName(False) + "_" + str_name
                                    if new_name != dex_field.getName(True):
                                        dex_field.name = new_name

                                    fields[str_name] = dex_field

            AstTraversal(handle_non_compound).traverse_block(decompiled_method.body)
        return fields

    @staticmethod
    def detect_first_getters(unit, fields):
        getters = {}
        decompiler = DecompilerHelper.getDecompiler(unit)
        for flag_name, field in fields.iteritems():
            sig = field.getSignature(False)
            for xref in xrefs_for(unit, field):
                method = unit.getMethod(xref)
                if method.data and method.data.codeItem and \
                        method.data.codeItem.instructions and method.data.codeItem.instructions.size() <= 20:
                    decompiled_method = decompile_dex_method(decompiler, method)
                    if not decompiled_method or decompiled_method.body.size() != 1:
                        continue

                    statement = decompiled_method.body.get(0)
                    if isinstance(statement, IJavaReturn):
                        last_call = extract_last_function_call(statement.expression)
                        if last_call and len(last_call.arguments) == 1:
                            src = last_call.getArgument(0)
                            if isinstance(src, IJavaStaticField) and src.field.signature == sig:
                                new_name = method.getName(False) + "_" + flag_name
                                if new_name != method.getName(True):
                                    method.name = new_name
                                getters[flag_name] = method
        return getters

    @staticmethod
    def detect_second_getters(unit, getters):
        secondary_getters = {}
        decompiler = DecompilerHelper.getDecompiler(unit)
        for flag_name, getter_method in getters.iteritems():
            sig = getter_method.getSignature(False).split(';->')[1]

            for xref in xrefs_for(unit, getter_method):
                method = unit.getMethod(xref)
                if method.getSignature(False) == sig:
                    continue

                if method.data and method.data.codeItem and \
                        method.data.codeItem.instructions and method.data.codeItem.instructions.size() <= 20:
                    decompiled_method = decompile_dex_method(decompiler, method)
                    if not decompiled_method or decompiled_method.body.size() != 1:
                        continue

                    statement = decompiled_method.body.get(0)
                    if isinstance(statement, IJavaReturn):
                        last_call = extract_last_function_call(statement.expression)
                        if last_call and len(last_call.arguments) <= 1:
                            if last_call.methodSignature.split(';->')[1] == sig:
                                new_name = method.getName(False) + "_" + flag_name
                                if new_name != method.getName(True):
                                    method.name = new_name
                                secondary_getters[flag_name] = method
        return secondary_getters

# region Utils
class AstTraversal(object):
    def __init__(self, traverse_non_compound):
        self.traverse_non_compound = traverse_non_compound

    def traverse_block(self, block):
        for i in xrange(0, block.size()):
            self.traverse_statement(block.get(i))

    def traverse_statement(self, statement):
        if isinstance(statement, ICompound):
            for block in statement.blocks:
                self.traverse_block(block)
        else:
            self.traverse_non_compound(statement)


def extract_last_function_call(exp):
    if isinstance(exp, IJavaCall):
        name = exp.methodName
        if name == "booleanValue":
            return extract_last_function_call(exp.getArgument(0))
        return exp
    elif isinstance(exp, IJavaArithmeticExpression):
        if exp.operator.isCast():
            return extract_last_function_call(exp.right)


def xrefs_for(unit, item):
    data = ActionXrefsData()
    if unit.prepareExecution(ActionContext(unit, Actions.QUERY_XREFS, item.itemId, item.address), data):
        return data.addresses

    return []


def decompile_dex_method(decompiler, method):
    if not decompiler.decompileMethod(method.signature):
        print "Failed to decompile", method.currentSignature
        return

    return decompiler.getMethod(method.signature, False)


BasicTypeMap = {
    'C': 'char',
    'B': 'byte',
    'D': 'double',
    'F': 'float',
    'I': 'int',
    'J': 'long',
    'L': 'ClassName',
    'S': 'short',
    'Z': 'boolean',
    'V': 'void',
}
# endregion
