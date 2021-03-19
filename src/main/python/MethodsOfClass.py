# ?description=get all methods of specific class
import sys
import traceback
from com.pnfsoftware.jeb.client.api import IScript, IGraphicalClientContext
from com.pnfsoftware.jeb.core import RuntimeProjectUtil
from com.pnfsoftware.jeb.core.units.code.android import IDexUnit
from com.pnfsoftware.jeb.core.units.code.android.dex import IDexMethod


class MethodsOfClass(IScript):
    def run(self, ctx):
        assert isinstance(ctx, IGraphicalClientContext)
        class_name = ctx.displayQuestionBox("Get class methods", "What is the class?", "")
        if not class_name or not class_name.strip():
            return
        filter_text = ctx.displayQuestionBox("Get class methods", "The method Should contain", "").strip() or ""

        try:
            unit = RuntimeProjectUtil.findUnitsByType(ctx.getEnginesContext().getProjects()[0], IDexUnit, False)[0]
            assert isinstance(unit, IDexUnit)
            for method in unit.getMethods():
                assert isinstance(method, IDexMethod)
                if method.getSignature().startswith(class_name) and filter_text in method.getSignature():
                    print method.getSignature()
        except:
            traceback.print_exc(file=sys.stdout)
