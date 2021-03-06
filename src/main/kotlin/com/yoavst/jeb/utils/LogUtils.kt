package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger
import java.io.File
import java.io.PrintStream

fun ILogger.enableAllLogs(): ILogger = apply {
    enabledLevel = GlobalLog.LEVEL_ALL
}

private var hasRunLogToFile = false
fun logToFile() {
    if (!hasRunLogToFile)
        GlobalLog.addDestinationStream(PrintStream(File("D:\\Documents\\Git\\JebPlugin\\test.txt")))

    hasRunLogToFile = true
}