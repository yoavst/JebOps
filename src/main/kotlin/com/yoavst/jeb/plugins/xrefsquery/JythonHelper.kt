package com.yoavst.jeb.plugins.xrefsquery

import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaConstant
import com.pnfsoftware.jeb.core.units.code.java.IJavaElement
import com.pnfsoftware.jeb.core.units.code.java.IJavaMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaStaticField
import com.pnfsoftware.jeb.core.units.code.java.JavaFlags
import org.python.util.PythonInterpreter

object JythonHelper {
    private const val JAVA_METHOD = "jmethod"
    private const val JAVA_TYPE = "jtype"
    private const val DEX_METHOD = "dmethod"
    private const val DEX_CLASS = "dclass"
    private const val DEX_UNIT = "dunit"
    private const val XREF_METHOD = "xref_method"
    private const val ARGS = "args"
    private const val CALL_ELEMENT = "call_element"

    private const val UTILS = "utils"
    private const val RESULT = "result"

    fun execute(script: String, executeParams: ExecuteParams): Boolean {
        val baseInterpreter = PythonInterpreter()

        baseInterpreter[JAVA_METHOD] = executeParams.javaMethod
        baseInterpreter[JAVA_TYPE] = executeParams.javaMethod.classType
        baseInterpreter[DEX_METHOD] = executeParams.dexMethod
        baseInterpreter[DEX_CLASS] = executeParams.dexMethod.classType.implementingClass
        baseInterpreter[DEX_UNIT] = executeParams.dexMethod.dex
        baseInterpreter[XREF_METHOD] = executeParams.xrefMethod
        baseInterpreter[ARGS] = executeParams.args
        baseInterpreter[CALL_ELEMENT] = executeParams.callElement
        baseInterpreter[UTILS] = Utils

        baseInterpreter.exec(script)

        return baseInterpreter[RESULT]?.__nonzero__() ?: false
    }

}

object Utils {
    fun isConst(element: IJavaElement): Boolean = element is IJavaConstant

    @JvmOverloads
    fun isConstInt(element: IJavaElement, value: Int? = null): Boolean {
        if (element !is IJavaConstant || !element.type.isInt) {
            return false
        }

        if (value != null && value != element.int)
            return false

        return true
    }

    @JvmOverloads
    fun isConstLong(element: IJavaElement, value: Long? = null): Boolean {
        if (element !is IJavaConstant || !element.type.isLong) {
            return false
        }

        if (value != null && value != element.long)
            return false

        return true
    }

    @JvmOverloads
    fun isConstString(element: IJavaElement, value: String? = null): Boolean {
        if (element !is IJavaConstant || !element.isString) {
            return false
        }

        if (value != null && value != element.string)
            return false

        return true
    }

    @JvmOverloads
    fun isConstBoolean(element: IJavaElement, value: Boolean? = null): Boolean {
        if (element !is IJavaConstant || !element.type.isBoolean) {
            return false
        }

        if (value != null && value != element.boolean)
            return false

        return true
    }

    @JvmOverloads
    fun isConstClass(element: IJavaElement, classSignature: String? = null): Boolean {
        if (element !is IJavaStaticField || element.fieldName != "class") {
            return false
        }

        if (classSignature != null && classSignature != element.classType.signature) {
            return false
        }

        return true
    }


    fun isStaticFinalField(element: IJavaElement): Boolean {
        return element is IJavaStaticField && (element.field.accessFlags and JavaFlags.FINAL != 0)
    }
}

data class ExecuteParams(
    val xrefMethod: IDexMethod,
    val javaMethod: IJavaMethod,
    val dexMethod: IDexMethod,
    val args: List<IJavaElement>,
    val callElement: IJavaElement
)