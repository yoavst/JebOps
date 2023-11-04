package com.yoavst.jeb.plugins.enumsupport

import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.*
import com.pnfsoftware.jeb.util.logging.GlobalLog
import com.pnfsoftware.jeb.util.logging.ILogger
import com.yoavst.jeb.utils.renaming.RenameEngine

abstract class BaseDalvikSimulator(protected val clazz: IDexClass, protected val renameEngine: RenameEngine) {
    protected val logger: ILogger = GlobalLog.getLogger(javaClass)

    protected val mapping: MutableMap<Long, RegisterValue> = mutableMapOf()
    protected val unit: IDexUnit = clazz.dex
    protected val classIndex: Int = clazz.classTypeIndex
    protected val className: String = clazz.name

    open fun run(method: IDexMethod) {
        @Suppress("UNCHECKED_CAST")
        (method.instructions as List<IDalvikInstruction>).forEach(::onInstruction)
    }

    protected open fun onInstruction(instruction: IDalvikInstruction) {
        when (instruction.opcode) {
            DalvikInstructionOpcodes.OP_CONST_STRING, DalvikInstructionOpcodes.OP_CONST_STRING_JUMBO ->
                onConstString(instruction)

            DalvikInstructionOpcodes.OP_INVOKE_DIRECT, DalvikInstructionOpcodes.OP_INVOKE_DIRECT_JUMBO ->
                onInvokeDirect(instruction)

            DalvikInstructionOpcodes.OP_INVOKE_DIRECT_RANGE ->
                onInvokeDirectRange(instruction)

            DalvikInstructionOpcodes.OP_SPUT_OBJECT, DalvikInstructionOpcodes.OP_SPUT_OBJECT_JUMBO ->
                onStaticPutObject(instruction)

            DalvikInstructionOpcodes.OP_MOVE_OBJECT, DalvikInstructionOpcodes.OP_MOVE_OBJECT_16, DalvikInstructionOpcodes.OP_MOVE_OBJECT_FROM_16 ->
                onMoveObject(instruction)

            DalvikInstructionOpcodes.OP_NEW_INSTANCE, DalvikInstructionOpcodes.OP_NEW_INSTANCE_JUMBO ->
                onNewInstance(instruction)
        }
    }

    //region Handlers
    protected open fun onConstString(instruction: IDalvikInstruction) {
        val register = instruction[0]
        val str = getString(instruction[1])

        mapping[register] = RegisterValue.StringValue(str)

        trace { "Mapping $register to $str, $instruction" }
    }

    protected open fun onMoveObject(instruction: IDalvikInstruction) {
        val (to, from) = instruction
        if (from in mapping) {
            mapping[to] = mapping[from]!!

            trace { "Moving from $from to $to" }

        } else {
            // Clear to in this case, as we don't want it to cache old value
            mapping.remove(to)

            trace { "Moving from $from to $to: invalidating" }
        }
    }

    protected abstract fun onInvokeDirect(instruction: IDalvikInstruction)
    protected abstract fun onInvokeDirectRange(instruction: IDalvikInstruction)
    protected abstract fun onStaticPutObject(instruction: IDalvikInstruction)
    protected abstract fun onNewInstance(instruction: IDalvikInstruction)
    //endregion

    //region Utils
    protected operator fun IDalvikInstruction.get(index: Int): Long = getParameter(index).value
    protected operator fun IDalvikInstruction.component1(): Long = this[0]
    protected operator fun IDalvikInstruction.component2(): Long = this[1]
    protected operator fun IDalvikInstruction.component3(): Long = this[2]

    protected fun getType(index: Long): IDexType = unit.getType(index.toInt())
    protected fun getString(index: Long): String = unit.getString(index.toInt()).value
    protected fun getMethod(index: Long): IDexMethod = unit.getMethod(index.toInt())


    protected inline fun trace(@Suppress("UNUSED_PARAMETER") state: () -> String) {
//        logger.trace("$className ${state()}")
    }

    protected inline fun warn(state: () -> String) {
        logger.warning("$className ${state()}")
    }

    protected fun IDexType.isSubClass(): Boolean =
        implementingClass?.supertypes?.any { it.index == classIndex } == true
    //endregion
}

sealed interface RegisterValue {
    data class StringValue(val value: String) : RegisterValue
    data class EnumInstanceValue(var value: String? = null) : RegisterValue
}