package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.core.IOptionDefinition
import com.pnfsoftware.jeb.core.OptionDefinition

const val ClassFilterDefault = ".*"

val ClassFilterOption: IOptionDefinition = OptionDefinition(
    "Class filter", """The operation you are about to perform may be costly, and cannot be interrupted.
If you only want to run it only on specific classes, you can specify a regex.
For example, if you only want to run it on default package, use "^L[^\/]*;$"
Or if you want to run it on package com.test use "^Lcom\/test\/.*;$"
Default is to run it on all packages: "$ClassFilterDefault""""
)
