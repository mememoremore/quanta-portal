import { test, expect, loginAs } from './support/fixture.js'

const markdown = '# 限流算法完整笔记\n\n限流用于保护系统在高并发场景下稳定运行。\n\n## 令牌桶算法\n\n请求拿到令牌后继续执行，没有令牌时等待或拒绝。\n\n```js\nif (bucket.tryAcquire()) {\n  handleRequest();\n}\n```\n\n## 适用场景\n\n- 接口访问频率限制\n- 突发流量保护\n\n![禁止自动加载](https://tracking.invalid/pixel)\n'
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/aZ8AAAAASUVORK5CYII=', 'base64')

function makePdf() {
  const stream = text => `<< /Length ${text.length} >>\nstream\n${text}\nendstream`
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 400 500] /Resources << /Font << /F1 5 0 R >> >> /Contents 6 0 R >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 400 500] /Resources << /Font << /F1 5 0 R >> >> /Contents 7 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    stream('BT /F1 24 Tf 40 430 Td (Quanta Learning - page 1) Tj ET'),
    stream('BT /F1 24 Tf 40 430 Td (Quanta Learning - page 2) Tj ET'),
  ]
  let pdf = '%PDF-1.4\n'
  const offsets = [0]
  objects.forEach((object, index) => { offsets.push(Buffer.byteLength(pdf)); pdf += `${index + 1} 0 obj\n${object}\nendobj\n` })
  const start = Buffer.byteLength(pdf)
  pdf += `xref\n0 8\n0000000000 65535 f \n${offsets.slice(1).map(offset => `${String(offset).padStart(10, '0')} 00000 n \n`).join('')}trailer\n<< /Size 8 /Root 1 0 R >>\nstartxref\n${start}\n%%EOF`
  return Buffer.from(pdf)
}

async function prepare(page, api) {
  api.materials = [
    { materialId: 11, fileName: '限流算法完整笔记.md' },
    { materialId: 12, fileName: 'Quanta入门教程.pdf' },
    { materialId: 13, fileName: 'quanta第一课.pptx' },
    { materialId: 14, fileName: '示例图片.png' },
    { materialId: 15, fileName: '示例代码.txt' },
  ].map(row => ({ ...row, fileSize: 1024, category: 'FRONTEND', folderId: 1, uploaderName: '测试上传者', uploadTime: '2026-10-19 10:00:00' }))
  const downloads = [], failures = { enabled: false }
  await page.context().route('**/qt/materials/*/download', async route => {
    const request = route.request()
    const headers = {
      'access-control-allow-origin': 'http://127.0.0.1:5177',
      'access-control-allow-credentials': 'true',
      'access-control-allow-headers': 'authorization, content-type, x-trace-id',
      'access-control-allow-methods': 'GET, OPTIONS',
    }
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
    downloads.push(request.headers().authorization)
    if (failures.enabled) return route.fulfill({ status: 200, contentType: 'application/json', headers, body: JSON.stringify({ code: 500, msg: '资料不存在或已删除' }) })
    const id = Number(new URL(request.url()).pathname.split('/')[3])
    const body = id === 12 ? makePdf() : id === 14 ? png : id === 15 ? '<script>alert("not executed")</script>\nconst count = 1' : markdown
    return route.fulfill({ status: 200, contentType: 'application/octet-stream', headers: { ...headers, 'content-disposition': 'attachment; filename="material"' }, body })
  })
  return { downloads, failures }
}

for (const width of [390, 1440]) {
  test(`塔员资料 ${width}px 独立按钮、新标签页预览和下载`, async ({ page, api }, testInfo) => {
    await page.setViewportSize({ width, height: 1000 })
    const { downloads } = await prepare(page, api)
    await loginAs(page, 'member')
    await page.goto('/member/materials')
    await page.getByRole('tab', { name: '前端', exact: true }).click()
    const article = page.locator('.member-materials__list article').first()
    const preview = article.getByRole('link', { name: '预览', exact: true })
    const download = article.getByRole('button', { name: '下载', exact: true })
    await expect(preview).toBeVisible()
    await expect(preview).toHaveCSS('justify-content', 'center')
    await expect(preview.locator('svg,i')).toHaveCount(0)
    const left = await preview.boundingBox(), right = await download.boundingBox()
    expect(Math.abs(right.x - left.x - left.width - 10)).toBeLessThan(1)
    expect(left.y).toBe(right.y)
    if (width === 390) expect(Math.abs(left.width - right.width)).toBeLessThan(1)
    await page.screenshot({ path: testInfo.outputPath(`materials-${width}.png`), fullPage: true })
    await article.screenshot({ path: testInfo.outputPath(`actions-${width}.png`) })
    const popup = page.waitForEvent('popup')
    await preview.click()
    const reader = await popup
    await reader.setViewportSize({ width, height: 1000 })
    await expect(reader).toHaveURL(/\/member\/materials\/11\/preview$/)
    await expect(reader.locator('.material-preview__markdown h1')).toHaveText('限流算法完整笔记')
    await expect(reader.locator('.material-preview img,.material-preview iframe')).toHaveCount(0)
    expect(await reader.evaluate(() => window.opener)).toBeNull()
    expect(downloads[0]).toBe('Bearer e2e-token-member_demo')
    await reader.screenshot({ path: testInfo.outputPath(`preview-${width}.png`), animations: 'disabled' })
    await reader.reload()
    await expect(reader.locator('.material-preview__markdown h1')).toHaveText('限流算法完整笔记')
    await reader.close()
    await expect(page).toHaveURL(/\/member\/materials$/)
    await expect(page.getByRole('tab', { name: '前端', exact: true })).toHaveAttribute('aria-selected', 'true')
    const pendingDownload = page.waitForEvent('download')
    await download.click()
    expect((await pendingDownload).suggestedFilename()).toBe('限流算法完整笔记.md')
    if (width === 390) {
      await page.setViewportSize({ width: 320, height: 900 })
      expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(320)
      const smallLeft = await preview.boundingBox(), smallRight = await download.boundingBox()
      expect(Math.abs(smallLeft.width - smallRight.width)).toBeLessThan(1)
      expect(smallRight.x - smallLeft.x - smallLeft.width).toBeCloseTo(10, 0)
    }
  })
}

