package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.actions.ActionContext
import com.pnfsoftware.jeb.core.actions.ActionXrefsData
import com.pnfsoftware.jeb.core.actions.Actions
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.DexPoolType
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexItem
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexString

fun IDexUnit.xrefsFor(item: IDexItem): List<String> {
    val data = ActionXrefsData()
    if (prepareExecution(ActionContext(this, Actions.QUERY_XREFS, item.itemId, item.address), data))
        return data.addresses

    return emptyList()
}

fun IDexUnit.xrefsForString(string: IDexString): List<String> {
    return referenceManager.getReferences(DexPoolType.STRING, string.index).map { it.internalAddress }
}

private val getComment = try {
    IDexUnit::class.java.getMethod("getComment", String::class.java)
} catch (e: NoSuchMethodException) {
    IDexUnit::class.java.getMethod("getInlineComment", String::class.java)
}

private val setComment = try {
    IDexUnit::class.java.getMethod("setComment", String::class.java, String::class.java)
} catch (e: NoSuchMethodException) {
    IDexUnit::class.java.getMethod("setInlineComment", String::class.java, String::class.java)
}

fun IDexUnit.getCommentBackport(address: String): String? = getComment(this, address) as? String
fun IDexUnit.setCommentBackport(address: String, value: String) {
    setComment(this, address, value)
}