$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
$env:MAVEN_HOME = "$env:USERPROFILE\.local\tools\apache-maven-3.9.11"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

& "$env:MAVEN_HOME\bin\mvn.cmd" clean install

Push-Location ".\owlplug-client"
try {
  & "$env:MAVEN_HOME\bin\mvn.cmd" clean install spring-boot:repackage
} finally {
  Pop-Location
}
