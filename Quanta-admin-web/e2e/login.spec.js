import { test, expect, loginAs } from './support/fixture.js'

test('新生首次登录直接到新生首页，不回到身份选择', async ({ page, api }) => {
  await loginAs(page, 'freshman')
  await expect(page).toHaveURL(/\/freshman\/home$/)
  await expect(page.getByRole('heading', { name: /让想法被看见/ })).toBeVisible()
  expect(api.requests).toContain('POST /login')
  expect(api.requests).toContain('GET /getInfo')
})

test('塔员首次登录直接到塔员首页，且不能切入新生门户', async ({ page, api }) => {
  await loginAs(page, 'member')
  await expect(page).toHaveURL(/\/member\/home$/)
  await expect(page.getByRole('heading', { name: /欢迎回来/ })).toBeVisible()
  await page.goto('/freshman/home')
  await expect(page).toHaveURL(/\/member\/home$/)
  expect(api.requests).toContain('GET /system/notice/listTop')
})

test('管理账号首次登录直接到控制台，不回到身份选择', async ({ page, api }) => {
  await loginAs(page, 'admin')
  await expect(page).toHaveURL(/\/admin\/dashboard$/)
  await expect(page.getByText('当届成员总数')).toBeVisible()
  expect(api.requests).toContain('GET /getRouters')
})

test('新生注册错误留在当前表单，不向后端提交', async ({ page, api }) => {
  await page.goto('/login/freshman')
  await page.getByTestId('register-mode').click()
  await page.locator('[name="studentNo"]').fill('123')
  await page.locator('[name="email"]').fill('not-an-email')
  await page.locator('[name="emailCode"]').fill('123')
  await page.locator('[name="registerPassword"]').fill('12345')
  await page.locator('[name="confirmPassword"]').fill('different')
  await page.getByTestId('register-submit').click()
  await expect(page.getByText('请输入11位学号，例如 20241003193').first()).toBeVisible()
  await expect(page.getByText('请输入6位邮箱验证码').first()).toBeVisible()
  await expect(page.getByText('两次输入的密码不一致').first()).toBeVisible()
  await expect(page).toHaveURL(/\/login\/freshman$/)
  expect(api.requests).not.toContain('POST /register')
})

test('新生申请邮箱验证码只调用测试 API，不发送真实邮件', async ({ page, api }) => {
  await page.goto('/login/freshman')
  await page.getByTestId('register-mode').click()
  await page.locator('[name="studentNo"]').fill('20241003193')
  await page.locator('[name="email"]').fill('student@example.test')
  await page.getByTestId('send-email-code').click()
  await expect(page.getByTestId('send-email-code')).toBeDisabled()
  expect(api.requests).toContain('POST /register/emailCode')
})

test('手机端新生登录后可以进入报名页', async ({ page, api }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await loginAs(page, 'freshman')
  await page.getByRole('link', { name: '加入我们' }).click()
  await expect(page).toHaveURL(/\/freshman\/recruitment$/)
  await expect(page.getByRole('button', { name: '填写报名' })).toBeVisible()
  expect(api.requests).toContain('GET /qt/interview/my')
})
