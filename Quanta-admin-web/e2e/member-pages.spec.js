import { expect, test } from '@playwright/test'

// Quanta Pages 控制台端到端验证：自包含 API 拦截（不依赖共享 fixture，
// 因为 fixture.js 承载了另一项尚未提交的工作），覆盖创建→部署→失败日志→回滚与新生拦截。
const appOrigin = 'http://127.0.0.1:5177'
const apiOrigin = 'http://127.0.0.1:18080'

const corsHeaders = {
  'access-control-allow-origin': appOrigin,
  'access-control-allow-credentials': 'true',
  'access-control-allow-methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'access-control-allow-headers': 'authorization, content-type, x-trace-id',
}

const memberUser = {
  userId: 102, userName: 'member_demo', nickName: '塔员同学', isQuantaMember: 1, memberDepartment: '产品部',
}
const freshmanUser = {
  userId: 101, userName: 'freshman_demo', nickName: '新生同学', isQuantaMember: 0,
}

function siteRow(overrides = {}) {
  return {
    siteId: 9, slug: 'octo-lab', repoUrl: 'https://github.com/octo/hello', repoOwner: 'octo', repoName: 'hello',
    branch: 'pages', publishDir: '/', currentSha: 'abcdef1234567890abcdef1234567890abcdef12',
    currentDeploymentId: 20, currentReleaseId: 20, status: 'ENABLED',
    lastCheckedTime: '2026-10-25 10:00:00', lastCheckError: '', ...overrides,
  }
}

function deploymentRow(id, overrides = {}) {
  return {
    deploymentId: id, requestVersion: id, triggerType: 'POLL', status: 'SUCCESS',
    commitSha: 'abcdef1234567890abcdef1234567890abcdef12', branch: 'pages', publishDir: '/',
    errorCode: '', errorMessage: '', logText: `resolved commit abcdef1 (deployment ${id})`,
    fileCount: 3, totalBytes: 300, releaseAvailable: true,
    createTime: '2026-10-25 09:00:00', finishedTime: '2026-10-25 09:01:00', ...overrides,
  }
}

function initialState() {
  return {
    mode: 'success',
    site: siteRow(),
    deployments: [
      deploymentRow(20, { triggerType: 'CREATE' }),
      deploymentRow(21, { requestVersion: 2, status: 'FAILED', releaseAvailable: false, errorCode: 'ARCHIVE_TOO_LARGE', errorMessage: '归档超过下载上限' }),
    ],
  }
}

function respond(state, request, url) {
  const method = request.method()
  const path = url.pathname
  if (method === 'POST' && path === '/login') return { code: 200, token: 'e2e-pages-token' }
  if (method === 'GET' && path === '/getInfo') {
    return {
      code: 200,
      user: state.freshman ? freshmanUser : memberUser,
      roles: state.freshman ? [] : ['member'],
      permissions: [],
    }
  }
  if (path === '/qt/pages/sites/me' && method === 'GET') {
    if (state.mode === 'disabled') return { code: 503, msg: '个人网站功能未开启' }
    return { code: 200, data: state.site }
  }
  if (path === '/qt/pages/sites' && method === 'POST') {
    if (state.mode === 'conflict') return { code: 409, msg: '该站点标识已被使用' }
    const body = request.postDataJSON()
    state.site = siteRow({
      slug: body.slug || 'octo-lab',
      repoUrl: body.repoUrl,
      branch: body.branch || 'pages',
      publishDir: body.publishDir || '/',
      currentSha: '', currentDeploymentId: null, currentReleaseId: null,
    })
    state.deployments = [deploymentRow(20, { triggerType: 'CREATE', status: 'PENDING', commitSha: '', requestVersion: 1 })]
    return { code: 200, data: { site: state.site, deployment: state.deployments[0] } }
  }
  if (/^\/qt\/pages\/sites\/\d+$/.test(path) && method === 'PUT') {
    const body = request.postDataJSON()
    state.site = { ...state.site, ...body }
    const deployment = deploymentRow(Date.now(), { triggerType: 'CONFIG', status: 'PENDING', commitSha: '', requestVersion: state.deployments.length + 1 })
    state.deployments.unshift(deployment)
    return { code: 200, data: { site: state.site, deployment } }
  }
  if (/^\/qt\/pages\/sites\/\d+\/deploy$/.test(path) && method === 'POST') {
    if (state.mode === 'rate-limited') return { code: 429, msg: '访问过于频繁，请稍候再试' }
    const deployment = deploymentRow(Date.now(), { triggerType: 'MANUAL', status: 'PENDING', commitSha: '', requestVersion: state.deployments.length + 1 })
    state.deployments.unshift(deployment)
    return { code: 200, data: deployment }
  }
  if (/^\/qt\/pages\/sites\/\d+\/deployments$/.test(path) && method === 'GET') {
    return { code: 200, rows: state.deployments, total: state.deployments.length }
  }
  const detailMatch = path.match(/^\/qt\/pages\/sites\/\d+\/deployments\/(\d+)$/)
  if (detailMatch && method === 'GET') {
    const row = state.deployments.find((item) => String(item.deploymentId) === detailMatch[1])
    return row ? { code: 200, data: row } : { code: 404, msg: '部署记录不存在' }
  }
  const rollbackMatch = path.match(/^\/qt\/pages\/sites\/\d+\/rollback\/(\d+)$/)
  if (rollbackMatch && method === 'POST') {
    const source = state.deployments.find((item) => String(item.deploymentId) === rollbackMatch[1])
    if (!source || source.status !== 'SUCCESS' || !source.releaseAvailable) {
      return { code: 409, msg: '该版本的发布文件已被清理，无法回滚' }
    }
    const rollback = deploymentRow(Date.now(), {
      triggerType: 'ROLLBACK', status: 'PENDING', releaseId: source.releaseId ?? source.deploymentId,
      commitSha: source.commitSha, requestVersion: state.deployments.length + 1,
    })
    state.deployments.unshift(rollback)
    state.site = { ...state.site, currentDeploymentId: rollback.deploymentId }
    return { code: 200, data: rollback }
  }
  return null
}

