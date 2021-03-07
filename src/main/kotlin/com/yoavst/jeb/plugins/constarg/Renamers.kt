package com.yoavst.jeb.plugins.constarg

import org.python.util.PythonInterpreter

private const val CLASS = "cls"
private const val METHOD = "method"
private const val ARGUMENT = "argument"
private const val ASSIGNEE = "assignee"
private const val INPUT = "tag"

val classRenamer: (String) -> RenameResult = { RenameResult(className = it) }
val methodRenamer: (String) -> RenameResult = { RenameResult(methodName = it) }
val argumentRenamer: (String) -> RenameResult = { RenameResult(argumentName = it) }
val assigneeRenamer: (String) -> RenameResult = { RenameResult(assigneeName = it) }
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
        baseInterpreter[CLASS] = null
        baseInterpreter[METHOD] = null
        baseInterpreter[ARGUMENT] = null
        baseInterpreter[ASSIGNEE] = null
        baseInterpreter[INPUT] = tag
        baseInterpreter.exec(script)
        RenameResult(
            baseInterpreter[CLASS]?.asStringOrNull(),
            baseInterpreter[METHOD]?.asStringOrNull(),
            baseInterpreter[ARGUMENT]?.asStringOrNull(),
            baseInterpreter[ASSIGNEE]?.asStringOrNull()
        )
    }

}