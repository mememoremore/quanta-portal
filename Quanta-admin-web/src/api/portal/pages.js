import { request } from '@/utils/request'

// 与后端 QtPageDeployment 的非终态集合保持一致
export const PAGES_ACTIVE_STATUSES = ['PENDING', 'RESOLVING', 'DOWNLOADING', 'VALIDATING', 'PUBLISHING']

export function isDeploymentActive(status) {
  return PAGES_ACTIVE_STATUSES.includes(status)
}

function mapSite(row = {}) {
  return {
    siteId: row.siteId == null ? null : Number(row.siteId),
    slug: row.slug || '',
    repoUrl: row.repoUrl || '',
    repoOwner: row.repoOwner || '',
    repoName: row.repoName || '',
    branch: row.branch || 'pages',
    publishDir: row.publishDir || '/',
    currentSha: row.currentSha || '',
    currentDeploymentId: row.currentDeploymentId == null ? null : Number(row.currentDeploymentId),
    currentReleaseId: row.currentReleaseId == null ? null : Number(row.currentReleaseId),
    status: row.status || 'ENABLED',
    lastCheckedTime: row.lastCheckedTime || '',
    lastCheckError: row.lastCheckError || '',
    nextPollTime: row.nextPollTime || '',
  }
}

function mapDeployment(row = {}) {
  return {
    deploymentId: row.deploymentId == null ? null : Number(row.deploymentId),
    requestVersion: row.requestVersion == null ? null : Number(row.requestVersion),
    triggerType: row.triggerType || '',
    status: row.status || 'PENDING',
    commitSha: row.commitSha || '',
    branch: row.branch || 'pages',
    publishDir: row.publishDir || '/',
    errorCode: row.errorCode || '',
    errorMessage: row.errorMessage || '',
    logText: row.logText || '',
    fileCount: row.fileCount == null ? null : Number(row.fileCount),
    totalBytes: row.totalBytes == null ? null : Number(row.totalBytes),
    releaseAvailable: Boolean(row.releaseAvailable),
    createTime: row.createTime || '',
    finishedTime: row.finishedTime || '',
  }
}

/** 本人站点；尚未创建时返回 null（正常空状态） */
export async function getMyPageSite() {
  const response = await request({ url: '/qt/pages/sites/me', method: 'get' })
  return response.data ? mapSite(response.data) : null
}

export async function createPageSite(body) {
  const response = await request({ url: '/qt/pages/sites', method: 'post', data: body })
  const data = response.data || {}
  return { site: mapSite(data.site || {}), deployment: mapDeployment(data.deployment || {}) }
}

export async function updatePageSite(siteId, body) {
  const response = await request({ url: `/qt/pages/sites/${siteId}`, method: 'put', data: body })
  const data = response.data || {}
  return { site: mapSite(data.site || {}), deployment: mapDeployment(data.deployment || {}) }
}

export async function deployPageSite(siteId) {
  const response = await request({ url: `/qt/pages/sites/${siteId}/deploy`, method: 'post' })
  return mapDeployment(response.data || {})
}

export async function getPageDeployments(siteId, params = {}) {
  const pageNum = Math.max(Number(params.pageNum) || 1, 1)
  const pageSize = Math.min(Math.max(Number(params.pageSize) || 20, 1), 100)
  const response = await request({
    url: `/qt/pages/sites/${siteId}/deployments`,
    method: 'get',
    params: { pageNum, pageSize },
  })
  return { rows: (response.rows || []).map(mapDeployment), total: Number(response.total || 0) }
}

export async function getPageDeployment(siteId, deploymentId) {
  const response = await request({
    url: `/qt/pages/sites/${siteId}/deployments/${deploymentId}`,
    method: 'get',
  })
  return mapDeployment(response.data || {})
}

export async function rollbackPageSite(siteId, deploymentId) {
  const response = await request({
    url: `/qt/pages/sites/${siteId}/rollback/${deploymentId}`,
    method: 'post',
  })
  return mapDeployment(response.data || {})
}
