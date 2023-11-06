package com.yoavst.jeb.plugins.constarg

data class RenameResult(
        val className: String? = null,
        val methodName: String? = null,
        val argumentName: String? = null,
        val assigneeName: String? = null
)
