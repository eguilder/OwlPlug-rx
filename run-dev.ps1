$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$jar = Get-ChildItem ".\owlplug-client\target\owlplug-client-*.jar" |
  Where-Object { $_.Name -notlike "*.original" } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

if ($null -eq $jar) {
  throw "No runnable owlplug-client jar found. Run .\build-dev.ps1 first."
}

& "$env:JAVA_HOME\bin\java.exe" -jar $jar.FullName
