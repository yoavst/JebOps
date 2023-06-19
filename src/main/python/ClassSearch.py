from com.pnfsoftware.jeb.client.api import IScript, IGraphicalClientContext
from com.pnfsoftware.jeb.core.units.code.android import IDexUnit
from com.pnfsoftware.jeb.core import RuntimeProjectUtil
from com.pnfsoftware.jeb.core.units.code.java import IJavaSourceUnit

classes = None
project = None

class ClassSearch(IScript):
    def run(self, ctx):
        global classes, project

        assert isinstance(ctx, IGraphicalClientContext), 'This script must be run within a graphical client'

        project = ctx.getEnginesContext().getProjects()[0]
        unit = RuntimeProjectUtil.findUnitsByType(project, IDexUnit, False)[0]
        assert isinstance(unit, IDexUnit), 'Unit must be IDexUnit'

        headers = ['Name', 'Original name', 'Package']
        rows = []

        classes = unit.classes
        for clazz in classes:
            if clazz.package is None or clazz.package.name is None:
                pkg = '<default>'
            else:
                pkg = clazz.package.getSignature(True)

            rows.append([clazz.getName(True), 'L' + clazz.getName(False) + ';', pkg])

        index = displayListModeless(ctx, 'Classes', 'go to class', headers, rows)
        if index < 0:
            return

        selected_class = classes[index]

        # Help GC
        classes = None

        # Try navigate to java, if fail try disassembly
        java_unit = RuntimeProjectUtil.findUnitsByType(project, IJavaSourceUnit, False).get(0)
        if java_unit is None:
            ctx.navigate(unit, selected_class.getAddress(True))
        else:
            ctx.navigate(java_unit, selected_class.getAddress(True))



# ----------------------------------------------------------------------
from com.pnfsoftware.jeb.rcpclient.dialogs import DataFrameDialog
from com.pnfsoftware.jeb.rcpclient.util import DataFrame

class GotoClassWindow(DataFrameDialog):
    def __init__(self, ctx, title, subtitle):
        DataFrameDialog.__init__(self, None, title, False, None)
        self.ctx = ctx
        self.displayIndex = True
        self.message = subtitle
        if self.hasInstance():
            raise ValueError("Can only have single instance")

    def createButtons(self, composite):
        self.super__createButtons(composite, [0x20, 0x100, 0x30000000], 0x20)
        self.hideButton(0x20)

    def getButtonText(self, v, s):
        if v == 0x100:
            return "Close"
        elif v == 0x30000000:
            return "Navigate"
        else:
            return self.super__getButtonText(v, s)

    def onButtonClick(self, v):
        if v == 0x20:
            self.nav()
            self.super__onButtonClick(0x20)
        elif v == 0x30000000:
            self.nav()
        else:
            self.super__onButtonClick(v)

    def onConfirm(self):
        self.nav()

    def nav(self):
        global classes

        v = self.selectedRow
        if v < 0 or v >= len(classes):
            return

        selected_class = classes[v]

        java_units = RuntimeProjectUtil.findUnitsByType(project, IJavaSourceUnit, False)
        java_unit = java_units[0] if java_units else None
        if java_unit is None:
            unit = RuntimeProjectUtil.findUnitsByType(project, IDexUnit, False).get(0)
            self.ctx.navigate(unit, selected_class.getAddress(True))
        else:
            self.ctx.navigate(java_unit, selected_class.getAddress(True))


def displayListModeless(ctx, title, subtitle, header, values):
    dialog = GotoClassWindow(ctx, title, subtitle)

    frame = DataFrame(header)
    for value in values:
        frame.addRow(value)

    dialog.dataFrame = frame
    return dialog.open()


