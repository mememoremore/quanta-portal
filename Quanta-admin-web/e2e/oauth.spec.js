import { test, expect } from '@playwright/test'

const id = 'a'.repeat(43)
const apiOrigin = 'http://127.0.0.1:18080'
async function setup(page, { member = '1', expired = false, missing = false, authenticated = true } = {}) {
  const calls = []
  await page.addInitScript(signedIn => {
    if (signedIn) localStorage.setItem('quanta_admin_token', 'oauth-browser-fixture')
    else localStorage.removeItem('quanta_admin_token')
  }, authenticated)
  await page.route(`${apiOrigin}/**`, async route => {
    const request = route.request(), path = new URL(request.url()).pathname
    const headers = { 'access-control-allow-origin': 'http://127.0.0.1:5177', 'access-control-allow-credentials': 'true', 'access-control-allow-headers': 'authorization,content-type,x-trace-id,x-csrf-token,istoken', 'access-control-allow-methods': 'GET,POST,OPTIONS' }
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
    calls.push(`${request.method()} ${path}`)
    if (path.endsWith('/continue')) return route.fulfill({ status: 200, headers, contentType: 'text/html; charset=utf-8', body: '<h1>测试授权续接端点</h1>' })
    const expiresAt = new Date(Date.now() + (expired ? -1000 : 600000)).toISOString()
    let payload
    if (path === `/oauth2/interaction/${id}`) payload = { code: missing ? 503 : 200, msg: '授权服务尚未就绪', data: { interactionId: id, status: 'pending', expiresAt, csrfToken: 'c'.repeat(43), consentRequired: true, scopes: ['openid', 'profile'], client: { clientId: 'quanta-challenge', name: 'Quanta Challenge', origin: 'https://challenge.quantacenter.com', redirectUri: 'https://challenge.quantacenter.com/api/auth/quanta/callback' } } }
    else if (path === '/captchaImage') payload = { code: 200, captchaEnabled: false }
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

async function swipeOAuthPage(page, direction) {
  const scroller = page.locator('.oauth-page')
  const atEdge = await scroller.evaluate((element, swipeDirection) => swipeDirection === 'up'
    ? element.scrollTop >= element.scrollHeight - element.clientHeight - 1
    : element.scrollTop <= 1, direction)
  if (atEdge) return
  const session = await page.context().newCDPSession(page)
  const { width, height } = page.viewportSize()
  const x = width / 2
  const from = direction === 'up' ? height * .8 : height * .22
  const to = direction === 'up' ? height * .22 : height * .8
  try {
    await session.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y: from, id: 1 }] })
    for (let step = 1; step <= 8; step++) {
      await session.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x, y: from + (to - from) * step / 8, id: 1 }] })
      await page.evaluate(() => new Promise(requestAnimationFrame))
    }
    // Hold at the endpoint before releasing to avoid an inertial fling consuming the next tap.
    await page.evaluate(async () => {
      for (let frame = 0; frame < 12; frame++) await new Promise(requestAnimationFrame)
    })
    await session.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] })
    await page.evaluate(() => new Promise(requestAnimationFrame))
  } finally {
    await session.detach()
  }
}

test.describe('OAuth页面滚动', () => {
  test.use({ hasTouch: true, isMobile: true })

  for (const viewport of [{ width: 390, height: 600 }, { width: 320, height: 480 }, { width: 844, height: 390 }]) {
    test(`手机 ${viewport.width}x${viewport.height} 手指可上下滚动并完成授权`, async ({ page }, info) => {
      await page.setViewportSize(viewport)
      const calls = await setup(page)
      await page.goto(`/oauth/authorize?interaction=${id}`)
      const scroller = page.locator('.oauth-page')
      await expect(page.getByTestId('oauth-approve')).toBeVisible()
      await expect(page.locator('#nprogress')).toHaveCount(0)
      // Body remains locked for the admin shell; OAuth must own a bounded scroll container.
      await expect(page.locator('body')).toHaveCSS('overflow-y', 'hidden')
      expect(await scroller.evaluate(element => element.scrollHeight > element.clientHeight)).toBe(true)
      await swipeOAuthPage(page, 'up')
      await expect.poll(() => scroller.evaluate(element => element.scrollTop)).toBeGreaterThan(20)
      const downPosition = await scroller.evaluate(element => element.scrollTop)
      await swipeOAuthPage(page, 'down')
      await expect.poll(() => scroller.evaluate(element => element.scrollTop)).toBeLessThan(downPosition)
      for (let swipe = 0; swipe < 3; swipe++) await swipeOAuthPage(page, 'up')
      await expect(page.locator('.oauth-footer')).toBeInViewport()
      await expect(page.getByTestId('oauth-approve')).toBeInViewport({ ratio: 1 })
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
      await page.screenshot({ path: info.outputPath(`oauth-scroll-${viewport.width}x${viewport.height}.png`) })
      await page.getByTestId('oauth-approve').tap()
      await expect(page).toHaveURL(`${apiOrigin}/oauth2/interaction/${id}/continue`)
      expect(calls.filter(call => call.endsWith('/continue'))).toHaveLength(1)
    })
  }

  test('手机登录页在可视高度缩小后仍可滑到登录按钮和页尾', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 700 })
    const calls = await setup(page, { authenticated: false })
    await page.goto(`/oauth/authorize?interaction=${id}`)
    await expect(page.getByRole('heading', { name: '登录塔员账号' })).toBeVisible()
    await expect(page.getByRole('button', { name: '登录并继续' })).toBeEnabled()
    await page.getByPlaceholder('学号或用户名').focus()
    // Shrinking the visual area covers keyboard/browser chrome changes without a physical keyboard dependency.
    await page.setViewportSize({ width: 390, height: 320 })
    await page.getByPlaceholder('学号或用户名').blur()
    const scroller = page.locator('.oauth-page')
    expect(await scroller.evaluate(element => element.scrollHeight > element.clientHeight)).toBe(true)
    for (let swipe = 0; swipe < 3; swipe++) await swipeOAuthPage(page, 'up')
    await expect.poll(() => scroller.evaluate(element => element.scrollTop)).toBeGreaterThan(20)
    await expect(page.getByRole('button', { name: '登录并继续' })).toBeInViewport({ ratio: 1 })
    await expect(page.locator('.oauth-footer')).toBeInViewport()
    expect(calls).toContain('GET /captchaImage')
  })
})
