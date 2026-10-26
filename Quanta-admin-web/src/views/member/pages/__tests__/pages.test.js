import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))

vi.mock('@/api/portal/pages', () => ({
  getMyPageSite: vi.fn(),
  createPageSite: vi.fn(),
  updatePageSite: vi.fn(),
  deployPageSite: vi.fn(),
  getPageDeployments: vi.fn(),
  getPageDeployment: vi.fn(),
  rollbackPageSite: vi.fn(),
  isDeploymentActive: (status) => ['PENDING', 'RESOLVING', 'DOWNLOADING', 'VALIDATING', 'PUBLISHING'].includes(status),
}))

import * as pagesApi from '@/api/portal/pages'
import { ElMessage, ElMessageBox } from 'element-plus'
import SiteConfigForm from '../components/SiteConfigForm.vue'
import DeploymentHistory from '../components/DeploymentHistory.vue'
import Index from '../index.vue'

const ownedSite = {
  siteId: 9,
  slug: 'octo-lab',
  repoUrl: 'https://github.com/octo/hello',
  branch: 'pages',
  publishDir: '/',
  currentSha: 'abcdef1234567890abcdef1234567890abcdef12',
  currentDeploymentId: 20,
  currentReleaseId: 20,
  status: 'ENABLED',
  lastCheckedTime: '2026-10-25 10:00:00',
  lastCheckError: '',
}

function successDeployment(overrides = {}) {
  return {
    deploymentId: 20, requestVersion: 1, triggerType: 'MANUAL', status: 'SUCCESS',
    commitSha: 'abcdef1234567890abcdef1234567890abcdef12', branch: 'pages', publishDir: '/',
    errorCode: '', errorMessage: '', logText: 'resolved commit abc', fileCount: 1, totalBytes: 10,
    releaseAvailable: true, createTime: '2026-10-25 09:00:00', finishedTime: '2026-10-25 09:01:00',
    ...overrides,
  }
}

async function mountIndex() {
  const wrapper = mount(Index, { global: { stubs: { RouterLink: true } } })
  await flushPromises()
  return wrapper
}

