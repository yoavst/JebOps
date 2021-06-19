package com.yoavst.jeb.plugins.kotlin

import com.pnfsoftware.jeb.core.units.code.ICodeItem
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.DalvikInstructionOpcodes
import com.pnfsoftware.jeb.core.units.code.android.dex.IDalvikInstruction
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger
import com.yoavst.jeb.plugins.constarg.ExtendedRenamer
import com.yoavst.jeb.plugins.constarg.RenameResult
import com.yoavst.jeb.plugins.constarg.argumentRenamer
import com.yoavst.jeb.utils.originalName
import com.yoavst.jeb.utils.originalSignature

object IntrinsicsMethodDetector {
    private val logger: ILogger = GlobalLog.getLogger(javaClass)

    fun detect(method: IDexMethod, unit: IDexUnit): IntrinsicDetectionResult? {
        // match by args
        return when (method.parameterTypes?.size ?: 0) {
            0 -> detectNoArgs(method, unit)
            1 -> detectOneArg(method, unit)
            2 -> detectTwoArgs(method, unit)
            3 -> detectThreeArgs(method, unit)
            else -> null
        }
    }

    private fun detectNoArgs(method: IDexMethod, unit: IDexUnit): IntrinsicDetectionResult? {
        @Suppress("UNCHECKED_CAST")
        val instructions = method.instructions as List<IDalvikInstruction>
        for (instruction in instructions) {
            if (instruction.opcode == DalvikInstructionOpcodes.OP_INVOKE_DIRECT) {
                val param = instruction.parameters[0]
                if (param.type == IDalvikInstruction.TYPE_IDX) {
                    val calledMethod = unit.getMethod(param.value.toInt())
                    if (calledMethod.originalName == "<init>") {
                        when (calledMethod.classType.originalSignature) {
                            "Ljava/lang/NullPointerException;" -> return IntrinsicDetectionResult("throwJavaNpe")
                            "Ljava/lang/AssertionError;" -> return IntrinsicDetectionResult("throwAssert")
                            "Ljava/lang/IllegalArgumentException;" -> return IntrinsicDetectionResult("throwIllegalArgument")
                            "Ljava/lang/IllegalStateException;" -> return IntrinsicDetectionResult("throwIllegalState")
                        }
                        // check for KotlinNullException
                        if (calledMethod.classType.implementingClass != null &&
                                calledMethod.classType.implementingClass.supertypes.any { superType ->
                                    superType.signature == "Ljava/lang/NullPointerException;"
                                }) {
                            return IntrinsicDetectionResult("throwNpe")
                        }
                    }

                }
            }
        }

        val constStringInst = instructions.firstOrNull { it.opcode == DalvikInstructionOpcodes.OP_CONST_STRING }
        if (constStringInst != null) {
            val param = constStringInst.parameters[1]
            if (param.type == IDalvikInstruction.TYPE_IDX) {
                val str = unit.getString(param.value.toInt()).toString()
                if (str == "This function has a reified type parameter and thus can only be inlined at compilation time, not called directly.") {
                    return IntrinsicDetectionResult("throwUndefinedForReified")
                }
            }
        }
        return null
    }

    private fun detectOneArg(method: IDexMethod, unit: IDexUnit): IntrinsicDetectionResult? {
        when {
            method.parameterTypes[0].originalSignature == "Ljava/lang/Throwable;" -> {
                return IntrinsicDetectionResult("sanitizeStackTrace")
            }
            method.parameterTypes[0].originalSignature == "Ljava/lang/Object;" -> {
                return IntrinsicDetectionResult("checkNotNull")
            }
            method.returnType.originalSignature == "Ljava/lang/String;" -> {
                return IntrinsicDetectionResult("createParameterIsNullExceptionMessage")
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                val instructions = method.instructions as List<IDalvikInstruction>
                for (instruction in instructions) {
                    if (instruction.opcode == DalvikInstructionOpcodes.OP_INVOKE_DIRECT) {
                        val param = instruction.parameters[0]
                        if (param.type == IDalvikInstruction.TYPE_IDX) {
                            val calledMethod = unit.getMethod(param.value.toInt())
                            if (calledMethod.originalName == "<init>") {
                                when (calledMethod.classType.originalSignature) {
                                    "Ljava/lang/UnsupportedOperationException;" -> return IntrinsicDetectionResult("throwUndefinedForReified")
                                    "Ljava/lang/AssertionError;" -> return IntrinsicDetectionResult("throwAssert")
                                    "Ljava/lang/IllegalStateException;" -> return IntrinsicDetectionResult("throwIllegalState")
                                    "Ljava/lang/IllegalArgumentException;" -> {
                                        return if (method.genericFlags and ICodeItem.FLAG_PUBLIC != 0) {
                                            IntrinsicDetectionResult("throwIllegalArgument")
                                        } else {
                                            IntrinsicDetectionResult("throwParameterIsNullIAE")
                                        }
                                    }
                                    "Ljava/lang/NullPointerException;" -> {
                                        return if (method.genericFlags and ICodeItem.FLAG_PUBLIC != 0) {
                                            IntrinsicDetectionResult("throwJavaNpe")
                                        } else {
                                            IntrinsicDetectionResult("throwParameterIsNullNPE")
                                        }
                                    }
                                }
                                if (calledMethod.classType.implementingClass != null) {
                                    if (calledMethod.classType.implementingClass.supertypes.any { superType ->
                                                superType.signature == "Ljava/lang/NullPointerException;"
                                            }) {
                                        return IntrinsicDetectionResult("throwNpe")
                                    } else if (calledMethod.classType.implementingClass.supertypes.any { superType ->
                                                superType.signature == "Ljava/lang/RuntimeException;"
                                            }) {
                                        return IntrinsicDetectionResult("throwUninitializedProperty")
                                    }
                                }

                            }

                        }
                    }
                }

                val constStringInst = instructions.firstOrNull { it.opcode == DalvikInstructionOpcodes.OP_CONST_STRING }
                if (constStringInst != null) {
                    val param = constStringInst.parameters[1]
                    if (param.type == IDalvikInstruction.TYPE_IDX) {
                        val str = unit.getString(param.value.toInt()).toString()
                        if (str == "lateinit property ") {
                            return IntrinsicDetectionResult("throwUninitializedPropertyAccessException")
                        }
                    }
                }
            }
        }
        return null
    }

