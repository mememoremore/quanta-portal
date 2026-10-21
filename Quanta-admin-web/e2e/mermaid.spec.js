import { test, expect, loginAs } from './support/fixture.js'

const flow = 'flowchart LR\n  A[编写帖子] --> B{检查内容}\n  B -->|通过| C[发布文章]\n  B -->|修改| A\n'
const sequence = 'sequenceDiagram\n  participant M as 塔员\n  participant Q as Quanta\n  M->>Q: 提交帖子\n  Q-->>M: 发布成功\n'
const fence = source => '```mermaid\n' + source + '```\n'
const body = '## 发布流程\n\n' + fence(flow) + '\n## 请求时序\n\n' + fence(sequence)

for (const width of [390, 1440]) {
  test(`Mermaid ${width}px 正文输入不重绘，修改单图防抖且保留旧图`, async ({ page, api }) => {
    await page.setViewportSize({ width, height: 1000 })
    await page.addInitScript(() => {
      window.diagramJobs = []
      window.diagramWorkerCount = 0
      const NativeWorker = window.Worker
      window.Worker = class extends NativeWorker {
        constructor(...args) {
          super(...args)
          window.diagramWorkerCount += 1
          const send = this.postMessage.bind(this)
          this.postMessage = (...messages) => {
            if (typeof messages[0]?.source === 'string') window.diagramJobs.push(messages[0].source)
            return send(...messages)
          }
        }
      }
    })
    await loginAs(page, 'member')
    await page.goto('/member/community/new')
    const editor = page.getByRole('textbox', { name: '帖子正文' })
    await editor.fill(body + '\n\n普通正文。')
    if (width === 390) await page.getByRole('tab', { name: '预览', exact: true }).click()
    await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2, { timeout: 15000 })
    await page.evaluate(() => {
      window.originalCharts = [...document.querySelectorAll('[data-mermaid-block]')]
      window.originalSVGs = window.originalCharts.map(chart => chart.querySelector('svg'))
      window.initialJobs = window.diagramJobs.length
      window.initialWorkers = window.diagramWorkerCount
    })
    if (width === 390) await page.getByRole('tab', { name: '编辑', exact: true }).click()
    await editor.press('End')
    await editor.pressSequentially('继续输入普通正文，不修改图表。', { delay: 40 })
    await expect.poll(() => page.evaluate(() => document.querySelector('.community-markdown').textContent.includes('继续输入普通正文，不修改图表。'))).toBe(true)
    // Wait beyond both debounce windows, without relying on a screenshot to
    // infer whether the actual Worker received unnecessary redraw requests.
    await expect.poll(async () => {
      await new Promise(resolve => setTimeout(resolve, 400))
      return page.evaluate(() => ({
        sameNodes: window.originalCharts.every((chart, index) => chart.isConnected && chart.querySelector('svg') === window.originalSVGs[index]),
        extraJobs: window.diagramJobs.length - window.initialJobs,
        extraWorkers: window.diagramWorkerCount - window.initialWorkers,
      }))
    }).toEqual({ sameNodes: true, extraJobs: 0, extraWorkers: 0 })

    const currentBody = await editor.inputValue()
    await editor.fill(currentBody.replace('编写帖子', '编写新帖'))
    await editor.fill(currentBody.replace('编写帖子', '编写新帖子'))
    expect(await page.evaluate(() => window.originalCharts[0].querySelector('svg') === window.originalSVGs[0])).toBe(true)
    await expect.poll(() => page.evaluate(() => window.diagramJobs.length - window.initialJobs)).toBe(1)
    await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2)
    expect(await page.evaluate(() => window.originalCharts[0].querySelector('svg') !== window.originalSVGs[0] && window.originalCharts[1].querySelector('svg') === window.originalSVGs[1])).toBe(true)
    if (width === 390) await page.getByRole('tab', { name: '预览', exact: true }).click()
    const first = page.locator('[data-mermaid-block]').first()
    await expect(first.locator('svg')).toContainText('编写新帖子')
    if (width === 390) await page.getByRole('tab', { name: '编辑', exact: true }).click()
    await page.evaluate(() => { window.lastGoodSVG = window.originalCharts[0].querySelector('svg') })
    const editedBody = currentBody.replace('编写帖子', '编写新帖子')
    await editor.fill(editedBody.replace(flow.replace('编写帖子', '编写新帖子'), 'not-a-diagram\n'))
    await expect(first.locator('[data-mermaid-status]')).toContainText('上一次')
    expect(await page.evaluate(() => window.originalCharts[0].querySelector('svg') === window.lastGoodSVG)).toBe(true)
    await expect(first.locator('[data-mermaid-code]')).toContainText('not-a-diagram')
    await editor.fill(editedBody)
    await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2)
    await expect(first.locator('[data-mermaid-status]')).toBeHidden()
    expect(await page.evaluate(() => window.originalCharts[1].querySelector('svg') === window.originalSVGs[1])).toBe(true)
    expect(api.unexpected).toEqual([])
  })
}

