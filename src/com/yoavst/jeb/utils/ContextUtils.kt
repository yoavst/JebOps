package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.IEnginesContext
import com.pnfsoftware.jeb.core.RuntimeProjectUtil
import com.pnfsoftware.jeb.core.units.IUnit
import com.pnfsoftware.jeb.core.units.UnitUtil
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit

fun IEnginesContext.getDexUnits(): MutableList<IDexUnit> =
    RuntimeProjectUtil.findUnitsByType(projects[0], IDexUnit::class.java, false)

fun IUnit.refresh() = UnitUtil.notifyGenericChange(this)