package com.yoavst.jeb.utils

fun String?.orIfBlank(other: String) = if (isNullOrBlank()) other else this