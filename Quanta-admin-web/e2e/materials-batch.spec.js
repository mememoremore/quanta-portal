import { test, expect, loginAs } from './support/fixture.js'

function list(page, width) { return page.locator(width <= 700 ? '.learning-materials-mobile' : '.learning-materials-table') }
function types(page) { return page.getByRole('group', { name: '资料类型', exact: true }) }
async function openCeo(page, width = 1440) {
  await page.setViewportSize({ width, height: 1050 })
  await loginAs(page, 'ceo')
  await page.goto('/admin/learning-materials')
  await expect(page.locator('.learning-materials-view')).toContainText('历史未分类资料.pdf')
}

for (const width of [320, 390, 430, 1440]) {
  test(`CEO资料列表和上传侧栏 ${width}px：左侧全部、分类toggle、选择清空与布局`, async ({ page, api }, testInfo) => {
    api.materials[1].fileName = '浏览器技术分享.md'
    api.materials[2].fileName = '门户项目实践.docx'
    await openCeo(page, width)
    const buttons = types(page).getByRole('button')
    await expect(buttons).toHaveText(['全部资料', '培训资料', '技术分享', '项目实践'])
    await expect(buttons.first()).toHaveAttribute('aria-pressed', 'true')
    await buttons.nth(1).click()
    await expect(list(page, width)).not.toContainText('浏览器技术分享.md')
    await expect(buttons.nth(1)).toHaveAttribute('aria-pressed', 'true')
    await buttons.nth(1).click()
    await expect(list(page, width)).toContainText('历史未分类资料.pdf')
    await expect(buttons.first()).toHaveAttribute('aria-pressed', 'true')
    await list(page, width).locator('.el-checkbox').first().click()
    await expect(page.getByLabel('批量操作')).toContainText('已选 1 项')
    await page.getByRole('group', { name: '资料所属部门' }).getByRole('button', { name: '后端', exact: true }).click()
    await expect(page.getByLabel('批量操作')).toContainText('已选 0 项')
    await page.getByRole('group', { name: '资料所属部门' }).getByRole('button', { name: '前端', exact: true }).click()
    await expect(list(page, width)).toContainText('Vue 入门培训.pdf')
    await page.locator('.learning-materials-batch .el-checkbox').click()
    await expect(page.getByLabel('批量操作')).toContainText('已选 4 项')
    const firstItem = width <= 700 ? list(page, width).locator('article').first() : list(page, width).locator('.el-table__row').first()
    if (width <= 700) {
      const item = await firstItem.boundingBox()
      const info = await firstItem.locator('.learning-materials-table__name').boundingBox()
      const actions = await firstItem.locator('.learning-materials-mobile__actions').boundingBox()
      expect(item.height).toBeLessThanOrEqual(110)
      expect(actions.y + actions.height / 2).toBeGreaterThanOrEqual(info.y)
      expect(actions.y + actions.height / 2).toBeLessThanOrEqual(info.y + info.height)
    }
    await expect(firstItem.locator('.material-file-icon svg')).toBeVisible()
    await expect(firstItem.getByRole('checkbox')).toBeChecked()
    await expect(firstItem.locator('.el-checkbox__inner')).toHaveCSS('background-color', 'rgb(255, 103, 0)')
    await expect(list(page, width).locator('.material-file-icon[data-kind="markdown"] svg')).toBeVisible()
    await expect(list(page, width).locator('.material-file-icon[data-kind="word"] svg')).toBeVisible()
    await expect(page.locator('.app-main')).toBeVisible()
    expect(await page.locator('.app-main').evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
    await page.screenshot({ path: testInfo.outputPath(`admin-batch-${width}.png`), fullPage: true, animations: 'disabled' })
    if (width <= 700) {
      api.materials[0].fileName = '一个很长的文件名📚-' + 'long-unbroken-filename'.repeat(5) + '.pdf'
      await page.getByRole('group', { name: '资料所属部门' }).getByRole('button', { name: '前端', exact: true }).click()
      await expect(firstItem).toContainText('一个很长的文件名📚')
      expect(await firstItem.evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
      const info = await firstItem.locator('.learning-materials-table__name').boundingBox()
      const actions = await firstItem.locator('.learning-materials-mobile__actions').boundingBox()
      expect(actions.x).toBeGreaterThanOrEqual(info.x + info.width - 1)
      await expect(firstItem.getByRole('button', { name: '下载', exact: true })).toBeVisible()
    }
    if (width > 700) await page.getByRole('button', { name: '取消选择', exact: true }).click()
    await buttons.nth(1).click()
    await page.getByTestId('open-material-upload').click()
    const drawer = page.locator('.material-upload-drawer')
    await expect(drawer).toBeVisible()
    const uploadTypes = drawer.getByRole('group', { name: '上传资料类型' })
    await expect(uploadTypes.getByRole('button', { name: '培训资料', exact: true })).toHaveAttribute('aria-pressed', 'true')
    await uploadTypes.getByRole('button', { name: '培训资料', exact: true }).click()
    await expect(uploadTypes.getByRole('button', { name: '培训资料', exact: true })).toHaveAttribute('aria-pressed', 'true')
    await drawer.locator('input[type=file]').setInputFiles([
      { name: '前端入门培训.pdf', mimeType: 'application/pdf', buffer: Buffer.from('preview') },
      { name: 'Vue组件开发规范.md', mimeType: 'text/markdown', buffer: Buffer.from('# Vue') },
      { name: '新成员学习指南.docx', mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', buffer: Buffer.from('preview-docx') },
    ])
    await expect(drawer.getByTestId('material-upload-item')).toHaveCount(3)
    await expect(drawer.getByTestId('material-upload-item').first().locator('.material-file-icon[data-kind="pdf"] svg')).toBeVisible()
    await expect(drawer.getByTestId('material-upload-item').nth(1).locator('.material-file-icon[data-kind="markdown"] svg')).toBeVisible()
    await expect(drawer.getByTestId('material-upload-item').nth(2).locator('.material-file-icon[data-kind="word"] svg')).toBeVisible()
    await expect(drawer).toContainText('已沿用当前页面的部门与类型')
    expect(api.requests).not.toContain('POST /qt/materials')
    expect(api.requests).not.toContain('POST /qt/materials/folders')
    const box = await drawer.boundingBox()
    expect(Math.abs(box.x + box.width - width)).toBeLessThan(1)
    expect(Math.round(box.width)).toBe(width <= 700 ? width : 440)
    expect(await drawer.evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
    await expect(drawer.getByTestId('confirm-material-upload')).toBeInViewport()
    await page.screenshot({ path: testInfo.outputPath(`admin-upload-${width}.png`), fullPage: true, animations: 'disabled' })
  })
}

for (const width of [390, 1440]) {
  test(`CEO批删 ${width}px：取消不请求、显示文本文件名、防误操作、成功刷新`, async ({ page, api }, testInfo) => {
    api.materials[0].fileName = '<img src=x onerror=alert(1)>.pdf'
    await openCeo(page, width)
    const checkboxes = list(page, width).locator('.el-checkbox')
    await checkboxes.nth(0).click()
    await checkboxes.nth(1).click()
    const remove = page.getByTestId('material-batch-delete')
    await remove.click()
    const dialog = page.getByRole('dialog', { name: '批量删除资料', exact: true })
    await expect(dialog).toContainText('以下 2 份资料')
    await expect(dialog).toContainText('<img src=x onerror=alert(1)>.pdf')
    await expect(dialog.locator('img')).toHaveCount(0)
    await expect(types(page).getByRole('button').first()).toBeDisabled()
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    expect(api.materialBatchRequests || []).toHaveLength(0)
    await expect(page.getByLabel('批量操作')).toContainText('已选 2 项')
    await remove.click()
    await page.screenshot({ path: testInfo.outputPath(`admin-batch-confirm-${width}.png`), fullPage: true })
    await dialog.getByRole('button', { name: '确认删除 2 份', exact: true }).click()
    await expect(page.getByLabel('批量操作')).toContainText('已选 0 项')
    await expect(list(page, width)).not.toContainText('<img src=x onerror=alert(1)>.pdf')
    await expect(list(page, width)).not.toContainText('浏览器技术分享.pdf')
    expect(api.materialBatchRequests).toEqual([{ materialIds: [1, 2] }])
    expect(api.requests.some(value => value.startsWith('DELETE /qt/materials/'))).toBe(false)
  })
}

for (const mode of ['missing', 'forbidden', 'conflict', 'unconfirmed']) {
  test(`批删${mode}如实报错，禁止单删降级`, async ({ page, api }) => {
    api.materialBatchMode = mode
    await openCeo(page)
    await list(page, 1440).locator('.el-checkbox').first().click()
    await page.getByTestId('material-batch-delete').click()
    await page.getByRole('dialog', { name: '批量删除资料', exact: true }).getByRole('button', { name: '确认删除 1 份' }).click()
    if (mode === 'unconfirmed') {
      await expect(page.locator('.el-message--error')).toContainText('删除结果未确认')
      await expect(page.getByLabel('批量操作')).toContainText('已选 0 项')
      await expect(list(page, 1440)).not.toContainText('Vue 入门培训.pdf')
    } else {
      await expect(page.getByLabel('批量操作')).toContainText('已选 1 项')
      await expect(list(page, 1440)).toContainText('Vue 入门培训.pdf')
      await expect(page.locator('.el-message--error')).toContainText(mode === 'missing' ? '接口尚未接通' : mode === 'forbidden' ? '仅管理层或经理' : '整个批次未删除')
    }
    expect(api.requests.some(value => value.startsWith('DELETE /qt/materials/'))).toBe(false)
    await expect(page.locator('.el-message--success')).toHaveCount(0)
  })
}

test('删空最后一页，服务器刷新后退回有效页', async ({ page, api }) => {
  api.materials = Array.from({ length: 21 }, (_, index) => ({ ...api.materials[0], materialId: index + 1, fileName: `分页资料${index + 1}.pdf` }))
  await page.setViewportSize({ width: 1440, height: 1050 })
  await loginAs(page, 'ceo')
  await page.goto('/admin/learning-materials')
  await page.locator('.el-pagination .number').filter({ hasText: /^2$/ }).click()
  await expect(list(page, 1440)).toContainText('分页资料21.pdf')
  await list(page, 1440).locator('.el-checkbox').first().click()
  await page.getByTestId('material-batch-delete').click()
  await page.getByRole('dialog', { name: '批量删除资料', exact: true }).getByRole('button', { name: '确认删除 1 份' }).click()
  await expect(list(page, 1440)).toContainText('分页资料1.pdf')
  await expect(list(page, 1440)).not.toContainText('分页资料21.pdf')
  await expect(page.locator('.learning-list-card__footer')).toContainText('共 20 份资料')
  await expect(page.locator('.el-pagination .number.is-active')).toHaveText('1')
})

test('非CEO手机资料条目紧凑排列且不出现勾选和批删', async ({ page, api }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 1050 })
  await loginAs(page, 'admin')
  await page.goto('/admin/learning-materials')
  const item = list(page, 390).locator('article').first()
  await expect(item).toContainText('Vue 入门培训.pdf')
  await expect(item.getByRole('checkbox')).toHaveCount(0)
  await expect(page.getByTestId('material-batch-delete')).toHaveCount(0)
  expect((await item.boundingBox()).height).toBeLessThanOrEqual(110)
  const info = await item.locator('.learning-materials-table__name').boundingBox()
  const actions = await item.locator('.learning-materials-mobile__actions').boundingBox()
  expect(actions.y + actions.height / 2).toBeGreaterThanOrEqual(info.y)
  expect(actions.y + actions.height / 2).toBeLessThanOrEqual(info.y + info.height)
  expect(await item.evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
  expect(api.materialBatchRequests || []).toHaveLength(0)
  await page.screenshot({ path: testInfo.outputPath('admin-materials-nonceo-390.png'), fullPage: true })
})

for (const audience of ['mgmt', 'manager']) {
  test(`${audience} 可以批量删除当前页资料`, async ({ page, api }) => {
    await page.setViewportSize({ width: 1440, height: 1050 })
    await loginAs(page, audience)
    await page.goto('/admin/learning-materials')
    await expect(page.getByTestId('material-batch-delete')).toBeVisible()
    await list(page, 1440).locator('.el-checkbox').first().click()
    await page.getByTestId('material-batch-delete').click()
    await page.getByRole('dialog', { name: '批量删除资料', exact: true }).getByRole('button', { name: '确认删除 1 份' }).click()
    await expect(page.getByLabel('批量操作')).toContainText('已选 0 项')
    expect(api.materialBatchRequests).toEqual([{ materialIds: [1] }])
  })
}

test('admin不自动获得CEO批删权限，塔员端保持现有分类行为', async ({ page, api }) => {
  await loginAs(page, 'admin')
  await page.goto('/admin/learning-materials')
  await expect(page.locator('.learning-materials-view')).toContainText('历史未分类资料.pdf')
  await expect(page.getByTestId('material-batch-delete')).toHaveCount(0)
  await expect(list(page, 1440).getByRole('checkbox')).toHaveCount(0)
  await page.goto('/member/materials')
  await page.getByRole('tab', { name: '前端', exact: true }).click()
  const training = page.getByRole('group', { name: '资料类型', exact: true }).getByRole('button', { name: '培训资料', exact: true })
  await training.click()
  await expect(training).toHaveAttribute('aria-pressed', 'true')
  await training.click()
  await expect(training).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByTestId('material-batch-delete')).toHaveCount(0)
  expect(api.materialBatchRequests || []).toHaveLength(0)
})
