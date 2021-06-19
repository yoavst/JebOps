package com.yoavst.jeb.plugins.kotlin

import com.yoavst.jeb.plugins.constarg.ExtendedRenamer

data class IntrinsicDetectionResult(val name: String, val renamer: ExtendedRenamer? = null)
