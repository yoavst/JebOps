from com.pnfsoftware.jeb.client.api import IScript, IClientContext

GRAPHICAL_CONTEXT = "graphical_context_cache"

class ContextCacheScript(IScript):
    def run(self, ctx):
        assert isinstance(ctx, IClientContext)
        proj = ctx.getMainProject()
        if not proj:
            print "Error: you should load a project, so the script could cache the context inside a project"
            return
        proj.setData(GRAPHICAL_CONTEXT, ctx, False)
        print "Done! now you can run the plugin with context."
