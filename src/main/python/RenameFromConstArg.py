#?description=Launch the already loaded const arg rename plugin
#?shortcut=Mod1+Mod3+L
import sys
import traceback
from com.pnfsoftware.jeb.client.api import IScript, IGraphicalClientContext
from com.pnfsoftware.jeb.core import IEnginesPlugin

import utils


class RenameFromConstArg(IScript):
    def run(self, ctx):
        assert isinstance(ctx, IGraphicalClientContext)
        try:
            plugins = ctx.getEnginesContext().getEnginesPlugins()
            for plugin in plugins:
                assert isinstance(plugin, IEnginesPlugin)
                if 'ConstArgRenamingPlugin' == plugin.getClass().getSimpleName():
                    classloader = plugin.getClass().getClassLoader()

                    # 1. update ui bridge
                    UIBridge = utils.get_object("com.yoavst.jeb.bridge.UIBridge", classloader)
                    UIBridge.update(ctx)

                    # 2. Launch plugin
                    utils.launch_plugin(plugin, ctx, classloader, close_loader=False)
                    break
        except:
            traceback.print_exc(file=sys.stdout)