test('PDF 本地 worker 实际渲染和翻页，图片、代码和 Office 降级正常', async ({ page, api }) => {
  await page.setViewportSize({ width: 390, height: 900 })
  await prepare(page, api)
  await loginAs(page, 'member')
  await page.goto('/member/materials')
  await page.goto('/member/materials/12/preview')
  const dialog = page.locator('.material-preview')
  await expect(dialog.getByText('1 / 2 页')).toBeVisible({ timeout: 20000 })
  await expect(dialog.getByText('正在渲染 PDF…')).toHaveCount(0)
  const canvas = dialog.locator('canvas')
  await expect(canvas).toBeVisible()
  expect(await canvas.evaluate(element => element.width)).toBeGreaterThan(100)
  expect(await canvas.evaluate(element => Array.from(element.getContext('2d').getImageData(0, 0, element.width, element.height).data).some((value, index) => index % 4 !== 3 && value < 200))).toBe(true)
  await dialog.getByRole('button', { name: '下一页' }).click()
  await expect(dialog.getByText('2 / 2 页')).toBeVisible()
  await expect(dialog.getByText('正在渲染 PDF…')).toHaveCount(0)
  await page.goto('/member/materials/14/preview')
  await expect(dialog.locator('img')).toBeVisible()
  await page.goto('/member/materials/15/preview')
  await expect(dialog.locator('pre')).toContainText('<script>alert("not executed")</script>')
  await expect(dialog.locator('script')).toHaveCount(0)
  await page.goto('/member/materials/13/preview')
  await expect(dialog).toContainText('暂不支持在线预览')
  await expect(dialog.locator('[data-preview-download]')).toBeEnabled()
})

test('资料读取失败可重试，权限校验错误不伪装为文本正文', async ({ page, api }) => {
  const { failures } = await prepare(page, api)
  failures.enabled = true
  await loginAs(page, 'member')
  await page.goto('/member/materials/11/preview')
  await expect(page.locator('.material-preview').getByRole('alert')).toContainText('资料不存在或已删除')
  failures.enabled = false
  await page.getByRole('button', { name: '重新加载' }).click()
  await expect(page.locator('.material-preview__markdown h1')).toHaveText('限流算法完整笔记')
})

test('独立预览地址仍要求登录，资料被删除时不会展示空阅读器', async ({ page, api }) => {
  await page.goto('/member/materials/999/preview')
  await expect(page).toHaveURL(/\/login\/member\?/)
  expect(new URL(page.url()).searchParams.get('redirect')).toBe('/member/materials/999/preview')
  expect(api.requests).not.toContain('GET /qt/materials/999')
  await loginAs(page, 'member')
  await page.goto('/member/materials/999/preview')
  await expect(page.getByRole('alert')).toContainText('资料不存在或已删除')
  await expect(page.locator('.material-preview')).toHaveCount(0)
})

test('手机编辑工具栏保持单行，桌面文案和插入行为保持不变', async ({ page, api }, testInfo) => {
  expect(api).toBeTruthy()
  await loginAs(page, 'member')
  await page.goto('/member/community/new')
  const toolbar = page.locator('.community-editor__toolbar')
  await expect(toolbar).toBeVisible()
  for (const width of [320, 390, 700, 1440]) {
    await page.setViewportSize({ width, height: 1100 })
    const positions = await toolbar.locator('button').evaluateAll(elements => elements.map(element => element.getBoundingClientRect().top))
    expect(new Set(positions).size).toBe(1)
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width)
    if (width <= 700) {
      await expect(toolbar.locator('.community-editor__mobile-tool').last()).toBeVisible()
      await expect(toolbar.locator('.community-editor__desktop-tool').last()).toBeHidden()
      expect(await toolbar.locator('button').first().evaluate(element => element.getBoundingClientRect().height)).toBeGreaterThanOrEqual(44)
    } else await expect(toolbar.locator('.community-editor__desktop-tool').last()).toHaveText('</> 代码块')
    if (width === 390) {
      await page.screenshot({ path: testInfo.outputPath('editor-mobile.png'), fullPage: true })
      await toolbar.screenshot({ path: testInfo.outputPath('editor-toolbar-mobile.png') })
    }
  }
  await page.setViewportSize({ width: 390, height: 900 })
  await toolbar.getByRole('button', { name: '插入代码块', exact: true }).click()
  await expect(page.getByRole('textbox', { name: '帖子正文' })).toHaveValue(/```js\n在这里写代码\n```/)
})
