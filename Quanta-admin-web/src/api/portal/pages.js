import { request } from '@/utils/request'

// 与后端 QtPageDeployment 的非终态集合保持一致
export const PAGES_ACTIVE_STATUSES = ['PENDING', 'RESOLVING', 'DOWNLOADING', 'VALIDATING', 'PUBLISHING']

// 官方模板仓库：门户一键创建时固定指向它；仓库公开且启用了 GitHub Template
export const TEMPLATE_REPO_URL = 'https://github.com/subingbo/quanta-pages-template'
export const TEMPLATE_BRANCH = 'main'

// 模板目录与仓库 templates/<key> 一一对应；classic 也在 2026-10-27 统一迁入 templates/。
export const PAGE_TEMPLATES = [
  {
    key: 'classic',
    name: '经典浅色',
    description: '白底、橙色强调，圆角卡片，适合技术笔记与日常记录',
    publishDir: '/templates/classic',
    accent: '#f97316',
    preview: 'cards',
  },
  {
    key: 'paper',
    name: '纸感文字',
    description: '米白纸底、衬线标题、窄栏编号文章，适合长文写作',
    publishDir: '/templates/paper',
    accent: '#9a3b2c',
    preview: 'lines',
  },
  {
    key: 'magazine',
    name: '杂志作品集',
    description: '超大姓名、粗边框卡片与分类色块，适合项目展示',
    publishDir: '/templates/magazine',
    accent: '#e11d48',
    preview: 'grid',
  },
  {
    key: 'terminal',
    name: '终端深色',
    description: '等宽字体、深色终端界面，适合极客风格内容',
    publishDir: '/templates/terminal',
    accent: '#3ddc84',
    preview: 'terminal',
  },
]

export const DEFAULT_TEMPLATE_KEY = 'classic'

export function isDeploymentActive(status) {
  return PAGES_ACTIVE_STATUSES.includes(status)
}

export function isTemplateRepo(url) {
  const normalized = String(url || '').trim().replace(/\/$/, '').replace(/\.git$/, '').toLowerCase()
  return normalized === TEMPLATE_REPO_URL.toLowerCase()
}

export function getPageTemplate(key) {
  return PAGE_TEMPLATES.find((template) => template.key === key) || PAGE_TEMPLATES[0]
}

function normalizePublishDir(value) {
  const raw = String(value || '').trim()
  if (!raw || raw === '/') return '/'
  const withLeading = raw.startsWith('/') ? raw : '/' + raw
  return withLeading.replace(/\/+$/, '') || '/'
}

export function findTemplateByPublishDir(publishDir) {
  const normalized = normalizePublishDir(publishDir)
  const matched = PAGE_TEMPLATES.find((template) => template.publishDir === normalized)
  if (matched) return matched
  // 兼容迁移前创建的站点：publishDir='/' 时仓库根 index.html 会跳转到 classic。
  if (normalized === '/') return { ...PAGE_TEMPLATES[0], publishDir: '/', legacy: true }
  return null
}

export function templateSiteOf(site) {
  if (!site || !isTemplateRepo(site.repoUrl)) return null
  return findTemplateByPublishDir(site.publishDir)
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
