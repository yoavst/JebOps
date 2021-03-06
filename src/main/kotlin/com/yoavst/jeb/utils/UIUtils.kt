package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.util.logging.GlobalLog
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.FileDialog

private class UIUtils

private val logger = GlobalLog.getLogger(UIUtils::class.java)

fun displayFileOpenSelector(caption: String): String? {
    val shell = Display.getDefault()?.activeShell
    if (shell == null) {
        logger.error("No available SWT shells, cannot open file dialog.")
        return null
    }

    val dlg = FileDialog(shell, 4096)
    dlg.text = caption.orIfBlank("Open a file...")
    return dlg.open()

}