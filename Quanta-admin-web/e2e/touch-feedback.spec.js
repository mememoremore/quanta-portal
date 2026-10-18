import { test, expect, loginAs } from './support/fixture.js'

const tapColor = 'rgba(255, 103, 0, 0.16)'

test.describe('官网全站触摸反馈', () => {
  test.use({ viewport: { width: 390, height: 844 }, hasTouch: true })

  test('塔员触摸高亮为淡橙色，键盘焦点仍然可见', async ({ page, api }, testInfo) => {
    await loginAs(page, 'member')
    expect(api.requests).toContain('GET /getInfo')
    const brand = page.locator('.portal-header__brand')
    const menu = page.locator('.portal-header__mobile-trigger')
    await expect(brand).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await expect(brand.locator('img')).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await expect(menu).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await menu.tap()
    const community = page.getByTestId('portal-mobile-panel').getByRole('link', { name: '社区论坛', exact: true })
    await expect(community).toBeVisible()
    await expect(community).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await community.tap()
    await expect(page).toHaveURL(/\/member\/community$/)
    const publish = page.getByRole('link', { name: /发布帖子/ })
    await expect(publish).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await page.keyboard.press('Tab')
    await publish.focus()
    await expect(publish).toHaveCSS('outline-style', 'solid')
    await expect(publish).toHaveCSS('outline-color', 'rgba(255, 103, 0, 0.28)')
    await expect(page.locator('.el-overlay:visible')).toHaveCount(0)
    await expect(page.locator('.el-message')).toHaveCount(0)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path: testInfo.outputPath('member-touch-orange.png'), fullPage: true })
  })

  test('新生端及导航抽屉也使用淡橙色反馈', async ({ page, api }) => {
    await loginAs(page, 'freshman')
    expect(api.requests).toContain('GET /getInfo')
    await expect(page.locator('.portal-header__brand')).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await page.getByTestId('portal-mobile-menu').tap()
    const link = page.getByTestId('portal-mobile-panel').getByRole('link').first()
    await expect(link).toBeVisible()
    await expect(link).toHaveCSS('-webkit-tap-highlight-color', tapColor)
  })

  test('统一入口和三种登录页使用淡橙色反馈', async ({ page, api }) => {
    await page.goto('/')
    await expect(page.locator('a[href="/login/freshman"]')).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    for (const path of ['/login/freshman', '/login/member', '/admin/login']) {
      await page.goto(path)
      await expect(page.getByRole('textbox').first()).toHaveCSS('-webkit-tap-highlight-color', tapColor)
      await expect(page.getByRole('button', { name: /登录/ }).last()).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    }
    expect(api.requests.filter(item => item === 'GET /captchaImage')).toHaveLength(3)
  })

  test('管理端导航和挂载在 body 的下拉选项使用淡橙色反馈', async ({ page, api }) => {
    await loginAs(page, 'admin')
    const trigger = page.getByRole('button', { name: '打开导航菜单', exact: true })
    await expect(trigger).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await trigger.tap()
    const members = page.locator('.app-sidebar').getByRole('menuitem', { name: '成员管理', exact: true })
    await expect(members).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await members.tap()
    await expect(page).toHaveURL(/\/admin\/members$/)
    await page.locator('.member-mobile__filters .el-select').first().tap()
    const option = page.getByRole('option', { name: '后端部', exact: true })
    await expect(option).toBeVisible()
    await expect(option).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await option.tap()
    await expect(page.locator('.member-mobile__card').first()).toContainText('BACKEND')
    expect(api.requests).toContain('GET /qt/member/list')
  })

  test('OAuth 登录、授权和完成页使用淡橙色反馈', async ({ page, api }) => {
    const id = 'a'.repeat(43)
    await page.route(`http://127.0.0.1:18080/oauth2/interaction/${id}**`, async route => {
      const headers = {
        'access-control-allow-origin': 'http://127.0.0.1:5177',
        'access-control-allow-credentials': 'true',
        'access-control-allow-headers': 'authorization,content-type,x-trace-id,x-csrf-token',
        'access-control-allow-methods': 'GET,POST,OPTIONS',
      }
      if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
      const expiresAt = new Date(Date.now() + 600000).toISOString()
      const data = route.request().method() === 'POST'
        ? { status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt }
        : { interactionId: id, status: 'pending', expiresAt, csrfToken: 'c'.repeat(43), consentRequired: true, scopes: ['openid', 'profile'], client: { clientId: 'quanta-challenge', name: 'Quanta Challenge', origin: 'https://challenge.quantacenter.com', redirectUri: 'https://challenge.quantacenter.com/api/auth/quanta/callback' } }
      await route.fulfill({ status: 200, headers, json: { code: 200, data } })
    })
    await page.goto(`/oauth/authorize?interaction=${id}`)
    const login = page.getByRole('button', { name: '登录并继续', exact: true })
    await expect(login).toBeEnabled()
    await expect(login).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await page.getByPlaceholder('学号或用户名').fill('member_demo')
    await page.getByPlaceholder('请输入密码', { exact: true }).fill('demo-password')
    await login.tap()
    const approve = page.getByTestId('oauth-approve')
    await expect(approve).toBeVisible()
    await expect(approve).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    await approve.tap()
    await expect(page.getByRole('heading', { name: '登录成功，欢迎回来' })).toBeVisible()
    await expect(page.getByTestId('oauth-return')).toHaveCSS('-webkit-tap-highlight-color', tapColor)
    expect(api.requests).toContain('POST /login')
  })
})

test('桌面鼠标端不改变原生点按高亮', async ({ page, api }) => {
  await loginAs(page, 'member')
  expect(api.requests).toContain('GET /getInfo')
  await expect(page.locator('.portal-header__brand')).not.toHaveCSS('-webkit-tap-highlight-color', tapColor)
  await page.goto('/')
  await expect(page.locator('a[href="/login/freshman"]')).not.toHaveCSS('-webkit-tap-highlight-color', tapColor)
  await page.goto('/admin/login')
  await expect(page.getByRole('button', { name: '登录', exact: true })).not.toHaveCSS('-webkit-tap-highlight-color', tapColor)
})
