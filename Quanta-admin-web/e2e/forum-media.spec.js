import { test, expect, loginAs } from './support/fixture.js'

const diagram = '```mermaid\nflowchart LR\n A[打开图片] --> B[缩放查看] --> C[关闭返回]\n```'
async function prepare(page, api) {
  // A local generated raster avoids external requests in privacy-sensitive post content.
  const image = await page.evaluate(() => {
    const canvas = document.createElement('canvas')
    canvas.width = 1200; canvas.height = 800
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#fff3eb'; ctx.fillRect(0, 0, 1200, 800)
    ctx.fillStyle = '#ff6b00'; ctx.font = '80px sans-serif'; ctx.fillText('Quanta media preview', 100, 400)
    return canvas.toDataURL('image/png')
  })
  api.forumPost.content = `![查看测试图](${image})\n\n${diagram}`
  await loginAs(page, 'member')
  await page.goto('/member/community/1')
  await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(1)
}

test('desktop image wheel zoom, drag, click close, focus and diagram cloning', async ({ page, api }, info) => {
  await prepare(page, api)
  const image = page.getByRole('button', { name: '全屏查看查看测试图', exact: true })
  await image.click()
  const viewer = page.getByRole('dialog', { name: '全屏看图' })
  const content = viewer.locator('.community-media-viewer__content')
  const stage = viewer.locator('.community-media-viewer__stage')
  await expect(viewer.getByRole('status')).toBeHidden()
  await expect(page.locator('body')).toHaveCSS('overflow', 'hidden')
  const fitted = await content.getAttribute('style')
  await stage.hover(); await page.mouse.wheel(0, -700)
  await expect.poll(() => content.getAttribute('style')).not.toBe(fitted)
  const box = await stage.boundingBox()
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.mouse.down(); await page.mouse.move(box.x + box.width / 2 + 100, box.y + box.height / 2 + 60, { steps: 8 }); await page.mouse.up()
  await expect(viewer).toBeVisible()
  await page.screenshot({ path: info.outputPath('desktop-media.png') })
  await stage.click()
  await expect(viewer).not.toBeVisible()
  await expect(image).toBeFocused()
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden')
  const source = page.locator('[data-mermaid-block] svg')
  const id = await source.getAttribute('aria-labelledby')
  await page.locator('.community-mermaid__viewport').click()
  await expect(viewer.locator('svg')).toBeVisible()
  if (id) expect(await viewer.locator('svg').getAttribute('aria-labelledby')).not.toBe(id)
  await page.keyboard.press('Escape')
  await expect(viewer).not.toBeVisible()
})

test.describe('touchscreen media viewer', () => {
test.use({ hasTouch: true })
test('mobile actual touch pinch and pan do not close; subsequent tap closes', async ({ page, api }, info) => {
  await page.setViewportSize({ width: 390, height: 844 })
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 2 })
  await prepare(page, api)
  await page.getByRole('button', { name: '全屏查看查看测试图', exact: true }).tap()
  const viewer = page.getByRole('dialog', { name: '全屏看图' })
  const stage = viewer.locator('.community-media-viewer__stage')
  await expect(viewer.getByRole('status')).toBeHidden()
  const initial = await viewer.locator('[data-media-percent]').textContent()
  const rect = await stage.boundingBox(), x = rect.width / 2, y = rect.y + rect.height / 2
  const send = (type, points) => cdp.send('Input.dispatchTouchEvent', { type, touchPoints: points.map(([id, px, py]) => ({ id, x: px, y: py })) })
  await send('touchStart', [[1, x - 30, y], [2, x + 30, y]])
  for (let i = 1; i <= 5; i++) await send('touchMove', [[1, x - 30 - i * 15, y], [2, x + 30 + i * 15, y]])
  await send('touchEnd', [])
  await expect(viewer).toBeVisible()
  await expect(viewer.locator('[data-media-percent]')).not.toHaveText(initial)
  await send('touchStart', [[1, x, y]])
  await send('touchMove', [[1, x + 60, y + 40]])
  await send('touchEnd', [])
  await expect(viewer).toBeVisible()
  await page.screenshot({ path: info.outputPath('mobile-media.png') })
  await stage.tap()
  await expect(viewer).not.toBeVisible()
  await cdp.detach()
})
})
