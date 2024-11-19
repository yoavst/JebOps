package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger

fun ILogger.enableAllLogs(): ILogger = apply {
    enabledLevel = GlobalLog.LEVEL_ALL
}