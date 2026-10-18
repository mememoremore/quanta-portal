#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

Write-Host ">> Maven package (skip tests)..."
& mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "mvn package failed" }

$Web = Join-Path $Root "Quanta-admin-web"
Write-Host ">> Frontend build..."
if (-not $env:NODE_OPTIONS) { $env:NODE_OPTIONS = "--max-old-space-size=8192" }
Push-Location $Web
& npm run build
if ($LASTEXITCODE -ne 0) { throw "npm run build failed" }
Pop-Location

$ReleaseDir = Join-Path $PSScriptRoot "release"
$Staging = Join-Path $ReleaseDir "staging"
$Tarball = Join-Path $ReleaseDir "quanta-portal.tar.gz"
$SqlSrc = Join-Path $Root "sql"

if (Test-Path $Staging) { Remove-Item -Recurse -Force $Staging }
New-Item -ItemType Directory -Force -Path (Join-Path $Staging "sql") | Out-Null

Copy-Item (Join-Path $Root "ruoyi-admin\target\ruoyi-admin.jar") (Join-Path $Staging "ruoyi-admin.jar")
Get-ChildItem $SqlSrc -Filter "*.sql" | Copy-Item -Destination (Join-Path $Staging "sql")

Push-Location (Join-Path $Web "dist")
& tar -czf (Join-Path $Staging "admin-dist.tgz") .
Pop-Location

if (Test-Path $Tarball) { Remove-Item -Force $Tarball }
& tar -czf $Tarball -C $Staging .
$size = (Get-Item $Tarball).Length / 1MB
Write-Host ">> OK: $Tarball ($([math]::Round($size, 1)) MB)"
