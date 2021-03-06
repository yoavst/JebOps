package com.yoavst.jeb.plugins.constarg

import org.python.util.PythonInterpreter

private const val CLASS = "cls"
private const val METHOD = "method"
private const val ARGUMENT = "argument"
private const val ASIGNEE = "assignee"
private const val INPUT = "tag"

val classRenamer: (String) -> RenameResult = { RenameResult(className = it) }
val methodRenamer: (String) -> RenameResult = { RenameResult(methodName = it) }
val argumentRenamer: (String) -> RenameResult = { RenameResult(argumentName = it) }
val asigneeRenamer: (String) -> RenameResult = { RenameResult(assigneeName = it) }
fun scriptRenamer(script: String): (String) -> RenameResult {
    val baseInterpreter = PythonInterpreter().apply {
        exec(
            """
        def split2(s, sep, at_least):
            '''Split the string using separator. Result array will be at least the given length.'''
            arr = s.split(sep)
            return arr + [""]*(at_least-len(arr))
    """.trimIndent()
        )
    }
    return { tag ->
        baseInterpreter.set(CLASS, null)
        baseInterpreter.set(METHOD, null)
        baseInterpreter.set(ARGUMENT, null)
        baseInterpreter.set(ASIGNEE, null)
        baseInterpreter.set(INPUT, tag)
        baseInterpreter.exec(script)
        RenameResult(
            baseInterpreter.get(CLASS).asStringOrNull(),
            baseInterpreter.get(METHOD).asStringOrNull(),
            baseInterpreter.get(ARGUMENT).asStringOrNull(),
            baseInterpreter.get(ASIGNEE).asStringOrNull()
        )
    }

}