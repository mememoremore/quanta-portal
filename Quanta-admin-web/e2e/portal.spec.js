import { test, expect, loginAs } from './support/fixture.js'
import { fileURLToPath } from 'node:url'

const samplePhoto = fileURLToPath(new URL('../src/assets/quanta-logo.jpg', import.meta.url))

async function fillValidApplication(page) {
  await page.locator('[name="realName"]').fill('测试新生')
  await page.locator('[name="gender"]').selectOption({ label: '男' })
  await page.locator('[name="className"]').fill('软工 2401')
  await page.locator('[name="firstChoice"]').click()
  await page.locator('[name="firstChoice"]').locator('..').getByRole('option', { name: '产品' }).click()
  await page.locator('[name="secondChoice"]').click()
  await page.locator('[name="secondChoice"]').locator('..').getByRole('option', { name: '设计' }).click()
  await page.locator('[name="photoFile"]').setInputFiles(samplePhoto)
  await page.locator('[name="resumeFile"]').setInputFiles({
    name: 'resume.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4\n%%EOF'),
  })
  await page.locator('[name="selfIntro"]').fill('喜欢协作和产品设计。')
  await page.locator('input[type="radio"][value="0"]').check()
  await page.locator('[name="quantaUnderstanding"]').fill('希望在社团参与真实项目。')
}

test('新生报名关闭时显示截止提示', async ({ page, api }) => {
  await loginAs(page, 'freshman')
  await page.goto('/freshman/recruitment')
  await page.getByRole('button', { name: '填写报名' }).click()
  await expect(page.getByTestId('recruitment-closed-notice')).toBeVisible()
  expect(api.requests).toContain('GET /qt/interview/my')
})

test('新生报名表校验文件类型，并能保存和恢复本地草稿', async ({ page, api }) => {
  api.recruitmentOpen = true
  await loginAs(page, 'freshman')
  await page.goto('/freshman/recruitment')
  await page.getByRole('button', { name: '填写报名' }).click()
  await page.locator('[name="realName"]').fill('测试新生')
  await page.locator('[name="resumeFile"]').setInputFiles({ name: 'note.txt', mimeType: 'text/plain', buffer: Buffer.from('not a pdf') })
  await expect(page.getByTestId('resume-error')).toContainText('PDF')
  await page.getByRole('button', { name: '保存草稿' }).click()
  await expect(page.getByText('草稿已保存')).toBeVisible()
  await page.reload()
  await page.getByRole('button', { name: '填写报名' }).click()
  await expect(page.locator('[name="realName"]')).toHaveValue('测试新生')
  expect(api.requests).not.toContain('POST /qt/interview/apply')
})

test('新生上传超过 10 MB 的简历会在浏览器内拦截', async ({ page, api }) => {
  api.recruitmentOpen = true
  await loginAs(page, 'freshman')
  await page.goto('/freshman/recruitment')
  await page.getByRole('button', { name: '填写报名' }).click()
  await page.locator('[name="resumeFile"]').setInputFiles({
    name: 'oversize.pdf', mimeType: 'application/pdf', buffer: Buffer.alloc(10 * 1024 * 1024 + 1),
  })
  await expect(page.getByTestId('resume-error')).toContainText('不能超过 10 MB')
  expect(api.requests).not.toContain('POST /qt/interview/apply')
})

test('新生报名提交成功后展示进度入口', async ({ page, api }) => {
  api.recruitmentOpen = true
  await loginAs(page, 'freshman')
  await page.goto('/freshman/recruitment')
  await page.getByRole('button', { name: '填写报名' }).click()
  await fillValidApplication(page)
  await page.getByRole('button', { name: '提交报名' }).click()
  await expect(page.getByText('提交成功')).toBeVisible()
  expect(api.requests).toContain('POST /qt/interview/apply')
})

test('新生报名提交失败时保留已填内容并显示后端错误', async ({ page, api }) => {
  api.recruitmentOpen = true
  api.applyResult = 'failure'
  await loginAs(page, 'freshman')
  await page.goto('/freshman/recruitment')
  await page.getByRole('button', { name: '填写报名' }).click()
  await fillValidApplication(page)
  await page.getByRole('button', { name: '提交报名' }).click()
  await expect(page.getByText('模拟提交失败')).toBeVisible()
  await expect(page.locator('[name="realName"]')).toHaveValue('测试新生')
  expect(api.requests).toContain('POST /qt/interview/apply')
})

test('塔员论坛可按分类筛选，手机端也能访问', async ({ page, api }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await loginAs(page, 'member')
  await page.goto('/member/community')
  await expect(page.getByRole('heading', { name: '社区论坛' })).toBeVisible()
  await expect(page.getByText('一起优化移动端报名页')).toBeVisible()
  await page.locator('[data-category="TECH"]').click()
  await expect(page.getByText('还没有讨论，来发布第一篇帖子吧')).toBeVisible()
  expect(api.requests).toContain('GET /qt/forum/posts')
})

test('管理端招新列表与评定权限按账号区分', async ({ page, api }) => {
  await loginAs(page, 'admin')
  await page.goto('/admin/recruitment')
  await page.getByRole('tab', { name: '一面' }).click()
  await expect(page.getByText('测试候选人')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Pass' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Out' })).toBeVisible()
  expect(api.requests).toContain('GET /qt/interview/admin/applications')
})

test('仅有列表权限的管理账号看不到 Pass / Out', async ({ page, api }) => {
  await loginAs(page, 'viewer')
  await page.goto('/admin/recruitment')
  await page.getByRole('tab', { name: '一面' }).click()
  await expect(page.getByText('测试候选人')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Pass' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Out' })).toHaveCount(0)
  expect(api.requests).toContain('GET /qt/interview/admin/applications')
})

test('管理端一面名单可以翻页并按姓名筛选', async ({ page, api }) => {
  api.candidateCount = 12
  await loginAs(page, 'admin')
  await page.goto('/admin/recruitment')
  await page.getByRole('tab', { name: '一面' }).click()
  const pagination = page.getByTestId('recruitment-pagination')
  await expect(pagination).toContainText('共 12 位候选人')
  await pagination.locator('.el-pager li').filter({ hasText: '2' }).click()
  await expect(page.getByText('测试候选人12')).toBeVisible()
  await page.getByTestId('recruitment-keyword').fill('测试候选人12')
  await page.getByTestId('recruitment-search').click()
  await expect(page.getByText('测试候选人12')).toBeVisible()
  await expect(page.getByTestId('recruitment-pagination')).toHaveCount(0)
  expect(api.requests.filter((item) => item === 'GET /qt/interview/admin/applications').length).toBeGreaterThan(1)
})
