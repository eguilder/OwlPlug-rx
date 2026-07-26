$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
$env:MAVEN_HOME = "$env:USERPROFILE\.local\tools\apache-maven-3.9.11"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

$scannerVersion = (Get-Content ".\owlplug-host\src\main\resources\owlplug-scanner.version" -Raw).Trim()
$scannerPlatform = "win-x64"
$scannerName = "owlplug-scanner-$scannerVersion-$scannerPlatform"
$scannerResource = ".\owlplug-host\src\main\resources\$scannerName"

if (-not (Test-Path -LiteralPath $scannerResource)) {
  $scannerUrl = "https://github.com/OwlPlug/owlplug-scanner/releases/download/$scannerVersion/$scannerName"
  Write-Host "Downloading $scannerName"
  Invoke-WebRequest -Uri $scannerUrl -OutFile $scannerResource
}

& "$env:MAVEN_HOME\bin\mvn.cmd" clean install

Push-Location ".\owlplug-client"
try {
  & "$env:MAVEN_HOME\bin\mvn.cmd" clean install spring-boot:repackage
} finally {
  Pop-Location
}
