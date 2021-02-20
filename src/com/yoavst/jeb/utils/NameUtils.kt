package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.ICodeItem
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass
import com.pnfsoftware.jeb.core.units.code.java.IJavaField
import com.pnfsoftware.jeb.core.units.code.java.IJavaIdentifier

val ICodeItem.originalName: String get() = getName(false)
val ICodeItem.currentName: String get() = getName(true)
val ICodeItem.originalSignature: String get() = getSignature(false)
val ICodeItem.currentSignature: String get() = getSignature(true)


val IJavaField.originalName: String get() = name
fun IJavaField.currentName(cls: IDexClass): String? = cls.getField(false, originalName, type.signature)?.currentName
fun IJavaField.currentName(unit: IDexUnit): String? = unit.getField(signature)?.currentName

val IJavaIdentifier.originalName: String get() = name
fun IJavaIdentifier.currentName(unit: IDexUnit) = unit.decompiler.getIdentifierName(this) ?: originalName