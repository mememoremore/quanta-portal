#Requires -Version 5.1
$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "build-release.ps1")

$EnvFile = Join-Path $PSScriptRoot "deploy.env.local"
if (-not (Test-Path $EnvFile)) {
    throw "缺少 $EnvFile，请复制 deploy.env.example 为 deploy.env.local 并填写 OSS_BUCKET"
}

$config = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line -match "^([^=]+)=(.*)$") {
        $config[$Matches[1].Trim()] = $Matches[2].Trim()
    }
}

$bucket = $config["OSS_BUCKET"]
$endpoint = $config["OSS_ENDPOINT"]
$prefix = if ($config["OSS_PREFIX"]) { $config["OSS_PREFIX"] } else { "deploy" }
$instanceId = $config["ECS_INSTANCE_ID"]
$region = $config["ECS_REGION"]

if (-not $bucket -or -not $endpoint) { throw "deploy.env.local 需配置 OSS_BUCKET、OSS_ENDPOINT" }

$ts = Get-Date -Format "yyyyMMddHHmmss"
$objectKey = "$prefix/quanta-portal-$ts.tar.gz"
$tarball = Join-Path $PSScriptRoot "release\quanta-portal.tar.gz"
$relName = "release-$ts"

$credPath = $config["ALIYUN_CREDENTIALS_FILE"]
if (-not $credPath) { $credPath = Join-Path $env:USERPROFILE ".aliyun\credentials" }
if (-not (Test-Path -LiteralPath $credPath)) { throw "找不到凭证文件: $credPath" }
$credVals = @{}
Get-Content -LiteralPath $credPath -Encoding UTF8 | ForEach-Object {
    $line = ($_.Trim() -replace [char]0x200B, '')
    if ($line -and -not $line.StartsWith("#") -and -not $line.StartsWith("[") -and $line -match "^([^=]+)=(.*)$") {
        $credVals[$Matches[1].Trim()] = $Matches[2].Trim()
    }
}
$ak = $credVals["access_key_id"]
$sk = $credVals["access_key_secret"]
if (-not $ak -or -not $sk) { throw "credentials 文件需包含 access_key_id、access_key_secret" }

$py = @"
import oss2
from pathlib import Path

ak = r'$($ak.Replace("'","''"))'
sk = r'$($sk.Replace("'","''"))'
auth = oss2.Auth(ak, sk)
bucket = oss2.Bucket(auth, 'https://$endpoint', r'$bucket')
local = Path(r'$($tarball.Replace("'","''"))')
key = r'$objectKey'
print('Uploading', key, '...')
bucket.put_object_from_file(key, str(local))
url = bucket.sign_url('GET', key, 3600)
Path(r'$($PSScriptRoot.Replace("'","''"))\release\.last-oss-url.txt').write_text(url, encoding='utf-8')
print('OSS_OBJECT', key)
print('SIGNED_URL', url)
"@

Write-Host ">> Upload to OSS..."
$py | python -
if ($LASTEXITCODE -ne 0) { throw "OSS upload failed (pip install oss2?)" }

$url = Get-Content (Join-Path $PSScriptRoot "release\.last-oss-url.txt") -Raw
$url = $url.Trim()

$remote = @"
REL=$relName
BASE=/opt/ruoyi/releases/`${REL}
mkdir -p "`${BASE}"
/usr/bin/curl -fsSL '$url' -o "`${BASE}/quanta-portal.tar.gz"
/usr/bin/tar -xzf "`${BASE}/quanta-portal.tar.gz" -C "`${BASE}"
if [ -x /opt/ruoyi/remote-deploy.sh ]; then
  /bin/bash /opt/ruoyi/remote-deploy.sh "`${REL}"
else
  echo "缺少 /opt/ruoyi/remote-deploy.sh，请按 docs/quanta-portal-deploy-oss-oos.md 一次性安装"
  exit 1
fi
echo DEPLOY_OK "`${REL}"
"@

$cmdFile = Join-Path $PSScriptRoot "release\last-oos-command.sh"
Set-Content -Path $cmdFile -Value $remote -Encoding UTF8

Write-Host ""
Write-Host "========== 云助手命令（实例 $instanceId，地域 $region）=========="
Write-Host $remote
Write-Host "================================================================"
Write-Host "已保存: $cmdFile"
Write-Host "OSS 对象: $objectKey"
