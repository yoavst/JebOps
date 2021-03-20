#?description=update the UIBridge class of JebPlugin
#?shortcut=Mod1+Mod3+U
import sys
import traceback
from com.pnfsoftware.jeb.client.api import IScript, IGraphicalClientContext
from com.pnfsoftware.jeb.core import IEnginesPlugin

import utils


class UIBridge(IScript):
    def run(self, ctx):
        assert isinstance(ctx, IGraphicalClientContext)
        try:
            plugins = ctx.getEnginesContext().getEnginesPlugins()
            for plugin in plugins:
                assert isinstance(plugin, IEnginesPlugin)
                if 'yoav' in plugin.getClass().getCanonicalName():
                    classloader = plugin.getClass().getClassLoader()

                    print "Updating UI bridge"
                    UIBridge = utils.get_object("com.yoavst.jeb.bridge.UIBridge", classloader)
                    UIBridge.update(ctx)
                    break
        except:
            traceback.print_exc(file=sys.stdout)
