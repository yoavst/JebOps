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