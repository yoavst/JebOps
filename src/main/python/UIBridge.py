#?description=update the UIBridge class of JebPlugin
#?shortcut=ctrl+shift+U
import sys
import traceback
from com.pnfsoftware.jeb.client.api import IScript
from com.yoavst.jeb.bridge.UIBridge import INSTANCE as UI_BRIDGE_INSTANCE


class UIBridge(IScript):
    def run(self, ctx):
        try:
            print "Updating UI bridge"
            UI_BRIDGE_INSTANCE.update(ctx)
        except:
            traceback.print_exc(file=sys.stdout)
