package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.java.IJavaElement

fun IJavaElement.visitSubElementsRecursive(visitor: (IJavaElement, IJavaElement) -> Unit) {
    subElements.forEach { subElement ->
        visitor(subElement, this)
        subElement.visitSubElementsRecursive(visitor)
    }
}