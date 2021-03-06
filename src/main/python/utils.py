from java.lang import Class, Runnable


def get_object(name, loader):
    """Get kotlin object for the given using the given loader"""
    return Class.forName(name, True, loader).getField("INSTANCE").get(None)


def launch_plugin(plugin, loader):
    UIUtils = get_object("com.yoavst.jeb.utils.script.UIUtils", loader)

    class RunnableClass(Runnable):
        def run(self):
            opts = UIUtils.openOptionsForPlugin(plugin)
            if not opts:
                print "Plugin opening aborted"
                return
            print opts
            # instance.execute(ctx.getEnginesContext())
            loader.close()

    UIUtils.runOnMainThread(RunnableClass())
