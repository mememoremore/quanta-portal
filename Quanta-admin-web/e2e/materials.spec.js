import { test, expect, loginAs } from './support/fixture.js'

for (const audience of ['admin', 'member']) {
  test(`${audience} 资料搜索清空恢复原类型、切换部门和旧资料可见`, async ({ page, api }) => {
    await loginAs(page, audience)
    await page.goto(audience === 'admin' ? '/admin/learning-materials' : '/member/materials')
    const content = audience === 'admin' ? page.locator('.learning-materials-view') : page.locator('.member-materials')
    await expect(content).toContainText('历史未分类资料.pdf')
    if (audience === 'member') await page.getByRole('tab', { name: '前端', exact: true }).click()
    const types = page.getByRole('group', { name: '资料类型', exact: true })
    await types.getByRole('button', { name: '培训资料', exact: true }).click()
    await expect(content).toContainText('Vue 入门培训.pdf')
    await expect(content).not.toContainText('浏览器技术分享.pdf')
    const search = page.getByRole(audience === 'member' ? 'searchbox' : 'textbox', { name: '搜索资料名称' })
    await search.fill('浏览器')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await expect(content).toContainText('浏览器技术分享.pdf')
    await expect(content).toContainText('当前部门全部类型的搜索结果')
    await search.fill('')
    await expect(content).toContainText('Vue 入门培训.pdf')
    await expect(content).not.toContainText('浏览器技术分享.pdf')
    await expect(types.getByRole('button', { name: '培训资料', exact: true })).toHaveAttribute('aria-pressed', 'true')
    if (audience === 'member') await page.getByRole('tab', { name: '后端', exact: true }).click()
    else await page.getByRole('group', { name: '资料所属部门', exact: true }).getByRole('button', { name: '后端', exact: true }).click()
    await expect(content).toContainText('Java 培训资料.pdf')
    expect(api.requests).toContain('GET /qt/materials/folders')
  })
}

test('上传面板先显示队列，确认后串行上传；失败继续且只重试失败文件', async ({ page, api }) => {
  await loginAs(page, 'admin')
  await page.goto('/admin/learning-materials')
  await page.getByTestId('open-material-upload').click()
  const drawer = page.locator('.material-upload-drawer')
  await expect(drawer).toBeVisible()
  await expect.poll(async () => {
    const box = await drawer.boundingBox()
    return Math.abs(box.x + box.width - page.viewportSize().width)
  }).toBeLessThan(1)
  await page.locator('.material-upload-drawer input[type=file]').setInputFiles([
    { name: 'batch-a.pdf', mimeType: 'application/pdf', buffer: Buffer.from('a') },
    { name: 'batch-b.pdf', mimeType: 'application/pdf', buffer: Buffer.from('b') },
  ])
  await expect(page.getByTestId('material-upload-item')).toHaveCount(2)
  await expect(page.getByTestId('material-upload-item').first()).toContainText('待上传')
  await expect(page.getByTestId('confirm-material-upload')).toHaveCSS('color', 'rgb(255, 255, 255)')
  expect(await page.getByTestId('material-upload-item').first().locator('.material-upload-drawer__status').evaluate(el => getComputedStyle(el, '::before').backgroundColor)).toBe('rgb(34, 180, 110)')
  await page.screenshot({ path: 'test-results/material-upload-queue.png' })
  expect(api.requests).not.toContain('POST /qt/materials')
  api.materialFailureName = 'batch-a.pdf'
  await page.getByTestId('confirm-material-upload').click()
  await expect(drawer).toContainText('成功 1 个，失败 1 个')
  expect(api.requests.filter(request => request === 'POST /qt/materials')).toHaveLength(2)
  api.materialFailureName = ''
  await page.getByRole('button', { name: '重试失败文件', exact: true }).click()
  await expect(drawer).toContainText('全部上传完成')
  expect(api.requests.filter(request => request === 'POST /qt/materials')).toHaveLength(3)
})