for (const width of [390, 1440]) {
  test(`Mermaid ${width}px 编辑预览、发布正文、源码复制和缩放`, async ({ page, api }, testInfo) => {
    await page.setViewportSize({ width, height: 1000 })
    await loginAs(page, 'member')
    await page.goto('/member/community/new')
    await page.getByPlaceholder('给讨论起个清晰的标题').fill('Mermaid 图表示例')
    await page.getByRole('textbox', { name: '帖子正文' }).fill(body)
    if (width === 390) await page.getByRole('tab', { name: '预览', exact: true }).click()
    await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2, { timeout: 15000 })
    const diagram = page.locator('[data-mermaid-block]').first()
    await expect(diagram.locator('svg')).toBeVisible()
    await expect(diagram.locator('.community-mermaid__toolbar')).toHaveCount(0)
    await expect(diagram.locator('[data-mermaid-zoom]')).toBeHidden()
    await expect(diagram.getByRole('button', { name: '复制代码', exact: true })).toBeHidden()
    await expect(diagram.locator('[data-mermaid-code]')).toBeHidden()
    await page.screenshot({ path: testInfo.outputPath(`editor-pele-${width}.png`), fullPage: true })
    await page.getByRole('button', { name: '发布帖子', exact: true }).click()
    await expect(page).toHaveURL(/\/member\/community\/2$/)
    await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2, { timeout: 15000 })
    expect(api.forumPost.content).toBe(body.trim())
    await expect(diagram.locator('svg')).toContainText('发布文章')
    await page.context().grantPermissions(['clipboard-read', 'clipboard-write'])
    await diagram.getByLabel('图表选项').click()
    await diagram.getByRole('button', { name: '复制代码', exact: true }).click()
    expect((await page.evaluate(() => navigator.clipboard.readText())).replace(/\r\n/g, '\n')).toBe(flow)
    await diagram.getByLabel('图表选项').click()
    await diagram.getByRole('button', { name: '查看源码', exact: true }).click()
    await expect(diagram.locator('[data-mermaid-code]')).toBeVisible()
    await diagram.getByLabel('图表选项').click()
    await diagram.getByRole('button', { name: '收起源码', exact: true }).click()
    await diagram.getByRole('button', { name: '放大查看', exact: true }).click()
    await expect(diagram.getByRole('dialog')).toBeVisible()
    const oldWidth = await diagram.locator('svg').evaluate(element => element.style.width)
    await diagram.getByRole('button', { name: '放大图表', exact: true }).click()
    expect(await diagram.locator('svg').evaluate(element => element.style.width)).not.toBe(oldWidth)
    await diagram.getByRole('button', { name: '适应宽度', exact: true }).click()
    await expect(diagram.locator('svg')).toHaveCSS('width', oldWidth)
    await page.screenshot({ path: testInfo.outputPath(`expanded-pele-${width}.png`), fullPage: true })
    await page.keyboard.press('Escape')
    await expect(diagram.getByRole('dialog')).toBeHidden()
    await expect(diagram.getByRole('button', { name: '放大查看', exact: true })).toBeFocused()
    await expect(diagram.locator('svg')).toBeVisible()
    await expect(diagram.locator('[data-mermaid-zoom]')).toBeHidden()
    await diagram.getByRole('button', { name: '放大查看', exact: true }).click()
    await diagram.getByRole('button', { name: '关闭', exact: true }).click()
    await expect(diagram.getByRole('dialog')).toBeHidden()
    const ids = await page.locator('.community-markdown svg [id]').evaluateAll(elements => elements.map(element => element.id))
    expect(new Set(ids).size).toBe(ids.length)
    if (width === 1440) await expect(page.getByRole('complementary', { name: '文章目录' })).toBeVisible()
    if (width === 390) {
      await page.setViewportSize({ width: 320, height: 1000 })
      await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2)
      expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(320)
      await page.setViewportSize({ width, height: 1000 })
    }
    await page.getByRole('heading', { name: 'Mermaid 图表示例', exact: true }).click()
    await diagram.screenshot({ path: testInfo.outputPath(`diagram-pele-${width}.png`) })
    // Real backend encoding should not corrupt arrows in the published view.
    api.forumPost.content = body.replaceAll('>', '&gt;')
    await page.reload()
    await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(2, { timeout: 15000 })
  })
}

