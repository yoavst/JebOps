import sys
import traceback
from com.pnfsoftware.jeb.client.api import IScript, IClientContext
from com.pnfsoftware.jeb.core import AbstractEnginesPlugin
from java.io import File
from java.lang import Class
from java.net import URLClassLoader

import utils
from debug import CLASS, JAR_PATH


class JarLoader(IScript):
    def run(self, ctx):
        try:
            assert isinstance(ctx, IClientContext)
            loader = URLClassLoader([File(JAR_PATH).toURI().toURL()], self.getClass().getClassLoader())
            instance = Class.forName(CLASS, True, loader).newInstance()
            assert isinstance(instance, AbstractEnginesPlugin)

            # 1. update ui bridge
            UIBridge = utils.get_object("com.yoavst.jeb.bridge.UIBridge", loader)
            UIBridge.update(ctx)

            # 2. Launch plugin
            utils.launch_plugin(instance, loader)
            return
        except:
            traceback.print_exc(file=sys.stdout)
