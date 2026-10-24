import { test, expect, loginAs } from './support/fixture.js'

async function submitMemberLogin(page, username = 'admin_demo', redirect = '') {
  await page.goto(`/login/member${redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''}`)
  await page.getByPlaceholder('请输入学号或用户名').fill(username)
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('demo-password')
  await page.getByRole('button', { name: '登录并进入塔员门户', exact: true }).click()
}

for (const width of [1200, 390]) {
  for (const username of ['admin_demo', 'ceo_demo', 'viewer_demo', 'member_demo']) {
    test(`${username} signing in through member login stays in the member portal at ${width}px`, async ({ page, api }) => {
      await page.setViewportSize({ width, height: 900 })
      await submitMemberLogin(page, username)
      await expect(page).toHaveURL(/\/member\/home$/)
      await expect(page.getByRole('heading', { name: /欢迎回来/ })).toBeVisible()
      expect(api.requests).not.toContain('GET /getRouters')
      await page.reload()
      await expect(page).toHaveURL(/\/member\/home$/)
      await expect(page.getByRole('heading', { name: /欢迎回来/ })).toBeVisible()
    })
  }
  test(`built-in admin identity userId=1 stays in member portal at ${width}px`, async ({ page, api }) => {
    api.userInfoOverrides = { userId: 1, userName: 'admin' }
    await page.setViewportSize({ width, height: 900 })
    await submitMemberLogin(page)
    await expect(page).toHaveURL(/\/member\/home$/)
    await expect(page.getByRole('heading', { name: /欢迎回来/ })).toBeVisible()
    expect(api.requests).not.toContain('GET /getRouters')
  })
}

for (const audience of ['admin', 'ceo', 'viewer']) {
  test(`an existing ${audience} admin session can deliberately enter member login`, async ({ page, api }) => {
    await loginAs(page, audience)
    expect(api.requests).toContain('GET /getRouters')
    await page.goto('/')
    await page.locator('a[href="/login/member"]').click()
    await expect(page).toHaveURL(/\/member\/home$/)
    await expect(page.getByRole('heading', { name: /欢迎回来/ })).toBeVisible()
  })
}

for (const redirect of ['/admin/dashboard', '/admin/login', '/login', '/', '//example.invalid/steal']) {
  test(`admin member login rejects cross-entry redirect ${redirect}`, async ({ page, api }) => {
    await submitMemberLogin(page, 'admin_demo', redirect)
    await expect(page).toHaveURL(/\/member\/home$/)
    expect(api.requests).not.toContain('GET /getRouters')
  })
}

test('admin member login preserves a member-only return URL', async ({ page, api }) => {
  await submitMemberLogin(page, 'admin_demo', '/member/community?scope=favorites')
  await expect(page).toHaveURL(/\/member\/community\?scope=favorites$/)
  expect(api.requests).not.toContain('GET /getRouters')
})

test('a remembered admin entry does not override explicit member login', async ({ page, api }) => {
  await page.addInitScript(() => localStorage.setItem('quanta_session_audience', 'admin'))
  await submitMemberLogin(page)
  await expect(page).toHaveURL(/\/member\/home$/)
  expect(api.requests).not.toContain('GET /getRouters')
})

test('a refreshed admin session stays in the requested member portal', async ({ page, api }) => {
  api.refreshAccount = 'admin_demo'
  await page.addInitScript(() => localStorage.setItem('quanta_session_audience', 'admin'))
  await page.goto('/member/home')
  await expect(page).toHaveURL(/\/member\/home$/)
  await expect(page.getByRole('heading', { name: /欢迎回来/ })).toBeVisible()
  expect(api.requests).toContain('POST /auth/refresh')
  expect(api.requests).not.toContain('GET /getRouters')
})

test('legacy admin user info without the member flag retains the member portal', async ({ page, api }) => {
  api.userInfoOverrides = { isQuantaMember: undefined }
  await submitMemberLogin(page)
  await expect(page).toHaveURL(/\/member\/home$/)
  expect(api.requests).not.toContain('GET /getRouters')
})

for (const scenario of ['missing', '401', '500', 'mismatched']) {
  test(`admin member login with ${scenario} identity fails at the member login`, async ({ page, api }) => {
    if (scenario === 'missing') api.userInfoMissing = true
    else if (scenario === 'mismatched') api.userInfoOverrides = { isQuantaMember: 0 }
    else api.userInfoFailure = Number(scenario)
    await submitMemberLogin(page)
    await expect(page.locator('.el-message--error').first()).toBeVisible()
    await expect(page).toHaveURL(/\/login\/member(?:\?.*)?$/)
    await expect(page.getByRole('button', { name: '登录并进入塔员门户', exact: true })).toBeEnabled()
    await expect(page.locator('.el-message--success')).toHaveCount(0)
    expect(api.requests).not.toContain('GET /getRouters')
  })
}

test('restoring an admin token without user info returns to member login, not admin', async ({ page, api }) => {
  api.refreshAccount = 'admin_demo'
  api.userInfoMissing = true
  await page.goto('/member/home')
  await expect(page).toHaveURL(/\/login\/member\?redirect=/)
  expect(api.requests).not.toContain('GET /getRouters')
})
