package com.yoavst.jeb.plugins.constarg

import java.io.File
import java.util.*

/**
 * File format is very simple
 * every line is of the form:
 *     TARGET SIGNATURE CONST_ARG_INDEX [custom script path] [renamed argument index]
 * where target in {Class, Method, Argument, Assignee, Custom}
 *       signature is a dex method signature. for example: "Lcom/yoavst/test/TestClass;->doStuff(Ljava/lang/String;)Ljava/lang/String;"
 *
 * you can use # for comments
 */
object RenameSignaturesFileParser {
    fun parseSignatures(signatures: String, basePath: String): Map<String, ExtendedRenamer> = signatures.lineSequence().map {
        it.substringBefore("#").trim()
    }.filter(String::isNotEmpty).associate {
        val split = it.split(" ", limit = 5)
        if (split.size == 1) {
            throw IllegalArgumentException("Invalid line: '$it'")
        }
        val constArgIndex = split[2].toIntOrNull() ?: throw IllegalArgumentException("Invalid line: '$it'")
        val target = RenameTarget.valueOf(
            split[0].lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        split[1] to when (target) {
            RenameTarget.Class -> ExtendedRenamer(constArgIndex, classRenamer)
            RenameTarget.Method -> ExtendedRenamer(constArgIndex, methodRenamer)
            RenameTarget.Argument -> {
                if (split.size != 4) {
                    throw IllegalArgumentException("Invalid line, no renamed argument index: '$it'")
                }
                val renamedArgIndex =
                    split[3].toIntOrNull() ?: throw IllegalArgumentException("Invalid line, renamed argument is not int: '$it'")
                ExtendedRenamer(constArgIndex, argumentRenamer, renamedArgIndex)
            }
            RenameTarget.Assignee -> ExtendedRenamer(constArgIndex, assigneeRenamer)
            RenameTarget.Custom -> {
                if (split.size < 4) {
                    throw IllegalArgumentException("Invalid line, no custom path: '$it'")
                }
                val filename = split[3]
                val script = if (filename.startsWith("jar:")) {
                    javaClass.classLoader.getResourceAsStream(filename.substringAfter("jar:"))?.bufferedReader()?.readText() ?: run {
                        throw IllegalArgumentException("Invalid line, no such file in jar: '$it'")
                    }
                } else {
                    File(basePath, split[3]).readText()
                }
                if (split.size == 5) {
                    val renamedArgIndex =
                        split[4].toIntOrNull() ?: throw IllegalArgumentException("Invalid line, renamed argument is not int: '$it'")
                    ExtendedRenamer(constArgIndex, scriptRenamer(script), renamedArgIndex)
                } else {
                    ExtendedRenamer(constArgIndex, scriptRenamer(script))
                }
            }
        }
    }
}