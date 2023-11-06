package com.yoavst.jeb.plugins.enumsupport

import com.pnfsoftware.jeb.core.units.code.android.dex.IDalvikInstruction
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.yoavst.jeb.utils.renaming.RenameEngine

class SingleEnumConstructorSimulator(clazz: IDexClass, private val superInits: Set<Int>, renameEngine: RenameEngine) :
        BaseDalvikSimulator(clazz, renameEngine) {
    var name: String? = null
    private var thisInstance = RegisterValue.EnumInstanceValue(null)

    override fun run(method: IDexMethod) {
        // Add p0 to mapping
        val thisIndex = (method.data.codeItem.registerCount - method.parameterTypes.size - 1).toLong()
        mapping[thisIndex] = thisInstance

        super.run(method)
    }

    override fun onInvokeDirect(instruction: IDalvikInstruction) {
        val invokedMethod = getMethod(instruction[0])
        if (invokedMethod.prototypeIndex in superInits) {
            onInvokeGeneralized(register = instruction[1], strRegister = instruction[2], instruction)
        }
    }

    override fun onInvokeDirectRange(instruction: IDalvikInstruction) {
        val invokedMethod = getMethod(instruction[0])
        if (invokedMethod.prototypeIndex in superInits) {
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

        if (thisInstance.value != null) {
            if (name != null) {
                warn { "Initialize this class twice, was: '$name' now '${thisInstance.value}'" }
            }
            name = thisInstance.value
        }
    }

    override fun onStaticPutObject(instruction: IDalvikInstruction) = Unit
    override fun onNewInstance(instruction: IDalvikInstruction) = Unit

}