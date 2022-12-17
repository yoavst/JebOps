package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.java.IJavaCompound
import com.pnfsoftware.jeb.core.units.code.java.IJavaBlock
import com.pnfsoftware.jeb.core.units.code.java.IJavaMethod
import com.pnfsoftware.jeb.core.units.code.java.IJavaStatement
import com.yoavst.jeb.utils.renaming.RenameEngine

abstract class BasicAstTraversal(protected val renameEngine: RenameEngine) {
    /** Traverse block is to traverse its children **/
    fun traverse(block: IJavaBlock) {
        for (i in 0 until block.size()) {
            traverse(block[i])
        }
    }

    /** Traverse a statement by delegating to `traverseNonCompound` **/
    private fun traverse(statement: IJavaStatement) {
        // Handle recursive case
        if (statement is IJavaCompound) {
            statement.blocks.forEach(this::traverse)
        } else {
            traverseNonCompound(statement)
        }
    }

    protected abstract fun traverseNonCompound(statement: IJavaStatement)


}

fun BasicAstTraversal.traverse(method: IJavaMethod) = traverse(method.body)