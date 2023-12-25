package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.IEnginesContext
import com.pnfsoftware.jeb.core.RuntimeProjectUtil
import com.pnfsoftware.jeb.core.units.IUnit
import com.pnfsoftware.jeb.core.units.IXmlUnit
import com.pnfsoftware.jeb.core.units.UnitUtil
import com.pnfsoftware.jeb.core.units.code.android.IApkUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.IXApkUnit
import org.w3c.dom.Document

fun IEnginesContext.getDexUnits(): MutableList<IDexUnit> =
        RuntimeProjectUtil.findUnitsByType(projects[0], IDexUnit::class.java, false)

fun IEnginesContext.getXmlResource(name: String): Document? =
        RuntimeProjectUtil.findUnitsByType(projects[0], IXmlUnit::class.java, false).firstOrNull { it.name == name }?.let {
            if (!it.isProcessed)
                it.process()
            it.document
        }


val IEnginesContext.apkPackage: String
    get() = RuntimeProjectUtil.findUnitsByType(projects[0], IApkUnit::class.java, false).firstOrNull()?.packageName
            ?: RuntimeProjectUtil.findUnitsByType(projects[0], IXApkUnit::class.java, false).first().packageName

fun IUnit.refresh() = UnitUtil.notifyGenericChange(this)