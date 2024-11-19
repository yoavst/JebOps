package com.yoavst.jeb.bridge

import com.pnfsoftware.jeb.client.api.IGraphicalClientContext
import com.pnfsoftware.jeb.core.output.IItem
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexType
import com.yoavst.jeb.utils.currentFocusedMethod
import com.yoavst.jeb.utils.currentFocusedType

@Suppress("unused")
/** Used for scripts to update ui elements, so plugins could access it **/
object UIBridge {
    var focusedMethod: IDexMethod? = null
        private set
    var focusedClass: IDexType? = null
        private set
    var focusedAddr: String? = null
        private set
    var focusedItem: IItem? = null
        private set

    var currentMethod: IDexMethod? = null
        private set

    var currentClass: IDexType? = null
        private set

    @JvmStatic
    fun update(context: IGraphicalClientContext) {
        focusedMethod = context.currentFocusedMethod()
        focusedClass = context.currentFocusedType()
        focusedAddr = context.focusedFragment?.activeAddress
        focusedItem = context.focusedFragment?.activeItem
        currentMethod = context.currentFocusedMethod(supportFocus = false, verbose = false)
        currentClass = context.currentFocusedType(supportFocus = false, verbose = false)
    }

    override fun toString(): String = """
        FocusedMethod: $focusedMethod
        CurrentMethod: $currentMethod
        FocusedClass: $focusedClass
        CurrentClass: $currentClass
        FocusedItem: $focusedItem
        FocusedAddr: $focusedAddr
    """.trimIndent()
}