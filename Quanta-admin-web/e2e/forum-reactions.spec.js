import { test, expect, loginAs } from './support/fixture.js'

test('detail reactions preserve rendered Markdown and Mermaid; private history shares state', async ({ page, api }) => {
  api.forumPost.content = '## 记录\n正文\n\n```mermaid\nflowchart LR\n A[开始] --> B[完成]\n```\n\n## 结论\n稳定阅读'
  await loginAs(page, 'member')
  await page.goto('/member/community/1')
  await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(1)
  await page.evaluate(() => {
    window.__article = document.querySelector('.community-markdown')
    window.__diagram = document.querySelector('[data-mermaid-block] svg')
  })
  const initialViews = api.requests.filter(item => item === 'GET /qt/forum/posts/1').length
  const actions = page.locator('.community-reactions.is-detail')
  await actions.locator('[data-reaction=liked]').click()
  await expect(actions.locator('[data-reaction=liked]')).toHaveAttribute('aria-pressed', 'true')
  await actions.locator('[data-reaction=favorites]').click()
  await expect(actions.locator('[data-reaction=favorites]')).toHaveAttribute('aria-pressed', 'true')
  expect(await page.evaluate(() => window.__article === document.querySelector('.community-markdown') && window.__diagram === document.querySelector('[data-mermaid-block] svg'))).toBe(true)
  expect(api.requests.filter(item => item === 'GET /qt/forum/posts/1')).toHaveLength(initialViews)
  await page.goto('/member/community?scope=favorites&page=3')
  await expect(page.locator('.community-post-card')).toHaveCount(1)
  await expect(page).not.toHaveURL(/page=3/)
  await expect(page.locator('.community-private-note')).toContainText('仅你可见')
  await page.locator('[data-reaction=favorites]').click()
  await expect(page.getByText('还没有收藏的帖子', { exact: true })).toBeVisible()
  await page.goto('/member/profile')
  await page.locator('.member-community-shortcuts').getByRole('link', { name: '我赞过的', exact: false }).click()
  await expect(page.getByRole('tab', { name: '我赞过的', exact: true })).toHaveAttribute('aria-selected', 'true')
  await page.locator('[data-reaction=liked]').click()
  await expect(page.getByText('还没有赞过的帖子', { exact: true })).toBeVisible()
})

for (const width of [320, 390, 430]) {
  test(`mobile ${width}px has separate compact actions, four tabs and personal entries`, async ({ page, api }, info) => {
    api.forumPost.title = '手机论坛互动测试'
    await page.setViewportSize({ width, height: 900 })
    await loginAs(page, 'member')
    await page.goto('/member/community')
    const actions = page.locator('.community-post-card .community-reactions')
    await actions.locator('[data-reaction=liked]').click()
    await expect(actions.locator('[data-reaction=liked]')).toHaveAttribute('aria-pressed', 'true')
    await actions.locator('[data-reaction=favorites]').click()
    const favorite = actions.locator('[data-reaction=favorites]')
    await expect(favorite).toHaveAttribute('aria-pressed', 'true')
    await expect(favorite).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
    const colors = await favorite.evaluate(el => ({ text:getComputedStyle(el).color, icon:getComputedStyle(el.querySelector('svg')).color }))
    expect(colors.icon).not.toBe(colors.text)
    for (const button of await actions.locator('button').all()) expect((await button.boundingBox()).height).toBeGreaterThanOrEqual(44)
    expect(await page.locator('.community-feed__tabs').evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path:info.outputPath(`reactions-${width}.png`), fullPage:true })
    await page.goto('/member/profile')
    await expect(page.locator('.member-community-shortcuts')).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path:info.outputPath(`profile-entries-${width}.png`), fullPage:true })
    await page.locator('.member-community-shortcuts').getByRole('link', { name:'我的收藏',exact:false }).click()
    await expect(page.getByRole('tab', { name:'我的收藏',exact:true })).toHaveAttribute('aria-selected','true')
    await expect(page.locator('.community-post-card')).toHaveCount(1)
  })
}

test('a rejected write does not paint a successful state or count', async ({ page, api }) => {
  api.forumReactionFailure = true
  await loginAs(page, 'member')
  await page.goto('/member/community')
  const like = page.locator('[data-reaction=liked]')
  await like.click()
  await expect(page.getByText('模拟互动失败', { exact:true })).toBeVisible()
  await expect(like).toHaveAttribute('aria-pressed','false')
  await expect(like).toContainText('0')
})
