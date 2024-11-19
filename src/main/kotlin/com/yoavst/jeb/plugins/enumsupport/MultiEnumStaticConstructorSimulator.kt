package com.yoavst.jeb.plugins.enumsupport

import com.pnfsoftware.jeb.core.units.code.android.dex.IDalvikInstruction
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexField
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.yoavst.jeb.utils.renaming.RenameEngine
import com.yoavst.jeb.utils.renaming.RenameReason
import com.yoavst.jeb.utils.renaming.RenameRequest

class MultiEnumStaticConstructorSimulator(
        clazz: IDexClass,
        constructors: List<IDexMethod>,
        renameEngine: RenameEngine
) : BaseDalvikSimulator(clazz, renameEngine) {
    private val constructorIndices = constructors.mapTo(mutableSetOf()) { it.prototypeIndex }

    override fun onInvokeDirect(instruction: IDalvikInstruction) {
        val invokedMethod = getMethod(instruction[0])
        if (invokedMethod.prototypeIndex in constructorIndices) {
            onInvokeGeneralized(register = instruction[1], strRegister = instruction[2], instruction)
        } else if (invokedMethod.name == "<init>" && invokedMethod.classType.isSubClass()) {
            val parameterTypes = invokedMethod.parameterTypes
            if (parameterTypes.size != 0 && parameterTypes[0].signature == "Ljava/lang/String;") {
                // Assume it receives the enum name as first parameter
                onInvokeGeneralized(register = instruction[1], strRegister = instruction[2], instruction)
            } else {
                // the enum constructor was moved to a subclass
                val singleEnumConstructorSimulator = SingleEnumConstructorSimulator(
                        invokedMethod.classType.implementingClass!!,
                        constructorIndices,
                        renameEngine
                )
                singleEnumConstructorSimulator.run(invokedMethod)
                if (singleEnumConstructorSimulator.name != null) {
                    onInvokeGeneralized(instruction[1], singleEnumConstructorSimulator.name!!, instruction)
                }
            }
        }
    }

    override fun onInvokeDirectRange(instruction: IDalvikInstruction) {
        val invokedMethod = getMethod(instruction[0])
        if (invokedMethod.prototypeIndex in constructorIndices || (invokedMethod.name == "<init>" && invokedMethod.classType.isSubClass() && invokedMethod.parameterTypes.size >= 2)) {
            val register = instruction[1] and 0xffffffff
            val strRegister = register + 1
            onInvokeGeneralized(register, strRegister, instruction)
        }
    }

    private fun onInvokeGeneralized(register: Long, strRegister: Long, instruction: IDalvikInstruction) {
        val matchingStr = (mapping[strRegister] as? RegisterValue.StringValue)?.value
        if (matchingStr == null) {
            warn { "Found init method, but the string wasn't in the mapping" }
            return
        }

        onInvokeGeneralized(register, matchingStr, instruction)
    }

    private fun onInvokeGeneralized(register: Long, matchingStr: String, instruction: IDalvikInstruction) {
        val instance = (mapping[register] as? RegisterValue.EnumInstanceValue) ?: run {
            warn { "Found init method on non enum instance" }
            return
        }

        if (instance.value != null) {
            warn { "Trying to init already initialized value" }
            return
        }
        instance.value = matchingStr

        trace { "Mapping $register to Enum($matchingStr), $instruction" }
    }


    override fun onStaticPutObject(instruction: IDalvikInstruction) {
        val (lhs, rhs) = instruction.parameters
        if (lhs.type == IDalvikInstruction.TYPE_REG && rhs.type == IDalvikInstruction.TYPE_IDX) {
            // Assign from reg to field
            val register = lhs.value
            val field = unit.getField(rhs.value.toInt())

            if ((field.genericFlags and IDexField.FLAG_STATIC) != 0 && field.classTypeIndex == classIndex && field.fieldTypeIndex == classIndex) {
                // Static field of the class, we want to rename
                when (val value = mapping[register]) {
                    is RegisterValue.EnumInstanceValue -> {
                        if (value.value == null) {
                            warn { "Assigning uninit instance to static field" }
                            return
                        }
                        trace { "Renaming $field to ${value.value}" }
                        renameEngine.renameField(RenameRequest(value.value!!, RenameReason.EnumName), field, clazz)
                    }

                    is RegisterValue.StringValue, null -> {
                        warn { "Found a static assignment but without matching enum assignment" }
                    }
                }
            }
        }
    }

    override fun onNewInstance(instruction: IDalvikInstruction) {
        val (reg, typeIndex) = instruction
        if (typeIndex.toInt() == classIndex) {
            mapping[reg] = RegisterValue.EnumInstanceValue(null)

            trace { "Allocating new instance on $reg" }
        } else {
            // Maybe it is a subclass
            val clazz = getType(typeIndex)
            if (clazz.isSubClass()) {
                mapping[reg] = RegisterValue.EnumInstanceValue(null)

                trace { "Allocating new subclass instance on $reg" }
            }
        }
    }
}