describe('member pages console', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pagesApi.getPageDeployments.mockResolvedValue({ rows: [], total: 0 })
    document.body.innerHTML = ''
  })

  afterEach(() => {
    vi.useRealTimers()
    document.body.innerHTML = ''
  })

  it('shows the creation guide before the first site exists', async () => {
    pagesApi.getMyPageSite.mockResolvedValueOnce(null)
    const wrapper = await mountIndex()
    expect(wrapper.find('[data-testid="pages-guide"]').exists()).toBe(true)
    expect(wrapper.findComponent(SiteConfigForm).exists()).toBe(true)
    expect(wrapper.text()).toContain('还没有个人网站')
  })

  it('surfaces backend conflict messages instead of faking success', async () => {
    pagesApi.getMyPageSite.mockResolvedValueOnce(null)
    const wrapper = await mountIndex()
    pagesApi.createPageSite.mockRejectedValueOnce(Object.assign(new Error('该站点标识已被使用'), { code: 409 }))

    await wrapper.find('[data-testid="pages-repo-url"]').setValue('https://github.com/octo/hello')
    await wrapper.find('[data-testid="pages-form-submit"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[data-testid="pages-form-error"]').text()).toContain('该站点标识已被使用')
    expect(ElMessage.success).not.toHaveBeenCalled()
  })

  it('creates the site from the guide and switches to the owned view', async () => {
    pagesApi.getMyPageSite.mockResolvedValueOnce(null)
    pagesApi.getMyPageSite.mockResolvedValueOnce(ownedSite)
    pagesApi.createPageSite.mockResolvedValueOnce({ site: ownedSite, deployment: successDeployment() })
    const wrapper = await mountIndex()

    await wrapper.find('[data-testid="pages-repo-url"]').setValue('https://github.com/octo/hello')
    await wrapper.find('[data-testid="pages-form-submit"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[data-testid="pages-guide"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="pages-visit-link"]').attributes('href')).toBe('https://octo-lab.page.quantacenter.com')
    expect(wrapper.find('[data-testid="pages-current-sha"]').text()).toContain('abcdef1')
  })

  it('shows load errors with a retry instead of pretending success', async () => {
    pagesApi.getMyPageSite.mockRejectedValueOnce(new Error('个人网站功能未开启'))
    const wrapper = await mountIndex()
    expect(wrapper.find('[data-testid="pages-load-error"]').text()).toContain('个人网站功能未开启')
    pagesApi.getMyPageSite.mockResolvedValueOnce(null)
    await wrapper.find('[data-testid="pages-load-error"] button').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="pages-guide"]').exists()).toBe(true)
  })

  it('saves configuration updates for an owned site', async () => {
    pagesApi.getMyPageSite.mockResolvedValueOnce(ownedSite)
    pagesApi.getMyPageSite.mockResolvedValueOnce(ownedSite)
    pagesApi.updatePageSite.mockResolvedValueOnce({ site: ownedSite, deployment: successDeployment({ status: 'PENDING' }) })
    const wrapper = await mountIndex()

    await wrapper.find('[data-testid="pages-publish-dir"]').setValue('/dist')
    await wrapper.find('[data-testid="pages-form-submit"]').trigger('submit')
    await flushPromises()

    expect(pagesApi.updatePageSite).toHaveBeenCalledWith(9, {
      repoUrl: 'https://github.com/octo/hello', branch: 'pages', publishDir: '/dist',
    })
    expect(ElMessage.success).toHaveBeenCalledWith('配置已保存，已按新配置排队部署')
  })

  it('disables rollback for pruned releases and rolls back available ones', async () => {
    pagesApi.getMyPageSite.mockResolvedValue(ownedSite)
    pagesApi.getPageDeployments.mockResolvedValue({
      rows: [
        successDeployment({ deploymentId: 30, requestVersion: 2, releaseAvailable: false, status: 'SUCCESS' }),
        successDeployment({ deploymentId: 31, requestVersion: 3 }),
      ],
      total: 2,
    })
    const wrapper = await mountIndex()
    const rollbackButtons = wrapper.findAll('[data-testid="pages-rollback-button"]')
    expect(rollbackButtons[0].attributes('disabled')).toBeDefined()
    expect(rollbackButtons[1].attributes('disabled')).toBeUndefined()

    ElMessageBox.confirm.mockResolvedValueOnce('confirm')
    pagesApi.rollbackPageSite.mockResolvedValueOnce(successDeployment({ deploymentId: 60, triggerType: 'ROLLBACK', status: 'PENDING' }))
    pagesApi.getMyPageSite.mockResolvedValueOnce({ ...ownedSite, currentDeploymentId: 60 })
    await rollbackButtons[1].trigger('click')
    await flushPromises()

    expect(pagesApi.rollbackPageSite).toHaveBeenCalledWith(9, 31)
    expect(ElMessage.success).toHaveBeenCalledWith('回滚已排队，稍后生效')
  })

  it('keeps the old online address visible while a deployment is running', async () => {
    pagesApi.getMyPageSite.mockResolvedValue(ownedSite)
    pagesApi.getPageDeployments.mockResolvedValue({
      rows: [successDeployment({ deploymentId: 40, requestVersion: 4, status: 'DOWNLOADING', commitSha: '' })],
      total: 1,
    })
    const wrapper = await mountIndex()
    expect(wrapper.find('[data-testid="pages-active-hint"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pages-active-hint"]').text()).toContain('旧版本继续提供访问')
    expect(wrapper.find('[data-testid="pages-visit-link"]').exists()).toBe(true)
  })

  it('renders the deployment log as plain text', async () => {
    pagesApi.getMyPageSite.mockResolvedValue(ownedSite)
    pagesApi.getPageDeployments.mockResolvedValue({
      rows: [successDeployment({ logText: 'resolved commit abc\n<script>alert(1)</script>' })],
      total: 1,
    })
    const wrapper = await mountIndex()
    await wrapper.findAll('button').filter((b) => b.text() === '查看日志')[0].trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="pages-log-dialog"]').exists()).toBe(true)
    const pre = wrapper.find('[data-testid="pages-log-text"]')
    expect(pre.text()).toContain('<script>alert(1)</script>')
    expect(pre.find('script').exists()).toBe(false)
  })

  it('polls every 5 seconds only while visible and a deployment is running', async () => {
    vi.useFakeTimers()
    pagesApi.getMyPageSite.mockResolvedValue(ownedSite)
    pagesApi.getPageDeployments.mockResolvedValue({
      rows: [successDeployment({ deploymentId: 40, status: 'PUBLISHING' })],
      total: 1,
    })
    const wrapper = await mountIndex()
    const callsAfterMount = pagesApi.getMyPageSite.mock.calls.length

    await vi.advanceTimersByTimeAsync(5000)
    expect(pagesApi.getMyPageSite.mock.calls.length).toBe(callsAfterMount + 1)

    // 离开页面（卸载）后停止轮询
    wrapper.unmount()
    const callsAfterUnmount = pagesApi.getMyPageSite.mock.calls.length
    await vi.advanceTimersByTimeAsync(15000)
    expect(pagesApi.getMyPageSite.mock.calls.length).toBe(callsAfterUnmount)
  })

  it('does not poll when every deployment reached a terminal state', async () => {
    vi.useFakeTimers()
    pagesApi.getMyPageSite.mockResolvedValue(ownedSite)
    pagesApi.getPageDeployments.mockResolvedValue({ rows: [successDeployment()], total: 1 })
    await mountIndex()
    const calls = pagesApi.getMyPageSite.mock.calls.length
    await vi.advanceTimersByTimeAsync(15000)
    expect(pagesApi.getMyPageSite.mock.calls.length).toBe(calls)
  })

  it('surfaces polling failures without faking SUCCESS', async () => {
    vi.useFakeTimers()
    pagesApi.getMyPageSite.mockResolvedValue(ownedSite)
    pagesApi.getPageDeployments.mockResolvedValue({
      rows: [successDeployment({ deploymentId: 40, status: 'PENDING' })],
      total: 1,
    })
    const wrapper = await mountIndex()
    pagesApi.getMyPageSite.mockRejectedValueOnce(new Error('网络连接异常'))
    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()
    expect(wrapper.find('[data-testid="pages-poll-error"]').text()).toContain('网络连接异常')
    expect(wrapper.text()).not.toContain('发布成功站点已更新')
  })
})

