import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  request: vi.fn(),
}))

import { request } from '@/utils/request'
import {
  createPageSite,
  deployPageSite,
  getMyPageSite,
  getPageDeployment,
  getPageDeployments,
  isDeploymentActive,
  rollbackPageSite,
  updatePageSite,
} from '../pages'

function mockedResponse(payload) {
  request.mockResolvedValueOnce(payload)
}

describe('portal pages api', () => {
  beforeEach(() => {
    request.mockReset()
  })

  it('getMyPageSite returns null for the empty state', async () => {
    mockedResponse({ code: 200 })
    expect(await getMyPageSite()).toBeNull()
    expect(request).toHaveBeenCalledWith({ url: '/qt/pages/sites/me', method: 'get' })
  })

  it('getMyPageSite maps the owned site', async () => {
    mockedResponse({
      code: 200,
      data: { siteId: '9', slug: 'octo-lab', repoUrl: 'https://github.com/octo/hello', branch: 'pages', currentSha: 'abc' },
    })
    const site = await getMyPageSite()
    expect(site.siteId).toBe(9)
    expect(site.slug).toBe('octo-lab')
    expect(site.branch).toBe('pages')
  })

  it('createPageSite posts the body and maps site + deployment', async () => {
    mockedResponse({
      code: 200,
      data: {
        site: { siteId: 9, slug: 'octo-lab' },
        deployment: { deploymentId: 20, status: 'PENDING', requestVersion: '1' },
      },
    })
    const result = await createPageSite({ repoUrl: 'https://github.com/octo/hello' })
    expect(request).toHaveBeenCalledWith({
      url: '/qt/pages/sites',
      method: 'post',
      data: { repoUrl: 'https://github.com/octo/hello' },
    })
    expect(result.site.siteId).toBe(9)
    expect(result.deployment.deploymentId).toBe(20)
    expect(result.deployment.status).toBe('PENDING')
  })

  it('updatePageSite puts to the owned site only', async () => {
    mockedResponse({ code: 200, data: { site: { siteId: 9 }, deployment: {} } })
    await updatePageSite(9, { repoUrl: 'https://github.com/octo/site', branch: 'pages', publishDir: '/dist' })
    expect(request).toHaveBeenCalledWith({
      url: '/qt/pages/sites/9',
      method: 'put',
      data: { repoUrl: 'https://github.com/octo/site', branch: 'pages', publishDir: '/dist' },
    })
  })

  it('deployPageSite posts deploy and maps the queued deployment', async () => {
    mockedResponse({ code: 200, data: { deploymentId: 30, status: 'PENDING' } })
    const deployment = await deployPageSite(9)
    expect(request).toHaveBeenCalledWith({ url: '/qt/pages/sites/9/deploy', method: 'post' })
    expect(deployment.deploymentId).toBe(30)
  })

  it('getPageDeployments clamps pageSize and maps rows/total', async () => {
    mockedResponse({ code: 200, rows: [{ deploymentId: 1, status: 'SUCCESS', releaseAvailable: 1 }], total: '12' })
    const page = await getPageDeployments(9, { pageNum: 2, pageSize: 500 })
    expect(request).toHaveBeenCalledWith({
      url: '/qt/pages/sites/9/deployments',
      method: 'get',
      params: { pageNum: 2, pageSize: 100 },
    })
    expect(page.total).toBe(12)
    expect(page.rows[0].releaseAvailable).toBe(true)
  })

  it('getPageDeployment fetches a single record', async () => {
    mockedResponse({ code: 200, data: { deploymentId: 20, logText: 'step' } })
    const deployment = await getPageDeployment(9, 20)
    expect(request).toHaveBeenCalledWith({ url: '/qt/pages/sites/9/deployments/20', method: 'get' })
    expect(deployment.logText).toBe('step')
  })

  it('rollbackPageSite posts to the rollback endpoint', async () => {
    mockedResponse({ code: 200, data: { deploymentId: 60, triggerType: 'ROLLBACK', status: 'PENDING' } })
    const deployment = await rollbackPageSite(9, 30)
    expect(request).toHaveBeenCalledWith({ url: '/qt/pages/sites/9/rollback/30', method: 'post' })
    expect(deployment.triggerType).toBe('ROLLBACK')
  })

  it('isDeploymentActive matches the backend non-terminal statuses', () => {
    for (const status of ['PENDING', 'RESOLVING', 'DOWNLOADING', 'VALIDATING', 'PUBLISHING']) {
      expect(isDeploymentActive(status)).toBe(true)
    }
    for (const status of ['SUCCESS', 'FAILED', 'SUPERSEDED']) {
      expect(isDeploymentActive(status)).toBe(false)
    }
  })
})
