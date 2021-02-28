package com.yoavst.jeb.utils

import com.pnfsoftware.jeb.client.api.IGraphicalClientContext
import com.pnfsoftware.jeb.core.IEnginesContext
import com.pnfsoftware.jeb.core.RuntimeProjectUtil
import com.pnfsoftware.jeb.core.units.IUnit
import com.pnfsoftware.jeb.core.units.UnitUtil
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.util.logging.GlobalLog

private const val GRAPHICAL_CONTEXT = "graphical_context_cache"

fun IEnginesContext.getDexUnits(): MutableList<IDexUnit> =
    RuntimeProjectUtil.findUnitsByType(projects[0], IDexUnit::class.java, false)

fun IUnit.refresh() = UnitUtil.notifyGenericChange(this)

fun unsafeGetGraphicalContext(context: IEnginesContext? = null): IGraphicalClientContext {
    // first try to get from cache
    val cached = context?.projects?.mapNotNull { it.getData(GRAPHICAL_CONTEXT) as? IGraphicalClientContext }?.firstOrNull()
    if (cached != null)
        return cached
    try {
        val publicContextClass = Class.forName("com.pnfsoftware.jeb.rcpclient.PublicContext")
        val rcpClientContextClass = Class.forName("com.pnfsoftware.jeb.rcpclient.RcpClientContext")
        val instanceField = rcpClientContextClass.getDeclaredField("singleIntance")
        instanceField.isAccessible = true
        val rcpInstance = instanceField.get(null)
        val result = publicContextClass.constructors[0].newInstance(rcpInstance) as IGraphicalClientContext
        // save to cache
        context?.projects?.firstOrNull()?.setData(GRAPHICAL_CONTEXT, cached, false)
        return result
    } catch (throwable: Throwable) {
        GlobalLog.getLogger()
            .error("Unsupported JEB version. Please contact me with the given version number, and jeb.jar, jebc.jar\n" +
                    "In the meantime, you could use the script ContextCacheScript.py to load the context from script, and then try again.")
        throw IllegalStateException("Jeb version is incorrect").initCause(throwable)
    }
}