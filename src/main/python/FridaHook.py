# ?description=update the UIBridge class of JebPlugin
# ?shortcut=Mod1+Mod3+F
# ?author=LeadroyaL With modification by Yoav Sternberg

from com.pnfsoftware.jeb.client.api import IScript, IGraphicalClientContext
from com.pnfsoftware.jeb.core import IEnginesPlugin
from com.pnfsoftware.jeb.core.units.code.android.dex import IDexMethod, IDexType

import utils


class FridaHook(IScript):
    def run(self, ctx):
        if not isinstance(ctx, IGraphicalClientContext):
            print ('This script must be run within a graphical client')
            return

        plugins = ctx.getEnginesContext().getEnginesPlugins()
        for plugin in plugins:
            assert isinstance(plugin, IEnginesPlugin)
            if 'yoav' in plugin.getClass().getCanonicalName():
                classloader = plugin.getClass().getClassLoader()

                # Update focused method
                UIBridge = utils.get_object("com.yoavst.jeb.bridge.UIBridge", classloader)
                UIBridge.update(ctx)

                # Get focused method
                dex_method = UIBridge.getFocusedMethod()
                if not dex_method:
                    print "No selected method"
                    return
                break
        else:
            print "JebOps is not installed"
            return

        assert isinstance(dex_method, IDexMethod)
        clz = dex_method.getClassType().getSignature()[1:-1].replace('/', '.')
        method = dex_method.getName(True)
        method = BasicMethodMap.get(method, method)
        sig = dex_method.getSignature(False)
        this_part = "" if (dex_method.getGenericFlags() & 8) == 8 else "this"
        if dex_method.getParameterTypes() and this_part:
            this_part += ", "

        params_list = ', '.join('"{}"'.format(self.signature_to_frida_signature(t.getSignature(False))) for t in
                                dex_method.getParameterTypes())
        args = []
        for t in dex_method.getParameterTypes():
            args.append(self.type_to_pretty_name(t, args))
        args_list = ', '.join(args)

        fmt = FMT_VOID if dex_method.getReturnType().getSignature() == "V" else FMT_RET
        print fmt.format(
            class_name=clz,
            method_name=method,
            method_sig=sig,
            this_part=this_part,
            param_list=params_list,
            args_list=args_list)

    def signature_to_frida_signature(self, sig):
        if not sig:
            print "Error: received empty type"
            return ""

        if sig[0] == '[':
            # Dealing with array. In this case, we only need to replace "/" with "."
            # input: [I, return: "[I"
            # input: [Ljava/lang/String; return: "[Ljava.lang.String;"
            return sig.replace('/', '.')
        elif sig[0] == "L":
            # Non primitive type, should just fix "/" and "."
            # input: Ljava/lang/String; return: "java.lang.String"
            return sig[1:-1].replace('/', '.')
        else:
            # Primitive type, map it to frida name
            # input: I, return: "int"
            return BasicTypeMap[sig]

    def type_to_pretty_name(self, t, history=None):
        assert isinstance(t, IDexType)
        name = t.getName(True).split("__")[-1].split("_", 1)[-1]

        if name == "Object":
            wanted_name = "arg"
        elif len(name) >= 4:
            wanted_name = decaptialize(name)
        elif t.getImplementingClass() is None:
            wanted_name = BasicTypeMap.get(t.getName(), "arg")
        else:
            # Try parent class
            clz = t.getImplementingClass()
            if clz.getSupertypes():
                wanted_name = self.type_to_pretty_name(clz.getSupertypes()[0], history)
                if wanted_name.startswith("arg"):
                    # Try interfaces
                    for interface in clz.getImplementedInterfaces():
                        wanted_name = self.type_to_pretty_name(interface, history)
                        if not wanted_name.startswith("arg"):
                            return wanted_name
            else:
                wanted_name = "arg"

        if history is None:
            history = []
        if wanted_name not in history:
            return wanted_name
        i = 1
        while True:
            new_name = wanted_name + str(i)
            if new_name not in history:
                return new_name
            i += 1


def decaptialize(name):
    if not name:
        return ""
    return name[0].lower() + name[1:]


FMT_RET = """Java.use("{class_name}")
    .{method_name}
    .overload({param_list})
    .implementation = function ({this_part}{args_list}) {{
        console.log("before hooked {method_sig}")
        let ret = this.{method_name}({args_list})
        console.log("after hooked {method_sig}")
        return ret;
    }};"""
FMT_VOID = """Java.use("{class_name}")
    .{method_name}
    .overload({param_list})
    .implementation = function ({this_part}{args_list}) {{
        console.log("before hooked {method_sig}")
        this.{method_name}({args_list})
        console.log("after hooked {method_sig}")
    }};"""

BasicTypeMap = {
    'C': u'char',
    'B': u'byte',
    'D': u'double',
    'F': u'float',
    'I': u'int',
    'J': u'long',
    'L': u'ClassName',
    'S': u'short',
    'Z': u'boolean',
    'V': u'void',
}

BasicMethodMap = {
    '<init>': u'$init',
}
