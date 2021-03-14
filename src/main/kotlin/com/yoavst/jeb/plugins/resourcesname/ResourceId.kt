package com.yoavst.jeb.plugins.resourcesname

data class ResourceId(val name: String, val value: Int, val type: String, val isSystem: Boolean = false) {
    fun toField(packagePrefix: String) = "${if (isSystem) ANDROID_PREFIX else packagePrefix}R\$$type;->$name:I"
    fun toClass(packagePrefix: String) = "${if (isSystem) ANDROID_PREFIX else packagePrefix}R\$$type;"

    companion object {
        private const val ANDROID_PREFIX = "Landroid/"
    }
}