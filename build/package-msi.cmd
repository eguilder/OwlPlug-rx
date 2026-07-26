@echo off

set owlplug-version=%1
set target-platform=%2

echo ** Preparing package **

echo ** Exporting LICENSE File **

copy "..\LICENSE" ".\input\LICENSE"

echo ** Copying owlplug-client-%owlplug-version%.jar to owlplug.jar **

copy "..\owlplug-client\target\owlplug-client-%owlplug-version%.jar" ".\input\owlplug.jar"

echo ** Generating OwlPlug-rx MSI Install package **

jpackage --type msi --input ./input/ --name OwlPlug-rx --main-class org.springframework.boot.loader.launch.JarLauncher ^
--main-jar owlplug.jar --license-file .\input\LICENSE --dest ./output ^
--app-version %owlplug-version% --icon .\resources\owlplug.ico --vendor OwlPlug-rx ^
--win-dir-chooser --win-menu --win-shortcut

move ".\output\OwlPlug-rx-%owlplug-version%.msi" ".\output\OwlPlug-rx-%owlplug-version%-%target-platform%.msi"

echo ** OwlPlug-rx MSI Install package generated**
