package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.util.logging.GlobalLog

private object ClassUtils

private val logger = GlobalLog.getLogger(ClassUtils::class.java)

fun IDexUnit.subclassesOf(classSignature: String): List<IDexClass> {
    val classesWithGivenName = types.filter { it.signature == classSignature }
    if (classesWithGivenName.isEmpty()) {
        logger.error("Failed to find class with the given signature: $classSignature")
        return emptyList()
    } else if (classesWithGivenName.size != 1) {
        logger.error("Multiple class with the given signature: ${classesWithGivenName.joinToString()}")
        return emptyList()
    }

    return classes.filter { classesWithGivenName == it.supertypes }
}

fun IDexClass.matches(regex: Regex): Boolean = regex.matches(signature)