test('Pele 实际渲染类图、思维导图，并隔离错误图和危险链接图片', async ({ page, api }) => {
  const classDiagram = 'classDiagram\n  class Post {\n    +String title\n    +publish()\n  }\n  Post --> User\n'
  const mindmap = 'mindmap\n  root((学习路径))\n    HTML\n    CSS\n    Vue\n'
  const dangerous = 'flowchart LR\n A@{ img: "https://tracker.invalid/pixel.png", label: "开始", h:60 }-->B[结束]\n click A "https://tracker.invalid/" "外链"\n'
  api.forumPost.content = '## 图表示例\n\n' + fence(classDiagram) + fence(mindmap) + fence(dangerous) + fence('not-a-diagram\n <script>alert(1)</script>\n') + '\n## 其他正文\n\n正文仍然正常。'
  await loginAs(page, 'member')
  await page.goto('/member/community/1')
  await expect(page.locator('[data-mermaid-ready=true]')).toHaveCount(3, { timeout: 15000 })
  const failed = page.locator('[data-mermaid-block]').last()
  await expect(failed.locator('[data-mermaid-status]')).toContainText('图表未能显示')
  await expect(failed.locator('pre')).toBeVisible()
  await expect(page.getByText('正文仍然正常。')).toBeVisible()
  await expect(page.locator('.community-mermaid svg a,.community-mermaid svg image,.community-mermaid script,.community-mermaid foreignObject')).toHaveCount(0)
})

test('普通文章不下载 Pele Worker，图表超限保留源码而不阻塞正文', async ({ page, api }) => {
  const requested = []
  page.on('request', request => requested.push(request.url()))
  await loginAs(page, 'member')
  await page.goto('/member/community/1')
  await expect(page.getByText('欢迎讨论移动端交互。')).toBeVisible()
  expect(requested.filter(url => /mermaid\.worker|mermaid-diagrams/.test(url))).toEqual([])
  api.forumPost.content = '## 复杂图表\n\n' + fence('flowchart LR\n' + ' A-->B\n'.repeat(1200)) + '\n结束正文。'
  await page.reload()
  await expect(page.locator('[data-mermaid-status]')).toContainText('8000')
  await expect(page.locator('[data-mermaid-code]')).toBeVisible()
  await expect(page.getByText('结束正文。')).toBeVisible()
})
