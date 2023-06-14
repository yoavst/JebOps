package com.yoavst.jeb.utils.script

import com.pnfsoftware.jeb.core.IEnginesContext
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.yoavst.jeb.utils.BasicEnginesPlugin
import org.eclipse.swt.widgets.Display

object UIUtils {
    private val logger = GlobalLog.getLogger(UIUtils::class.java)

    private const val OptionsDialogClass = "com.pnfsoftware.jeb.rcpclient.dialogs.OptionsForEnginesPluginDialog"

    fun openOptionsForPlugin(plugin: BasicEnginesPlugin, ctx: IEnginesContext): Map<String, String>? {
        val shell = Display.getDefault()?.activeShell
        if (shell == null) {
            logger.error("No available SWT shells, cannot open options dialog.")
            return null
        }
        try {
            val dialogCls = Class.forName(OptionsDialogClass)
            val dialog = dialogCls.constructors[0].newInstance(shell, ctx, plugin)
            @Suppress("UNCHECKED_CAST")
            return (dialogCls.getMethod("open").invoke(dialog) as? Map<String, String>) ?: emptyMap()
        } catch (e: ClassNotFoundException) {
            logger.catching(e, "Dialog class is not available in current version of JEB")
        } catch (e: ReflectiveOperationException) {
            logger.catching(e, "Could not initiate the options dialog. Probably unsupported jeb version")
        } catch (e: IllegalArgumentException) {
            try {
                val dialogCls = Class.forName(OptionsDialogClass)
                val dialog = dialogCls.constructors[0].newInstance(shell, plugin)
                @Suppress("UNCHECKED_CAST")
                return (dialogCls.getMethod("open").invoke(dialog) as? Map<String, String>) ?: emptyMap()
            } catch (e: ClassNotFoundException) {
                logger.catching(e, "Dialog class is not available in current version of JEB")
            } catch (e: ReflectiveOperationException) {
                logger.catching(e, "Could not initiate the options dialog. Probably unsupported jeb version")
            }
        }
        return null
    }

    fun runOnMainThread(runnable: Runnable) = Display.getDefault().asyncExec(runnable)
}