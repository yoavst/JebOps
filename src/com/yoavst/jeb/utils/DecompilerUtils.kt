package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaMethod
import com.pnfsoftware.jeb.core.util.DecompilerHelper
import com.pnfsoftware.jeb.util.logging.GlobalLog

private object DecompilerUtils

private val logger = GlobalLog.getLogger(DecompilerUtils::class.java)

val IDexUnit.decompiler: IDexDecompilerUnit get() = DecompilerHelper.getDecompiler(this) as IDexDecompilerUnit

fun IDexDecompilerUnit.decompileDexMethod(method: IDexMethod): IJavaMethod? {
    if (!decompileMethod(method.signature)) {
        logger.warning("Failed to decompile ${method.currentSignature}")
        return null
    }

    return getMethod(method.signature, false)
}