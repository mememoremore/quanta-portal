import { test, expect } from './support/fixture.js'

test('统一入口展示三种身份，链接分别指向正确登录页', async ({ page, api }) => {
  await page.goto('/')
  await expect(page.locator('a[href="/login/freshman"]')).toContainText('新生入口')
  await expect(page.locator('a[href="/login/member"]')).toContainText('塔员入口')
  await expect(page.locator('a[href="/admin/login"]')).toContainText('管理入口')
  await page.locator('a[href="/admin/login"]').click()
  await expect(page).toHaveURL(/\/admin\/login$/)
  expect(api.requests).toContain('GET /captchaImage')
})

test('未登录访问受保护页面会跳到各自的登录页', async ({ page, api }) => {
  for (const [target, login] of [
    ['/freshman/recruitment', '/login/freshman'],
    ['/member/community', '/login/member'],
    ['/admin/recruitment', '/admin/login'],
  ]) {
    await page.goto(target)
    await expect(page).toHaveURL(new RegExp(`${login.replaceAll('/', '\\/')}\\?redirect=`))
  }
  expect(api.requests.filter((item) => item === 'GET /captchaImage')).toHaveLength(3)
})

test('手机端入口可以滚动到第三张管理卡片', async ({ page, api }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  const adminEntry = page.locator('a[href="/admin/login"]')
  await adminEntry.scrollIntoViewIfNeeded()
  await expect(adminEntry).toBeInViewport()
  await adminEntry.click()
  await expect(page).toHaveURL(/\/admin\/login$/)
  expect(api.unexpected).toEqual([])
})