async function mockApi(context, state) {
  await context.route('**/*', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (url.origin === appOrigin) return route.continue()
    if (url.origin !== apiOrigin) return route.abort()
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers: corsHeaders })
    const payload = respond(state, request, url)
    if (!payload) return route.fulfill({ status: 599, headers: corsHeaders })
    return route.fulfill({ status: 200, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify(payload) })
  })
}

async function loginAsMember(page, state, { freshman = false } = {}) {
  state.freshman = freshman
  await page.goto(freshman ? '/login/freshman' : '/login/member')
  await page.getByPlaceholder(freshman ? '请输入学号' : '请输入学号或用户名')
    .fill(freshman ? 'freshman_demo' : 'member_demo')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('demo-password')
  await page.getByRole('button', {
    name: freshman ? '登录并进入新生门户' : '登录并进入塔员门户',
    exact: true,
  }).click()
  await expect(page).toHaveURL(/\/(member|freshman)\/home/)
}

test.describe('member pages console', () => {
  let state

  test.beforeEach(async ({ context }) => {
    state = initialState()
    await mockApi(context, state)
  })

  test('member sees the owned site with current release and history', async ({ page }) => {
    await loginAsMember(page, state)
    await page.goto('/member/pages')
    await expect(page.getByTestId('pages-visit-link')).toContainText('octo-lab.page.quantacenter.com')
    await expect(page.getByTestId('pages-current-sha')).toContainText('abcdef1')
    await expect(page.getByTestId('pages-history')).toContainText('当前在线')
    await expect(page.getByTestId('pages-history')).toContainText('发布失败')
  })

  test('creation guide posts and switches to the owned view', async ({ page }) => {
    state.site = null
    await loginAsMember(page, state)
    await page.goto('/member/pages')
    await expect(page.getByTestId('pages-guide')).toBeVisible()
    await page.getByTestId('pages-repo-url').fill('https://github.com/octo/hello')
    await page.getByTestId('pages-form-submit').click()
    await expect(page.getByTestId('pages-visit-link')).toContainText('octo-lab.page.quantacenter.com')
  })

  test('one-click template creates a site without GitHub', async ({ page }) => {
    state.site = null
    await loginAsMember(page, state)
    await page.goto('/member/pages')
    await expect(page.getByTestId('pages-template-form')).toBeVisible()
    await expect(page.locator('[data-testid^="pages-template-option-"]')).toHaveCount(4)
    await page.getByTestId('pages-template-option-paper').click()
    await page.getByTestId('pages-template-slug').fill('my-space')
    await page.getByTestId('pages-template-submit').click()
    await expect(page.getByTestId('pages-visit-link')).toContainText('my-space.page.quantacenter.com')
    await expect(page.getByTestId('pages-template-badge')).toContainText('纸感文字')
    expect(state.site.repoUrl).toBe('https://github.com/subingbo/quanta-pages-template')
    expect(state.site.branch).toBe('main')
    expect(state.site.publishDir).toBe('/templates/paper')
  })

  test('creation conflict surfaces the backend message', async ({ page }) => {
    state.site = null
    state.mode = 'conflict'
    await loginAsMember(page, state)
    await page.goto('/member/pages')
    await page.getByTestId('pages-repo-url').fill('https://github.com/octo/hello')
    await page.getByTestId('pages-form-submit').click()
    await expect(page.getByTestId('pages-form-error')).toContainText('该站点标识已被使用')
  })

  test('deploy keeps the old online address visible and shows the log as text', async ({ page }) => {
    await loginAsMember(page, state)
    await page.goto('/member/pages')
    await page.getByTestId('pages-deploy-button').click()
    await expect(page.getByTestId('pages-active-hint')).toContainText('旧版本继续提供访问')
    await expect(page.getByTestId('pages-visit-link')).toBeVisible()

    // 模拟任务失败后查看日志：日志按纯文本渲染
    const row = state.deployments[0]
    row.status = 'FAILED'
    row.errorMessage = '归档超过下载上限'
    row.logText = 'failed ARCHIVE_TOO_LARGE <img src=x onerror=alert(1)>'
    await page.reload()
    await expect(page.getByTestId('pages-history')).toContainText('归档超过下载上限')
    await page.getByRole('button', { name: '查看日志' }).first().click()
    await expect(page.getByTestId('pages-log-text')).toContainText('failed ARCHIVE_TOO_LARGE <img src=x onerror=alert(1)>')
  })

  test('rollback queues an independent rollback deployment', async ({ page }) => {
    await loginAsMember(page, state)
    await page.goto('/member/pages')
    // deployment 21 是失败记录（不可回滚），20 是当前在线（不可回滚）：补一条可回滚的旧版本
    state.deployments.push(deploymentRow(19, { requestVersion: 0 }))
    await page.reload()
    await expect(page.getByTestId('pages-history')).toBeVisible()
    await page.locator('[data-testid="pages-rollback-button"]:not([disabled])').first().click()
    await page.locator('.el-message-box__btns button').last().click()
    await expect(page.getByTestId('pages-history')).toContainText('排队中')
  })

  test('freshman is redirected away from the member pages console', async ({ page }) => {
    state.site = null
    await loginAsMember(page, state, { freshman: true })
    await page.goto('/member/pages')
    await expect(page).toHaveURL(/\/freshman\/home/)
  })
})
