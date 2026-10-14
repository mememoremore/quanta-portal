param(
    [string]$BaseUrl = 'http://127.0.0.1:5173',
    [string]$Password = $env:QUANTA_SMOKE_PASSWORD
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($Password)) {
    $securePassword = Read-Host -Prompt '请输入本地测试账号密码' -AsSecureString
    $Password = [System.Net.NetworkCredential]::new('', $securePassword).Password
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    throw '未提供本地测试账号密码，冒烟测试已停止。'
}

$passwordLiteral = ConvertTo-Json -InputObject $Password -Compress
$session = 'quanta-portal-smoke'
$npx = if (Get-Command npx.cmd -ErrorAction SilentlyContinue) { 'npx.cmd' } else { 'npx' }

& $npx --no-install playwright-cli "-s=$session" close 2>$null
& $npx --no-install playwright-cli "-s=$session" open "$BaseUrl/"

$script = @"
async page => {
  const result = [];
  const login = async (path, username, buttonName, expectedPath) => {
    await page.goto('$BaseUrl' + path);
    await page.getByRole('textbox', { name: /用户名/ }).fill(username);
    await page.getByRole('textbox', { name: /密码/ }).fill($passwordLiteral);
    await page.getByRole('button', { name: buttonName }).click();
    await page.waitForURL('**' + expectedPath, { timeout: 15000 });
    result.push({ username, path: new URL(page.url()).pathname });
  };

  await page.getByRole('link', { name: /新生入口/ }).waitFor();
  await page.getByRole('link', { name: /塔员入口/ }).waitFor();
  await page.getByRole('link', { name: /管理入口/ }).waitFor();

  await login('/login/freshman', 'qt_fresh', /登录并进入新生门户/, '/freshman/home');
  for (const path of ['/freshman/recruitment', '/freshman/events', '/freshman/security']) {
    await page.goto('$BaseUrl' + path);
    await page.locator('main').waitFor();
    result.push({ path, title: await page.title() });
  }

  await page.evaluate(() => localStorage.clear());
  await login('/login/member', 'qt_member', /登录并进入塔员门户/, '/member/home');
  for (const path of ['/member/directory', '/member/profile', '/member/materials', '/member/community', '/member/services', '/member/library', '/member/clothing', '/member/security']) {
    await page.goto('$BaseUrl' + path);
    await page.locator('main').waitFor();
    result.push({ path, title: await page.title() });
  }

  await page.evaluate(() => localStorage.clear());
  await login('/admin/login', 'admin', /登录/, '/admin/dashboard');
  result.push({ path: '/admin/dashboard', title: await page.title() });
  return result;
}
"@

& $npx --no-install playwright-cli "-s=$session" run-code $script
& $npx --no-install playwright-cli "-s=$session" console error