for (const width of [320, 390, 430, 1440]) {
  test(`资料页面 ${width}px 布局、分类和后台侧栏外观`, async ({ page, api }, testInfo) => {
    await page.setViewportSize({ width, height: 900 })
    await loginAs(page, 'admin')
    if (width === 1440) {
      await expect(page.locator('.app-sidebar')).toHaveCSS('background-color', 'rgb(255, 255, 255)')
      await expect(page.locator('.welcome-banner__mark')).toHaveCSS('opacity', '1')
    }
    await page.goto('/admin/learning-materials')
    await expect(page.getByTestId('open-material-upload')).toBeVisible()
    await expect(page.locator('.learning-materials-view')).toContainText('历史未分类资料.pdf')
    await expect(page.locator('#nprogress')).toHaveCount(0)
    await expect(page.getByTestId('open-material-upload')).toHaveCSS('background-color', 'rgb(255, 103, 0)')
    await expect(page.getByTestId('open-material-upload').locator('span').last()).toHaveCSS('color', 'rgb(255, 255, 255)')
    const downloadLink = width <= 700 ? page.locator('.learning-materials-mobile__actions .el-button--primary').first() : page.locator('.learning-materials-table .el-button--primary').first()
    await expect(downloadLink).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    const sizes = await page.locator('.app-main').evaluate(el => ({ content: el.scrollWidth, width: el.clientWidth }))
    expect(sizes.content).toBeLessThanOrEqual(sizes.width + 1)
    await page.screenshot({ path: testInfo.outputPath(`admin-materials-${width}.png`) })
    if (width <= 390) {
      await page.getByTestId('open-material-upload').click()
      const drawer = page.locator('.material-upload-drawer')
      await expect.poll(async () => Math.abs((await drawer.boundingBox()).x)).toBeLessThan(1)
      await expect(drawer).toHaveCSS('width', `${width}px`)
      await expect(page.getByTestId('confirm-material-upload')).toBeVisible()
      await page.screenshot({ path: testInfo.outputPath(`admin-upload-${width}.png`) })
      await drawer.getByRole('button', { name: '关闭', exact: true }).click()
      await expect(drawer).not.toBeVisible()
    }
    await page.goto('/member/materials')
    await page.reload()
    await expect(page.getByRole('tab', { name: '全部', exact: true })).toHaveAttribute('aria-selected', 'true')
    await expect(page.locator('.member-materials')).toContainText('历史未分类资料.pdf')
    await expect(page.locator('.member-materials').getByRole('button', { name: '搜索', exact: true })).toHaveCSS('background-color', 'rgb(255, 103, 0)')
    await expect(page.locator('#nprogress')).toHaveCount(0)
    const library = page.locator('.member-materials')
    expect(await library.evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
    await expect(page.getByRole('group', { name: '资料类型', exact: true })).toHaveCount(0)
    await page.screenshot({ path: testInfo.outputPath(`member-materials-original-${width}.png`) })
    if (width <= 700) {
      const tabs = await page.locator('.member-materials__tabs').boundingBox()
      const list = await page.locator('.member-materials__list').boundingBox()
      expect(Math.abs(tabs.x - list.x)).toBeLessThan(1)
      expect(Math.abs(tabs.width - list.width)).toBeLessThan(1)
      const buttons = await page.getByRole('tab').evaluateAll(nodes => nodes.map(el => el.getBoundingClientRect().width))
      expect(Math.max(...buttons) - Math.min(...buttons)).toBeLessThan(1)
    }
    await page.getByRole('tab', { name: '前端', exact: true }).click()
    await expect(page.getByRole('button', { name: '培训资料', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: '培训资料', exact: true })).toHaveCSS('border-radius', '9px')
    if (width <= 700) {
      const types = await page.getByRole('group', { name: '资料类型', exact: true }).boundingBox()
      const tabs = await page.locator('.member-materials__tabs').boundingBox()
      expect(Math.abs(types.x - tabs.x)).toBeLessThan(1)
      expect(Math.abs(types.width - tabs.width)).toBeLessThan(1)
      const buttons = await page.locator('.member-materials__types button').evaluateAll(nodes => nodes.map(el => ({ width: el.getBoundingClientRect().width, top: el.getBoundingClientRect().top, fits: el.scrollWidth <= el.clientWidth })))
      expect(Math.max(...buttons.map(item => item.width)) - Math.min(...buttons.map(item => item.width))).toBeLessThan(1)
      expect(new Set(buttons.map(item => item.top)).size).toBe(1)
      expect(buttons.every(item => item.fits)).toBe(true)
      const download = page.locator('.member-materials__list article').first().getByRole('button', { name: '下载', exact: true })
      const item = page.locator('.member-materials__list article').first()
      expect((await download.boundingBox()).width).toBeGreaterThanOrEqual((await item.boundingBox()).width - 4)
    }
    await page.screenshot({ path: testInfo.outputPath(`member-materials-${width}.png`) })
    expect(api.requests).toContain('GET /qt/materials')
  })
}
