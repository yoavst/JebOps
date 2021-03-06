# Yoav's Jeb plugins

TODO write readme

## Internal api

The plugin relay on JEB internal api for some purposes. It was tested on JEB 3.24 Here is the list of the cases:

1. `UIUtils` depends on `OptionsForEnginesPluginDialog` for showing options dialog from script. As such, it also depends
   on SWT.