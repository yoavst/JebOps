package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaCall

fun IJavaCall.getRealArgument(pos: Int) = if (isStaticCall) getArgument(pos) else getArgument(pos + 1)

val IDexMethod.isStatic: Boolean
    get() = (data.accessFlags and IDexUnit.ACC_STATIC) != 0