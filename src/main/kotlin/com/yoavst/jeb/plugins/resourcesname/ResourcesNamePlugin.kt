package com.yoavst.jeb.plugins.resourcesname

import com.pnfsoftware.jeb.core.IPluginInformation
import com.pnfsoftware.jeb.core.PluginInformation
import com.pnfsoftware.jeb.core.Version
import com.pnfsoftware.jeb.core.units.code.android.DexUtil.bytearrayULEInt32ToInt
import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.core.units.code.android.dex.*
import com.pnfsoftware.jeb.core.units.code.java.IJavaConstant
import com.pnfsoftware.jeb.util.collect.MultiMap
import com.yoavst.jeb.bridge.UIBridge
import com.yoavst.jeb.utils.*
import com.yoavst.jeb.utils.renaming.RenameEngine
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory


/**
 * Replace const resource id with R.type.resourceName.
 * Doesn't work with:
 *  - switch case since it is not supported by jeb.
 *      xrefs are enabled though
 *  - static initializers for field (works for array)
 *      should we add xrefs support for this case?
 */
class ResourcesNamePlugin : BasicEnginesPlugin(
    supportsClassFilter = true, defaultForScopeOnThisClass = false,
    defaultForScopeOnThisFunction = false
) {
    private lateinit var resources: MultiMap<String, ResourceId>
    private lateinit var intToResourceId: Map<Int, ResourceId>

    override fun getPluginInformation(): IPluginInformation = PluginInformation(
        "Resources name restore",
        "Fire the plugin to recreate R class and replace constants in code",
        "Yoav Sternberg",
        Version.create(0, 1, 0),
        Version.create(3, 0, 16),
        null
    )

    override fun preProcess(): Boolean {
        val (resources, intToResourceId) = context.getXmlResource("public.xml")?.parsePublicXml() ?: run {
            logger.error("No such resource public.xml! The resource is needed to reconstruct the names from ids.")
            return false
        }
        val (androidResources, androidIntToResourceId) = parseAndroidPublicXml()
        this.resources = resources
        androidResources.keySet().forEach {
            resources.putMulti(it, androidResources[it])
        }
        this.intToResourceId = intToResourceId + androidIntToResourceId
        return true
    }

    override fun processUnit(unit: IDexUnit, renameEngine: RenameEngine) {
        val decompiler = unit.decompiler
        createRForUnit(unit)

        if (isOperatingOnlyOnThisMethod) {
            if (UIBridge.currentMethod != null && UIBridge.currentClass != null) {
                // you cannot see the sources of a type without implementing class
                processMethod(UIBridge.currentMethod!!, unit, decompiler, renameEngine)
            }
        } else if (isOperatingOnlyOnThisClass) {
            if (UIBridge.currentClass != null) {
                UIBridge.currentClass!!.implementingClass.methods.forEach { method ->
                    preProcessMethod(method, unit, decompiler, renameEngine)
                }
            }
        } else {
            unit.classes.asSequence().filter(classFilter::matches).flatMap(IDexClass::getMethods)
                .forEach { preProcessMethod(it, unit, decompiler, renameEngine) }
        }
    }

    private fun preProcessMethod(method: IDexMethod, unit: IDexUnit, decompiler: IDexDecompilerUnit, engine: RenameEngine) {
        if (method.isInternal && method.instructions != null) {
            @Suppress("UNCHECKED_CAST")
            val instructions = method.instructions as List<IDalvikInstruction>
            for (instruction in instructions) {
                if (instruction.opcode == DalvikInstructionOpcodes.OP_CONST) {
                    val value = instruction.parameters[1].value.toInt()
                    if (value in intToResourceId) {
                        // method uses resource id, it's ok to decompile it.
                        processMethod(method, unit, decompiler, engine)
                        break
                    }
                }
            }
        }
    }

    private fun processMethod(method: IDexMethod, unit: IDexUnit, decompiler: IDexDecompilerUnit, engine: RenameEngine) {
        logger.trace("Processing: ${method.currentSignature}")
        val decompiledMethod = decompiler.decompileDexMethod(method) ?: run {
            logger.warning("Failed to decompile method: ${method.currentSignature}")
            return
        }

        val packageSignature = "L${context.apkPackage}.".replace(".", "/")

        // process decompile method
        decompiledMethod.body.visitSubElementsRecursive { element, parent ->
            if (element is IJavaConstant && element.type?.isInt == true) {
                val resource = intToResourceId[element.int] ?: return@visitSubElementsRecursive

                val rType = decompiler.astFactories.typeFactory.createType(resource.toClass(packageSignature))
                val resField = decompiler.astFactories.createFieldReference(resource.toField(packageSignature))
                val resStaticField = decompiler.astFactories.createStaticField(rType, resField)
                parent.replaceSubElement(element, resStaticField)
                method.classType.implementingClass?.let(engine.stats.effectedClasses::add)
            }
        }

        // add xrefs
        @Suppress("UNCHECKED_CAST")
        val instructions = method.instructions as List<IDalvikInstruction>
        for (instruction in instructions) {
            when {
                instruction.opcode == DalvikInstructionOpcodes.OP_CONST -> {
                    // const REGISTER, VALUE
                    val value = instruction.parameters[1].value.toInt()
                    val resource = intToResourceId[value] ?: continue
                    resource.addXref(method, unit, packageSignature, instruction)
                }
                instruction.opcode == DalvikInstructionOpcodes.OP_FILL_ARRAY_DATA -> {
                    // fill-array-data REGISTER, ARRAY_DATA
                    for (element in instruction.arrayData.elements) {
                        if (element.size == 4) {
                            // 32 bit number
                            val value = bytearrayULEInt32ToInt(element, 0)
                            val resource = intToResourceId[value] ?: continue
                            resource.addXref(method, unit, packageSignature, instruction)
                        }
                    }
                }
                instruction.isSwitch -> {
                    // switch data consists of pairs (value, address)
                    for ((value, _) in instruction.switchData.elements) {
                        val resource = intToResourceId[value] ?: continue
                        resource.addXref(method, unit, packageSignature, instruction)
                    }
                }
            }
        }
    }

    private fun ResourceId.addXref(method: IDexMethod, unit: IDexUnit, packageSignature: String, instruction: IDalvikInstruction) {
        unit.referenceManager.addFieldReference(
            toField(packageSignature),
            "${method.signature}+${instruction.offset}",
            DexReferenceType.GET
        )
    }

    private fun createRForUnit(unit: IDexUnit) {
        val packageName = context.apkPackage
        val packageSignature = "L$packageName.".replace(".", "/")
        resources.keySet().forEach { resourceType ->
            val typeSignature = ResourceId("", 0, resourceType, isSystem = false).toClass(packageSignature)
            val androidTypeSignature = ResourceId("", 0, resourceType, isSystem = true).toClass(packageSignature)
            unit.addType(typeSignature)
            unit.addType(androidTypeSignature)
            resources[resourceType].forEach { res ->
                unit.addField(if (res.isSystem) androidTypeSignature else typeSignature, res.name, "I")
            }
        }
    }

    private fun Document.parsePublicXml(isSystem: Boolean = false): Pair<MultiMap<String, ResourceId>, Map<Int, ResourceId>> {
        val publicNodeList = getElementsByTagName("public")
        val resources = MultiMap<String, ResourceId>()
        val idsToResources = mutableMapOf<Int, ResourceId>()
        for (i in 0 until publicNodeList.length) {
            val attrs = publicNodeList.item(i).attributes
            val type = attrs.getNamedItem("type").textContent
            val id = attrs.getNamedItem("id").textContent.substring(2).toInt(16)
            val res = ResourceId(attrs.getNamedItem("name").textContent, id, type, isSystem)
            resources.put(type, res)
            idsToResources[id] = res
        }
        return resources to idsToResources
    }

    private fun parseAndroidPublicXml(): Pair<MultiMap<String, ResourceId>, Map<Int, ResourceId>> =
        javaClass.classLoader.getResourceAsStream("aosp_public.xml")!!.toXmlDocument().parsePublicXml(isSystem = true)

    private fun InputStream.toXmlDocument(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(this)
    }
}