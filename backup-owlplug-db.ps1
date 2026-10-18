[CmdletBinding()]
param(
    [string]$DatabaseDirectory = (Join-Path $env:USERPROFILE ".owlplug"),
    [string]$BackupDirectory = $null,
    [switch]$AllowOpenDatabase
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BackupDirectory)) {
    $BackupDirectory = Join-Path $DatabaseDirectory "backups"
}

if (-not (Test-Path -LiteralPath $DatabaseDirectory -PathType Container)) {
    throw "OwlPlug database directory was not found: $DatabaseDirectory"
}

$lockFile = Join-Path $DatabaseDirectory "owlplug.lock.db"
if ((Test-Path -LiteralPath $lockFile -PathType Leaf) -and -not $AllowOpenDatabase) {
    throw "The OwlPlug database appears to be in use. Close OwlPlug and run the script again. Use -AllowOpenDatabase only if you accept the risk of an inconsistent backup."
}

$databaseFiles = @(Get-ChildItem -LiteralPath $DatabaseDirectory -File | Where-Object {
    $_.Name -like "owlplug*.db" -and $_.Name -notlike "*.lock.db"
})

if ($databaseFiles.Count -eq 0) {
    throw "No OwlPlug database files were found in: $DatabaseDirectory"
}

New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupPath = Join-Path $BackupDirectory "owlplug-db-$timestamp.zip"
$stagingDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "owlplug-db-backup-$([guid]::NewGuid().ToString('N'))"

try {
    New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null

    foreach ($databaseFile in $databaseFiles) {
        Copy-Item -LiteralPath $databaseFile.FullName -Destination $stagingDirectory
    }

    Compress-Archive -Path (Join-Path $stagingDirectory "*") -DestinationPath $backupPath -CompressionLevel Optimal
    $hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash

    Write-Output "OwlPlug database backup created: $backupPath"
    Write-Output "Files backed up: $($databaseFiles.Name -join ', ')"
    Write-Output "SHA-256: $hash"
}
finally {
    if (Test-Path -LiteralPath $stagingDirectory) {
        Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
    }
}
