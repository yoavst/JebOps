# JebOps

Set of JEB plugins and scripts for better Android reversing experience.

## Build

### Offline environment

Extract the provided `offline.tar.xz` to the main project's folder. You are good to go.

### Compilation

1. Copy `jeb.jar` and `swt.jar` from `$JEB_HOME/bin/app` to `libs/runtime/`
2. Use `./gradlew jar` to build a flat jar. It will be stored on `build/libs/JebOps-VERSION.jar`

## Installation

1. Copy the flat jar to `$JEB_HOME/coreplugins/`
2. Copy The following scripts from `src/main/python` to `$JEB_HOME/scripts`:
    * RenameFromConstArg.py
    * UIBridge.py
    * utils.py (not a script, but a dependency of both scripts)
3. Restart JEB

## Usage

### One time plugins

This kind of plugin are supposed to run only once per project. In order to run them, Go to "File" menu, select "Plugins"
, Then "Execute an engine plugin". Now, choose the plugin you want to run.

### Dynamic plugins

Currently, the project has only one dynamic plugin: rename from constant arg. It can be launched in two ways:

1. Use the shortcut "Ctrl+Alt+U" while selecting a method. Then, choose the plugin like you choose a one time plugin.
2. Use the shortcut "Ctrl+Alt+L" while selecting a method.

### Naming convention

The naming convention of the plugin is `original__Reason_NewName`. 
For example, it may convert `axu` to `axu__A_SettingsUtils`.

## Plugins

If the plugin supports running on a specific class/method, you can use "Ctrl+Alt+U" to select the method, before
launching the plugin.

### ToString

Obfuscation will rename private fields, getters and setters. However, if the class has an informative `toString()`
method, those names could be restored. The one time plugin `ToString renaming` could be used to automate the task.

Running the plugin on Twitter apk using i7-4770 took 30s, yielding:

```
Stats:
 Classes: 826 Methods: 1531
 Fields: 2421
```

#### Example

Before:

```java
public final class a01 {
    private final TextView a;
    private final CharSequence b;
    private final int c;
    private final int d;
    private final int e;

    public final int a() {
        return this.e;
    }

    @Override
    public String toString() {
        return "TextViewTextChangeEvent(view=" + this.a + ", text=" + this.b + ", start=" + this.c + ", before=" + this.d + ", count=" + this.e + ")";
    }
}
```

After:

```java
public final class a01__TS_TextViewTextChangeEvent {
    private final TextView a__TS_view;
    private final CharSequence b__TS_text;
    private final int c__TS_start;
    private final int d__TS_before;
    private final int e__TS_count;

    public final int a__TS_getCount() {
        return this.e__TS_count;
    }

    @Override
    public String toString() {
        return "TextViewTextChangeEvent(view=" + this.a__TS_view + ", text=" + this.b__TS_text + ", start=" + this.c__TS_start + ", before=" + this.d__TS_before + ", count=" + this.e__TS_count + ")";
    }
}
```

### Enums

Enum's constructor signature is `<init>(String name, int index)`. All the possible enum values are public static final
fields of the enum class. However, after obfuscation, the field's name might change, and JEB sometimes fail to use the
name from the constructor. The one time plugin `Enum field renaming` could be used to automate the task.

Running the plugin on Twitter apk using i7-4770 took 15s, yielding:

```
Stats:
 Classes: 805 Fields: 3719
```

#### Example

Before:

```java
// PARTIAL FAILURE: ENUM SUGARING
// The enumeration is rendered as-is instead of being sugared into a Java 5 enum.
static final class b extends Enum {
    public static final enum b U;
    public static final enum b V;
    public static final enum b W;
    public static final enum b X;
    private static final b[] Y;

    static {
        b v0 = new b("NONE", 0);
        b.U = v0;
        b v1 = new b("START", 1);
        b.V = v1;
        b v3 = new b("END", 2);
        b.W = v3;
        b v5 = new b("CENTER", 3);
        b.X = v5;
        b.Y = new b[]{v0, v1, v3, v5};
    }
}
```

