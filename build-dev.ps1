$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
$maven = Join-Path $repoRoot "mvnw.cmd"

if (-not (Test-Path -LiteralPath $maven -PathType Leaf)) {
  throw "Maven Wrapper not found: $maven"
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
  $java = Get-Command java.exe -ErrorAction SilentlyContinue
  if ($null -eq $java) {
    throw "Java was not found. Set JAVA_HOME or install a JDK before building."
  }
  $env:JAVA_HOME = Split-Path (Split-Path $java.Source -Parent) -Parent
}

$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Push-Location $repoRoot
try {
  $scannerVersion = (Get-Content ".\owlplug-host\src\main\resources\owlplug-scanner.version" -Raw).Trim()
  $scannerPlatform = "win-x64"
  $scannerName = "owlplug-scanner-$scannerVersion-$scannerPlatform"
  $scannerResource = ".\owlplug-host\src\main\resources\$scannerName"

  if (-not (Test-Path -LiteralPath $scannerResource)) {
    $scannerUrl = "https://github.com/OwlPlug/owlplug-scanner/releases/download/$scannerVersion/$scannerName"
    Write-Host "Downloading $scannerName"
    Invoke-WebRequest -Uri $scannerUrl -OutFile $scannerResource
  }

  Write-Host "Building with Maven Wrapper"
  & $maven clean install
  if ($LASTEXITCODE -ne 0) {
    Write-Warning "Clean build failed, likely because a target file is locked. Retrying without clean."
    & $maven install
    if ($LASTEXITCODE -ne 0) {
      exit $LASTEXITCODE
    }
  }

  Push-Location ".\owlplug-client"
  try {
    & $maven install spring-boot:repackage
    if ($LASTEXITCODE -ne 0) {
      exit $LASTEXITCODE
    }
  } finally {
    Pop-Location
  }
} finally {
  Pop-Location
}
