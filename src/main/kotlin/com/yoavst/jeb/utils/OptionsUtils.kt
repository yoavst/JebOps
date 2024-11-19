package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.BooleanOptionDefinition
import com.pnfsoftware.jeb.core.IOptionDefinition
import com.pnfsoftware.jeb.core.OptionDefinition

const val ClassFilterDefault = ".*"

const val ClassFilterOptionTag = "Class filter"
val ClassFilterOption: IOptionDefinition = OptionDefinition(
        ClassFilterOptionTag,
        ClassFilterDefault,
        """The operation you are about to perform may be costly, and cannot be interrupted.
If you only want to run it only on specific classes, you can specify a regex.
For example, if you only want to run it on default package, use "^L[^\/]*;$"
Or if you want to run it on package com.test use "^Lcom\/test\/.*;$"
Default is to run it on all packages: "$ClassFilterDefault""""
)

const val ScopeThisClassTag = "Scope this class"
fun scopeThisClass(default: Boolean, classSignature: String? = null): IOptionDefinition {
    return if (classSignature == null) {
        BooleanOptionDefinition(
                ScopeThisClassTag, default, "Run the given operation only on the selected class. Default: $default"
        )
    } else {
        BooleanOptionDefinition(
                ScopeThisClassTag,
                default,
                "Run the given operation only on the selected class. Default: $default\nCurrent class: $classSignature"
        )
    }
}

const val ScopeThisMethodTag = "Scope this method"
fun scopeThisMethod(default: Boolean = true, methodSignature: String? = null): IOptionDefinition {
    return if (methodSignature == null) {
        BooleanOptionDefinition(
                ScopeThisMethodTag, default, "Run the given operation only on the selected method. Default: $default"
        )
    } else {
        BooleanOptionDefinition(
                ScopeThisMethodTag,
                default,
                "Run the given operation only on the selected method. Default: $default\nCurrent method: $methodSignature"
        )
    }
}

fun usingThisMethod(methodSignature: String? = null) = OptionDefinition("Selected method: $methodSignature")
fun usingThisClass(methodSignature: String? = null) = OptionDefinition("Selected class: $methodSignature")