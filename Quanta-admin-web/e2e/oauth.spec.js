import { test, expect } from '@playwright/test'

const id = 'a'.repeat(43)
const apiOrigin = 'http://127.0.0.1:18080'
async function setup(page, { member = '1', expired = false, missing = false } = {}) {
  const calls = []
  await page.addInitScript(() => localStorage.setItem('quanta_admin_token', 'oauth-browser-fixture'))
  await page.route(`${apiOrigin}/**`, async route => {
    const request = route.request(), path = new URL(request.url()).pathname
    const headers = { 'access-control-allow-origin': 'http://127.0.0.1:5177', 'access-control-allow-credentials': 'true', 'access-control-allow-headers': 'authorization,content-type,x-trace-id,x-csrf-token,istoken', 'access-control-allow-methods': 'GET,POST,OPTIONS' }
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
    calls.push(`${request.method()} ${path}`)
    if (path.endsWith('/continue')) return route.fulfill({ status: 200, headers, contentType: 'text/html; charset=utf-8', body: '<h1>测试授权续接端点</h1>' })
    const expiresAt = new Date(Date.now() + (expired ? -1000 : 600000)).toISOString()
    let payload
    if (path === `/oauth2/interaction/${id}`) payload = { code: missing ? 503 : 200, msg: '授权服务尚未就绪', data: { interactionId: id, status: 'pending', expiresAt, csrfToken: 'c'.repeat(43), consentRequired: true, scopes: ['openid', 'profile'], client: { clientId: 'quanta-challenge', name: 'Quanta Challenge', origin: 'https://challenge.quantacenter.com', redirectUri: 'https://challenge.quantacenter.com/api/auth/quanta/callback' } } }
    else if (path === '/getInfo') payload = { code: 200, user: { userId: 1, nickName: '测试塔员', communityNickname: '不应展示的昵称', isQuantaMember: member }, roles: ['ceo'], permissions: [] }
    else if (path.endsWith('/decision')) {
      expect(request.headers()['x-csrf-token']).toBe('c'.repeat(43))
      payload = { code: 200, data: { status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt } }
    } else payload = { code: 599, msg: 'Unexpected test request' }
    return route.fulfill({ status: 200, contentType: 'application/json', headers, body: JSON.stringify(payload) })
  })
  return calls
}

for (const width of [1440, 390]) {
  test(`官网授权 ${width}px：完成后立即安全续接，不显示成功卡片`, async ({ page }, info) => {
    await page.setViewportSize({ width, height: 940 })
    const calls = await setup(page)
    await page.goto(`/oauth/authorize?interaction=${id}`)
    await expect(page.getByText('测试塔员', { exact: true })).toBeVisible()
    await expect(page.getByText('不应展示的昵称')).toHaveCount(0)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await expect(page.locator('#nprogress')).toHaveCount(0)
    await page.screenshot({ path: info.outputPath(`oauth-consent-${width}.png`) })
    await page.getByTestId('oauth-approve').click()
    await expect(page).toHaveURL(`${apiOrigin}/oauth2/interaction/${id}/continue`)
    await expect(page.getByRole('heading', { name: '测试授权续接端点' })).toBeVisible()
    expect(calls.filter(call => call.endsWith('/continue'))).toHaveLength(1)
  })
}
test('非塔员即使有 CEO 角色也不展示授权按钮', async ({ page }) => {
  await setup(page, { member: '0' }); await page.goto(`/oauth/authorize?interaction=${id}`)
  await expect(page.getByRole('heading', { name: '该登录方式仅向塔员开放' })).toBeVisible()
  await expect(page.getByTestId('oauth-approve')).toHaveCount(0)
})
test('过期或后端未就绪时不显示成功', async ({ page }) => {
  await setup(page, { expired: true }); await page.goto(`/oauth/authorize?interaction=${id}`)
  await expect(page.getByText('本次授权已过期', { exact: false })).toBeVisible()
  await expect(page.getByTestId('oauth-approve')).toHaveCount(0)
})
