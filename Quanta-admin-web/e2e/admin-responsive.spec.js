import { test, expect, loginAs } from './support/fixture.js'

async function expectInsideMain(page, selector) {
  const main = await page.locator('.app-main').boundingBox()
  const boxes = await page.locator(selector).all()
  expect(boxes.length).toBeGreaterThan(0)
  for (const locator of boxes) {
    const box = await locator.boundingBox()
    expect(box).not.toBeNull()
    expect(box.x).toBeGreaterThanOrEqual(main.x - 1)
    expect(box.x + box.width).toBeLessThanOrEqual(main.x + main.width + 1)
  }
  const sizes = await page.locator('.app-main').evaluate((element) => ({ width: element.clientWidth, content: element.scrollWidth }))
  expect(sizes.content).toBeLessThanOrEqual(sizes.width + 1)
}

for (const width of [320, 390, 430, 768, 1440]) {
  test(`控制台在 ${width}px 不溢出并完整显示三张统计卡`, async ({ page, api }, testInfo) => {
    await page.setViewportSize({ width, height: 844 })
    await loginAs(page, 'admin')
    await expect(page).toHaveURL(/\/admin\/dashboard$/)
    await expect(page.locator('.stat-card').first()).toContainText('12')
    await expect(page.locator('.stat-card')).toHaveCount(3)
    await expectInsideMain(page, '.welcome-banner, .stat-card')
    await expect(page.locator('.stat-card p').first()).toHaveCSS('font-size', '14px')
    await expect(page.locator('.stat-card strong').first()).toHaveCSS('font-size', width <= 600 ? '28px' : '33px')
    await expect(page.locator('.stat-card__icon').first()).toHaveCSS('width', width <= 600 ? '44px' : '48px')
    await expect(page.locator('.stat-card__icon .el-icon').first()).toHaveCSS('font-size', width <= 600 ? '24px' : '26px')
    await expect(page.locator('.welcome-banner__mark')).toHaveCSS('width', width <= 600 ? '55px' : '120px')
    await expect(page.locator('.welcome-banner__brand')).toHaveAttribute('aria-hidden', 'true')
    const columns = await page.locator('.dashboard-view__stats').evaluate((element) => getComputedStyle(element).gridTemplateColumns.split(' ').length)
    expect(columns).toBe(width <= 600 ? 1 : width <= 1280 ? 2 : 3)
    if (width === 390 || width === 1440) {
      await expect(page.locator('#nprogress')).toHaveCount(0)
      await expect(page.locator('.el-message')).toHaveCount(0)
      await page.screenshot({ path: testInfo.outputPath(`admin-dashboard-polished-${width}.png`) })
    }
    if (width === 1440) {
      await page.emulateMedia({ reducedMotion: 'reduce' })
      await expect(page.locator('.welcome-banner__mark')).toHaveCSS('transition-duration', '0s')
    }
    expect(api.requests).toContain('GET /dashboard/stats')
  })
}

test('手机成员卡片完整显示字段，筛选、搜索、分页和桌面切换正常', async ({ page, api }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await loginAs(page, 'admin')
  await page.goto('/admin/members')
  const cards = page.locator('.member-mobile__card')
  await expect(cards).toHaveCount(10)
  const first = cards.first()
  await expect(first).toContainText('测试成员1')
  for (const text of ['产品', '经理', '第21届', '138****0000', '2026-09-23 18:30:00', '在职']) {
    await expect(first).toContainText(text)
  }
  await expect(first.getByRole('button', { name: '删除', exact: true })).toBeVisible()
  await expect(first.getByRole('button', { name: '重置密码', exact: true })).toBeVisible()
  await expectInsideMain(page, '.members-card, .member-mobile__card')
  await page.screenshot({ path: testInfo.outputPath('admin-members-h5.png') })

  const pagination = page.locator('.members-card__footer .el-pagination')
  await pagination.locator('.el-pager li').filter({ hasText: /^2$/ }).click()
  await expect(cards.first()).toContainText('测试成员11')
  await pagination.scrollIntoViewIfNeeded()
  const main = await page.locator('.app-main').boundingBox()
  const footer = await pagination.boundingBox()
  expect(footer.y + footer.height).toBeLessThanOrEqual(main.y + main.height + 1)

  await page.locator('.member-mobile__filters .el-select').first().click()
  await page.getByRole('option', { name: '后端', exact: true }).click()
  await expect(cards.first()).toContainText('测试成员2')
  await expect(cards.first()).toContainText('后端')
  await expect(page.locator('.members-card__mobile-total')).toContainText('11')

  await page.getByRole('textbox', { name: '搜索成员姓名' }).fill('测试成员22')
  await expect(cards).toHaveCount(1)
  await expect(cards.first()).toContainText('测试成员22')
  await page.setViewportSize({ width: 320, height: 700 })
  await expectInsideMain(page, '.members-card, .member-mobile__card')
  await page.setViewportSize({ width: 1440, height: 900 })
  await expect(page.locator('.member-mobile')).toHaveCount(0)
  await expect(page.locator('.member-table')).toBeVisible()
  await expect(page.locator('.member-table')).toContainText('测试成员22')
  expect(api.requests).toContain('GET /qt/member/list')
})
