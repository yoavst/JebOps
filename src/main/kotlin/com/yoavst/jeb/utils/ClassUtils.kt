package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.util.logging.GlobalLog
import java.util.stream.Stream

private object ClassUtils

private val logger = GlobalLog.getLogger(ClassUtils::class.java)

fun IDexUnit.classBySignature(classSignature: String) = classes.firstOrNull { it.currentSignature == classSignature }

fun IDexUnit.subclassesOf(classSignature: String): Stream<out IDexClass> {
    val classesWithGivenName = types.filter { it.signature == classSignature }
    if (classesWithGivenName.isEmpty()) {
        logger.error("Failed to find class with the given signature: $classSignature")
        return Stream.empty()
    } else if (classesWithGivenName.size != 1) {
        logger.error("Multiple class with the given signature: ${classesWithGivenName.joinToString()}")
        return Stream.empty()
    }

    return classes.parallelStream().filter { classesWithGivenName == it.supertypes }
}


fun IDexClass.isSubclassOf(unit: IDexUnit, classSignature: String): Boolean {
    val classesWithGivenName = unit.types.filter { it.signature == classSignature }
    if (classesWithGivenName.isEmpty()) {
        logger.error("Failed to find class with the given signature: $classSignature")
        return false
    }
    return supertypes == classesWithGivenName
}

fun Regex.matches(cls: IDexClass): Boolean = matches(cls.signature)