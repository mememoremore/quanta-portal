import { test, expect, loginAs } from './support/fixture.js'

const output = process.env.QUANTA_VISIBILITY_SCREENSHOTS

for (const width of [1440, 390]) {
  test(`manager visibility is correctly laid out and published at ${width}px`, async ({ page, api }, testInfo) => {
    const screenshotPath = name => output ? `${output}/${name}` : testInfo.outputPath(name)
    await page.setViewportSize({ width, height: 1024 })
    await loginAs(page, 'viewer')
    await page.goto('/member/community/new')
    await page.getByPlaceholder('给讨论起个清晰的标题').fill('Challenge 技术分享：登录流程与实现')
    const category = page.getByRole('combobox', { name: '帖子分类' })
    const visibility = page.getByRole('combobox', { name: '帖子可见范围' })
    await page.locator('.community-field').filter({ has: category }).locator('.el-select__wrapper').click()
    await page.getByRole('option', { name: '技术分享', exact: true }).click()
    await page.locator('.community-field').filter({ has: visibility }).locator('.el-select__wrapper').click()
    await page.getByRole('option', { name: '经理及以上', exact: true }).click()
    await page.getByRole('textbox', { name: '帖子正文' }).fill('## 登录流程说明\n\n梳理 Challenge 的登录流程，记录前后端协作中的注意事项。\n\n### 授权与回跳\n\n- 校验回跳地址与请求状态\n- 服务端确认用户身份\n- 成功后进入 Challenge 首页')
    const first = await category.boundingBox(), second = await visibility.boundingBox()
    if (width > 600) expect(Math.abs(first.y - second.y)).toBeLessThan(3)
    else expect(second.y).toBeGreaterThan(first.y + first.height)
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width)
    await page.getByRole('button', { name: '保存草稿', exact: true }).click()
    await expect(page.getByRole('status')).toContainText('草稿已保存')
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(0)
    await page.screenshot({ path: screenshotPath(`forum-visibility-editor-${width}.png`), fullPage: true, animations: 'disabled' })
    if (width === 390) {
      await page.setViewportSize({ width: 320, height: 900 })
      expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(320)
      await page.setViewportSize({ width, height: 1024 })
    }
    await page.getByRole('button', { name: '发布帖子', exact: true }).click()
    await expect(page).toHaveURL(/\/member\/community\/2$/)
    await expect(page.locator('.community-visibility-badge')).toHaveText('经理及以上可见')
    await expect(page.locator('.el-message')).toHaveCount(0)
    await expect(page.locator('#nprogress')).toHaveCount(0)
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
    await page.screenshot({ path: screenshotPath(`forum-visibility-detail-${width}.png`), fullPage: true, animations: 'disabled' })
    expect(api.forumPost.visibility).toBe('MANAGERS')
    await page.goto('/member/community')
    await expect(page.locator('.community-post-card .community-visibility-badge')).toBeVisible()
    await expect(page.locator('#nprogress')).toHaveCount(0)
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
    await page.screenshot({ path: screenshotPath(`forum-visibility-feed-${width}.png`), fullPage: true, animations: 'disabled' })
  })
}

test('ordinary members have no manager selector and cannot open restricted post links', async ({ page, api }) => {
  api.forumPost.visibility = 'MANAGERS'
  await loginAs(page, 'member')
  await page.goto('/member/community/new')
  await expect(page.locator('.community-visibility-fixed')).toHaveText('全部塔员')
  await expect(page.getByRole('combobox', { name: '帖子可见范围' })).toHaveCount(0)
  await page.goto('/member/community')
  await expect(page.getByText(api.forumPost.title, { exact: true })).toHaveCount(0)
  await page.goto('/member/community/1')
  await expect(page.getByRole('alert')).toContainText('帖子不存在或已删除')
  await expect(page.locator('.community-detail__post')).toHaveCount(0)
})

test('revoking manager capability during editing rejects saving without losing the draft', async ({ page, api }) => {
  await loginAs(page, 'viewer')
  await page.goto('/member/community/new')
  await page.getByPlaceholder('给讨论起个清晰的标题').fill('待发布的管理层文章')
  await page.getByRole('textbox', { name: '帖子正文' }).fill('保留我的正文')
  await page.locator('.community-field').filter({ has: page.getByRole('combobox', { name: '帖子可见范围' }) }).locator('.el-select__wrapper').click()
  await page.getByRole('option', { name: '经理及以上', exact: true }).click()
  api.forumManagerDenied = true
  await page.getByRole('button', { name: '发布帖子', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('仅经理及以上')
  await expect(page.getByRole('textbox', { name: '帖子正文' })).toHaveValue('保留我的正文')
  await page.reload()
  await expect(page.locator('.community-category-select').last()).toContainText('经理及以上')
  await page.getByRole('button', { name: '发布帖子', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('不会自动改为公开')
})

test('protected pictures use an authenticated request and open as a local blob', async ({ page, api }) => {
  api.forumPost.content = '![流程图](/qt/forum/images/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa)'
  api.forumPost.visibility = 'MANAGERS'
  await loginAs(page, 'viewer')
  await page.goto('/member/community/1')
  const image = page.locator('.community-markdown img')
  await expect(image).toHaveAttribute('src', /^blob:/)
  await expect.poll(() => image.evaluate(node => node.complete && node.naturalWidth > 0)).toBe(true)
  expect(api.requests).toContain('GET /qt/forum/images/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')
})

test('making an existing restricted post public requires explicit confirmation', async ({ page, api }) => {
  api.forumPost.visibility = 'MANAGERS'
  api.forumPost.authorId = 104
  await loginAs(page, 'viewer')
  await page.goto('/member/community/1/edit')
  await page.locator('.community-field').filter({ has: page.getByRole('combobox', { name: '帖子可见范围' }) }).locator('.el-select__wrapper').click()
  await page.getByRole('option', { name: '全部塔员', exact: true }).click()
  await page.getByRole('button', { name: '保存修改', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '确认扩大可见范围' })
  await expect(dialog).toContainText('已被查看、保存或转发的内容无法收回')
  await dialog.getByRole('button', { name: '保留原范围' }).click()
  expect(api.forumPost.visibility).toBe('MANAGERS')
  expect(api.requests).not.toContain('PUT /qt/forum/posts/1')
  await page.getByRole('button', { name: '保存修改', exact: true }).click()
  await dialog.getByRole('button', { name: '改为全部塔员' }).click()
  await expect(page).toHaveURL(/\/member\/community\/1$/)
  expect(api.forumPost.visibility).toBe('MEMBERS')
  await expect(page.locator('.community-visibility-badge')).toHaveCount(0)
})
