#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$ReleaseDir = Join-Path $PSScriptRoot "release"
$Staging = Join-Path $ReleaseDir "staging"
$Tarball = Join-Path $ReleaseDir "quanta-portal.tar.gz"
$SqlSrc = Join-Path $Root "sql"
$Web = Join-Path $Root "Quanta-admin-web"
$Jar = Join-Path $Root "ruoyi-admin\target\ruoyi-admin.jar"
if (-not (Test-Path $Jar)) { throw "Missing $Jar — run mvn package first" }
if (-not (Test-Path (Join-Path $Web "dist\index.html"))) { throw "Missing frontend dist — run npm run build first" }
if (Test-Path $Staging) { Remove-Item -Recurse -Force $Staging }
New-Item -ItemType Directory -Force -Path (Join-Path $Staging "sql") | Out-Null
Copy-Item $Jar (Join-Path $Staging "ruoyi-admin.jar")
Get-ChildItem $SqlSrc -Filter "*.sql" | Copy-Item -Destination (Join-Path $Staging "sql")
Push-Location (Join-Path $Web "dist")
& tar -czf (Join-Path $Staging "admin-dist.tgz") .
Pop-Location
if (Test-Path $Tarball) { Remove-Item -Force $Tarball }
& tar -czf $Tarball -C $Staging .
$size = (Get-Item $Tarball).Length / 1MB
Write-Host ">> OK: $Tarball ($([math]::Round($size, 1)) MB)"