After:

```java
static final class b__T_Enum extends Enum {
    public static final enum b__T_Enum U__E_NONE;
    public static final enum b__T_Enum V__E_START;
    public static final enum b__T_Enum W__E_END;
    public static final enum b__T_Enum X__E_CENTER;
    private static final b__T_Enum[] Y;
}
```

### Resources

Android app use R.type.resourceName to reference resources from Java code. Obfuscation may:

1. inline the const ints into the code
2. Rename the classes and fields

The one time plugin `Resources name restore` could be used to restore the names. It does that by:

1. Create a fake R class, and replace constant in the decompiled view with reference to fake field.
2. Rename the static fields.

Running the plugin on Twitter apk using i7-4770 took 15s, yielding:

```
Stats:
 Fields: 9454
```

#### Example

Before:

```java
public final class p8 {
    public static final int A = 0x7F0A00B4;  // id:advertiser_avatar
    public static final int A0 = 0x7F0A0154;  // id:birdwatch_indicator
    public static final int A1 = 0x7F0A022A;  // id:caret
    public static final int A2 = 0x7F0A0335;  // id:crop
    public static final int A3 = 0x7F0A03D3;  // id:dock
    public static final int A4 = 0x7F0A0493;  // id:favorite
    public static final int A5 = 0x7F0A0560;  // id:gallery_header_trash
    public static final int A6 = 0x7F0A0623;  // id:interstitial_view_stub
    public static final int A7 = 0x7F0A073D;  // id:menu_ads_companion
    public static final int A8 = 0x7F0A07CD;  // id:namespace_text
    public static final int A9 = 0x7F0A0891;  // id:pin_list_button
    public static final int Aa = 0x7F0A093D;  // id:ptr_overlay_bg
    public static final int Ab = 0x7F0A09F8;  // id:scroll_view
    // ...
}
```

After:

```java
public final class p8 {
    public static final int A__R_advertiser_avatar = 0x7F0A00B4;  // id:advertiser_avatar
    public static final int A0__R_birdwatch_indicator = 0x7F0A0154;  // id:birdwatch_indicator
    public static final int A1__R_caret = 0x7F0A022A;  // id:caret
    public static final int A2__R_crop = 0x7F0A0335;  // id:crop
    public static final int A3__R_dock = 0x7F0A03D3;  // id:dock
    public static final int A4__R_favorite = 0x7F0A0493;  // id:favorite
    public static final int A5__R_gallery_header_trash = 0x7F0A0560;  // id:gallery_header_trash
    public static final int A6__R_interstitial_view_stub = 0x7F0A0623;  // id:interstitial_view_stub
    public static final int A7__R_menu_ads_companion = 0x7F0A073D;  // id:menu_ads_companion
    public static final int A8__R_namespace_text = 0x7F0A07CD;  // id:namespace_text
    public static final int A9__R_pin_list_button = 0x7F0A0891;  // id:pin_list_button
    public static final int Aa__R_ptr_overlay_bg = 0x7F0A093D;  // id:ptr_overlay_bg
    public static final int Ab__R_scroll_view = 0x7F0A09F8;  // id:scroll_view
```

### Const arg renaming

Modern code contains a lot of references to the real variable name. Few examples are:

1. Call to log: `Log.e("ResUtils", "Failed to run")`
2. Assertions: `Preconditions.notNull(activity, "activity")`
3. Dict retrieve: `conditions = jsonObject.get("conditions")`

`Const arg renaming plugin` can be used to automate the renaming task. select the method you want to use as source for
renaming, and press "Ctrl+Alt+L". You will have to supply the following details:

1. What is going to get renamed: class, method, asignee, another argument or custom target
2. What is the index of the const argument that is the name. (0-based)
3. (Optional) if you choose argument, what is the index of the other argument you want to rename. (0-based)