    private fun detectTwoArgs(method: IDexMethod, unit: IDexUnit): IntrinsicDetectionResult? {
        when (method.returnType.originalSignature) {
            "Z" -> {
                return IntrinsicDetectionResult("areEqual")
            }
            "I" -> {
                return IntrinsicDetectionResult("compare")
            }
            "Ljava/lang/Throwable;" -> {
                return IntrinsicDetectionResult("sanitizeStackTrace")
            }
        }

        when (method.parameterTypes[0].signature) {
            "Ljava/lang/String;" -> {
                when (method.parameterTypes[1].signature) {
                    "Ljava/lang/String;" -> {
                        return IntrinsicDetectionResult("checkHasClass")
                    }
                    "Ljava/lang/Object;" -> {
                        return IntrinsicDetectionResult("stringPlus")
                    }
                }
            }
            "I" -> {
                if (method.parameterTypes[1].signature == "Ljava/lang/String;") {
                    return IntrinsicDetectionResult("reifiedOperationMarker")
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val instructions = method.instructions as List<IDalvikInstruction>
        for (instruction in instructions) {
            if (instruction.opcode == DalvikInstructionOpcodes.OP_INVOKE_DIRECT) {
                val param = instruction.parameters[0]
                if (param.type == IDalvikInstruction.TYPE_IDX) {
                    val calledMethod = unit.getMethod(param.value.toInt())
                    if (calledMethod.originalName == "<init>") {
                        when (calledMethod.classType.originalSignature) {
                            "Ljava/lang/NullPointerException;" -> return IntrinsicDetectionResult("checkNotNullExpressionValue", simpleExpressionRenamer)
                            "Ljava/lang/IllegalStateException;" -> {
                                if (instructions.any { it.opcode == DalvikInstructionOpcodes.OP_CONST_STRING }) {
                                    return IntrinsicDetectionResult("checkExpressionValueIsNotNull", simpleExpressionRenamer)
                                }
                                // else, it is checkReturnedValueIsNotNull or checkFieldIsNotNull, but they have the same impl
                                return IntrinsicDetectionResult("checkReturnedValueOrFieldIsNotNull", simpleExpressionRenamer)
                            }
                        }
                    }
                }
            } else if (instruction.opcode == DalvikInstructionOpcodes.OP_INVOKE_STATIC) {
                val param = instruction.parameters[0]
                if (param.type == IDalvikInstruction.TYPE_IDX) {
                    val calledMethod = unit.getMethod(param.value.toInt())
                    if (calledMethod.parameterTypes.size == 1) {
                        val detectionResult = detectOneArg(calledMethod, unit)
                        if (detectionResult != null) {
                            if (detectionResult.name == "throwParameterIsNullIAE") {
                                return IntrinsicDetectionResult("checkParameterIsNotNull", ExtendedRenamer(1, argumentRenamer, 0))
                            } else if (detectionResult.name == "throwParameterIsNullNPE") {
                                return IntrinsicDetectionResult("checkNotNullParameter", ExtendedRenamer(1, argumentRenamer, 0))
                            }
                        }
                    }
                }
            }
        }


        return null
    }

    private fun detectThreeArgs(method: IDexMethod, unit: IDexUnit): IntrinsicDetectionResult? {
        if (method.parameterTypes[0].signature == "I" && method.parameterTypes[1].signature == "Ljava/lang/String;" && method.parameterTypes[2].signature == "Ljava/lang/String;") {
            return IntrinsicDetectionResult("reifiedOperationMarker")
        }
        @Suppress("UNCHECKED_CAST")
        val instructions = method.instructions as List<IDalvikInstruction>
        val constStringInst = instructions.firstOrNull { it.opcode == DalvikInstructionOpcodes.OP_CONST_STRING }
        if (constStringInst != null) {
            val param = constStringInst.parameters[1]
            if (param.type == IDalvikInstruction.TYPE_IDX) {
                val str = unit.getString(param.value.toInt()).toString()
                if (str == "Method specified as non-null returned null: ") {
                    // idk how it is used, so no renamer for now.
                    return IntrinsicDetectionResult("checkReturnedValueIsNotNull")
                } else if (str == "Field specified as non-null is null: ") {
                    // no class renaming because idk how it is used, so can't check it works.
                    return IntrinsicDetectionResult("checkFieldIsNotNull", ExtendedRenamer(2, argumentRenamer, 0))
                }
            }
        }
        return null
    }

    private val identifierRegex = Regex("[A-Za-z0-9_$]+")

    /** If the expression is simple enough, extract the name from it **/
    private val simpleExpressionRenamer = ExtendedRenamer(1, { data ->
        if (data.matches(identifierRegex))
            RenameResult(argumentName = data)
        else RenameResult()
    }, 0)


}