package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.units.code.java.ICompound
import com.pnfsoftware.jeb.core.units.code.java.IJavaBlock
import com.pnfsoftware.jeb.core.units.code.java.IJavaMethod
import com.pnfsoftware.jeb.core.units.code.java.IStatement
import com.yoavst.jeb.utils.renaming.RenameEngine

abstract class BasicAstTraversal(protected val renameEngine: RenameEngine) {
    /** Traverse block is to traverse its children **/
    fun traverse(block: IJavaBlock) {
        for (i in 0 until block.size()) {
            traverse(block[i])
        }
    }

    /** Traverse a statement by delegating to `traverseNonCompound` **/
    private fun traverse(statement: IStatement) {
        // Handle recursive case
        if (statement is ICompound) {
            statement.blocks.forEach(this::traverse)
        } else {
            traverseNonCompound(statement)
        }
    }

    protected abstract fun traverseNonCompound(statement: IStatement)


}

fun BasicAstTraversal.traverse(method: IJavaMethod) = traverse(method.body)