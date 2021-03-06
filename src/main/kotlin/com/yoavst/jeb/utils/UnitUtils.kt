package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.actions.ActionContext
import com.pnfsoftware.jeb.core.actions.ActionXrefsData
import com.pnfsoftware.jeb.core.actions.Actions
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexItem

fun IDexUnit.xrefsFor(item: IDexItem): List<String> {
    val data = ActionXrefsData()
    if (prepareExecution(ActionContext(this, Actions.QUERY_XREFS, item.itemId, item.address), data))
        return data.addresses

    return emptyList()
}