Then, the plugin will go over all the xrefs of the method, and if the name argument is constant, it will be used as
name.

#### Custom

One can have a custom scheme to convert the constant argument to names. You will be asked to supply a python file.

Input:

```python
# tag will be the constant argument from the call
tag = "..."  
```

Output:

```python
# Initialize one of the following variables
cls = "New class name"
method = "New method name"
assignee = "New assignee name"
argument = "New argument name"
```

Note that if you use argument, you should supply the argument index in the dialog.

There are 2 predefined functions you can use:

```python
def split2(s, sep, at_least):
    '''Split the string using separator. Result array will be at least the given length.'''
    arr = s.split(sep)
    return arr + [""] * (at_least - len(arr))


def underscore_to_camelcase(word):
    return ''.join(x.capitalize() or '_' for x in word.split('_'))
```

For example, for `Intent::getExtra` you can use: `assignee = underscore_to_camelcase(tag.split(".")[-1])`

#### Mass renaming

In order to automate the task, you can use the plugin `Const arg mass renaming plugin` to run the renaming on multiple
function at the same time. You need to create a file with the following format:

```
# You can use "#" for comments
TARGET SIGNATURE CONST_ARG_INDEX [custom script path] [renamed argument index]
```

Where:

* target in `{Class, Method, Argument, Assignee, Custom}`
* signature is a dex method signature. for example: "Lcom/yoavst/test/TestClass;->doStuff(Ljava/lang/String;)
  Ljava/lang/String;"
* custom script path is a path relative to the signatures file, or a resource from jar, written
  as `jar:resource_name.py`. Currently, the available built-in renamers are:
    * `renamer_bundle_get.py` - supports the case of `var = get("LONG.DOT.SEPARATED.NAME.NOTIFICATION_TITLE")`
    * `renamer_bundle_set.py` - supports the case of `set("LONG.DOT.SEPARATED.NAME.NOTIFICATION_TITLE", var)`
    * `renamer_log_renamer.py` - supports the case of `log("CLASSNAME MAYBE_METHODNAME", ...)`
    
The project comes with a builtin list, supporting: Intent, Bundle, ContentValues, org.json and shared preferences. It
doesn't support external libraries because they would probably be obfuscated.

```
# ...
Custom Landroid/os/Bundle;->get(Ljava/lang/String;)Ljava/lang/Object; 0 jar:renamer_bundle_get.py
Custom Landroid/os/Bundle;->getBinder(Ljava/lang/String;)Landroid/os/IBinder; 0 jar:renamer_bundle_get.py
# ...
```

#### Example

Running the mass renaming plugin, using built-in list, on Twitter apk using i7-4770 took 100s, yielding:

```
Stats:
 Classes: 295 Methods: 95
 Fields: 281 Identifiers: 1272
```

Before:

```java
private static Float d(Intent arg3) {
    int v0 = arg3.getIntExtra("level", -1);
    int v3 = arg3.getIntExtra("scale", -1);
    return v0 == -1 || v3 == -1 ? null : ((float)(((float)v0) / ((float)v3)));
}
```

After:

```java
private static Float d(Intent arg3) {
    int __A_Level = arg3.getIntExtra("level", -1);
    int __A_Scale = arg3.getIntExtra("scale", -1);
    return __A_Level == -1 || __A_Scale == -1 ? null : ((float)(((float)__A_Level) / ((float)__A_Scale)));
}
```

## Development

### Internal api

The plugin relays on JEB internal api for some purposes. It was tested on JEB 3.24.

1. `UIUtils` depends on `OptionsForEnginesPluginDialog` for showing options dialog from script. As such, it also depends
   on SWT.
   
### Utils

You can use the script `JarLoader.py` to run a plugin directly from a jar. 
For that to work, you need to delete the current version from `coreplugins`, and have your code to have no reference after running.

## Wishlist

1. Use android api methods for naming. For example, `Intent::getAction` can be used to name a variable action
2. Create an informative name from short methods body