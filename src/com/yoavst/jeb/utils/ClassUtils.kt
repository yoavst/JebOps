package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.util.logging.GlobalLog

private object ClassUtils

private val logger = GlobalLog.getLogger(ClassUtils::class.java)

fun IDexUnit.subclassesOf(classSignature: String): Sequence<IDexClass> {
    val classesWithGivenName = types.filter { it.signature == classSignature }
    if (classesWithGivenName.isEmpty()) {
        logger.error("Failed to find class with the given signature: $classSignature")
        return emptySequence()
    } else if (classesWithGivenName.size != 1) {
        logger.error("Multiple class with the given signature: ${classesWithGivenName.joinToString()}")
        return emptySequence()
    }

    return classes.asSequence().filter { classesWithGivenName == it.supertypes }
}

fun Regex.matches(cls: IDexClass): Boolean = matches(cls.signature)