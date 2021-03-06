package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.java.IJavaCall

fun IJavaCall.getRealArgument(pos: Int) = if (isStaticCall) getArgument(pos) else getArgument(pos + 1)