describe('site config form contract', () => {
  it('locks the slug field when editing an existing site', () => {
    const wrapper = mount(SiteConfigForm, { props: { site: ownedSite } })
    expect(wrapper.find('[data-testid="pages-slug"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-testid="pages-form-submit"]').text()).toContain('保存配置')
  })
})

describe('deployment history contract', () => {
  it('labels the current deployment and hides rollback for it', () => {
    const wrapper = mount(DeploymentHistory, {
      props: { site: ownedSite, rows: [successDeployment()], total: 1 },
    })
    expect(wrapper.text()).toContain('当前在线')
    expect(wrapper.find('[data-testid="pages-rollback-button"]').attributes('disabled')).toBeDefined()
  })

  it('surfaces backend errors from deploy actions without mock fallback', async () => {
    const wrapper = mount(DeploymentHistory, {
      props: { site: ownedSite, rows: [successDeployment()], total: 1 },
    })
    pagesApi.deployPageSite.mockRejectedValueOnce(Object.assign(new Error('功能正在恢复，请稍后再试'), { code: 503 }))
    await wrapper.find('[data-testid="pages-deploy-button"]').trigger('click')
    await flushPromises()
    expect(ElMessage.error).toHaveBeenCalledWith('功能正在恢复，请稍后再试')
  })
})

describe('site api guide', () => {
  it('lists the open endpoints for the owned slug with privacy hints', async () => {
    pagesApi.getMyPageSite.mockResolvedValueOnce(ownedSite)
    const wrapper = await mountIndex()

    const guide = wrapper.find('[data-testid="pages-api-guide"]')
    expect(guide.exists()).toBe(true)
    expect(guide.text()).toContain('/open/pages/octo-lab/posts')
    expect(guide.text()).toContain('/open/pages/octo-lab/posts/{postId}/comments')
    expect(guide.text()).toContain('社区昵称')
    expect(guide.text()).toContain('图片在公网页面无法加载')
    expect(wrapper.findAll('[data-testid="pages-api-endpoint"]')).toHaveLength(3)
  })

  it('copies the fetch snippet to the clipboard', async () => {
    pagesApi.getMyPageSite.mockResolvedValueOnce(ownedSite)
    const writeText = vi.fn().mockResolvedValue()
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    const wrapper = await mountIndex()

    await wrapper.find('[data-testid="pages-api-copy"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('/open/pages/octo-lab/posts?pageNum=1&pageSize=20'))
    expect(ElMessage.success).toHaveBeenCalledWith('示例代码已复制')
  })
})
