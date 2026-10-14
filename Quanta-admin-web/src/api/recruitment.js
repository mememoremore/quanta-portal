import { request } from '@/utils/request'
import { requestBlob } from '@/utils/download'

export const departmentLabels = {
  BACKEND: '后端部',
  PRODUCT: '产品部',
  DESIGN: '设计部',
  FRONTEND: '前端部',
}

export const resultLabels = {
  PENDING: '待评',
  PASS: 'Pass',
  OUT: 'Out',
}

function normalizeResultStatus(status) {
  return status === 'FAIL' ? 'OUT' : status || 'PENDING'
}

function normalizeTrack(track = {}) {
  const rounds = Object.fromEntries(
    Object.entries(track.rounds || {}).map(([round, value]) => [
      round,
      { ...value, status: normalizeResultStatus(value?.status) },
    ]),
  )
  if (rounds[2]) rounds[2].advanced = rounds[2].advanced ?? rounds[1]?.status === 'PASS'
  return {
    ...track,
    label: departmentLabels[track.department] || track.department,
    rounds,
  }
}

function buildChoice(application, order) {
  const prefix = order === 1 ? 'firstChoice' : 'secondChoice'
  const department = application[prefix]
  if (!department) return null
  const status = application[`${prefix}Status`]
  const firstRoundStatus = application[`${prefix}FirstRoundStatus`] ?? status
  const secondRoundStatus = application[`${prefix}SecondRoundStatus`] ?? status
  return {
    choiceOrder: order,
    department,
    label: departmentLabels[department] || department,
    rounds: {
      1: { status: normalizeResultStatus(firstRoundStatus), evaluations: [] },
      2: {
        status: normalizeResultStatus(secondRoundStatus),
        score: application[`${prefix}SecondRoundScore`] ?? null,
        updatedBy: application[`${prefix}SecondRoundScoreUpdateBy`] || '',
        updatedTime: application[`${prefix}SecondRoundScoreUpdateTime`] || '',
        advanced: false,
        evaluations: [],
      },
    },
  }
}

function applyDefaultAdvancement(choices) {
  const first = choices.find((item) => item.choiceOrder === 1)
  const second = choices.find((item) => item.choiceOrder === 2)
  if (first?.rounds?.[1]?.status === 'PASS') first.rounds[2].advanced = true
  if (second?.rounds?.[1]?.status === 'PASS') second.rounds[2].advanced = true
  return choices
}

export function mapApplication(application = {}) {
  const choices = application.tracks?.length
    ? application.tracks.map(normalizeTrack)
    : applyDefaultAdvancement([buildChoice(application, 1), buildChoice(application, 2)].filter(Boolean))
  return {
    ...application,
    id: application.applicationId,
    name: application.realName || '-',
    phone: application.phonenumber || application.phone || '-',
    resumeUrl: application.resumeAccessUrl || application.resumeUrl || '',
    resumeFileName: application.resumeFileName || '',
    appliedAt: application.createTime || application.appliedAt || '-',
    applicationStatus: application.applyStatus || application.applicationStatus,
    noticeStatuses: application.noticeStatuses || {},
    choices,
  }
}

export function mergeApplicationDetail(payload = {}) {
  return mapApplication({ ...(payload.application || payload), ...(payload.profile || {}) })
}

export function createOfferPayload(data = {}) {
  return {
    applicationId: data.applicationId,
    department: data.department,
    decision: data.decision === 'FAIL' ? 'OUT' : data.decision,
    notice: data.notice ?? data.content,
    roundId: data.roundId ?? 2,
  }
}

export async function getRecruitmentStatistics() {
  const response = await request({ url: '/qt/interview/admin/statistics', method: 'get' })
  const data = response.data || {}
  return {
    ...data,
    submissions: data.submissions ?? data.submittedCount,
    firstPassed: data.firstPassed ?? data.processingCount,
    secondPassed: data.secondPassed ?? data.offeredCount,
    joined: data.joined ?? data.joinedCount,
  }
}

export async function getRecruitmentApplications(query = {}) {
  const response = await request({
    url: '/qt/interview/admin/applications',
    method: 'get',
    params: query,
  })
  return {
    rows: (response.rows || []).map(mapApplication),
    total: Number(response.total) || 0,
  }
}

export async function getRecruitmentApplication(applicationId) {
  const response = await request({
    url: `/qt/interview/admin/applications/${applicationId}`,
    method: 'get',
  })
  return mergeApplicationDetail(response.data || {})
}

export function mapEvaluation(item = {}) {
  const evaluatorUserId = item.evaluatorUserId ?? item.interviewerId
  const evaluatorUserName = item.evaluatorUserName || item.interviewerName || ''
  return {
    ...item,
    evaluatorUserId,
    evaluatorUserName,
    evaluatorName: item.evaluatorName || evaluatorUserName || '-',
    interviewerId: evaluatorUserId,
    interviewerName: evaluatorUserName,
  }
}

export function normalizeEvaluation(item = {}) {
  return mapEvaluation(item)
}

export async function getEvaluations(params) {
  const response = await request({
    url: '/qt/interview/admin/evaluations',
    method: 'get',
    params,
  })
  const rows = Array.isArray(response.data) ? response.data : response.data?.rows || response.rows || []
  return rows.map(normalizeEvaluation)
}

export function saveEvaluation(data) {
  return request({ url: '/qt/interview/admin/evaluations', method: 'post', data })
}

export function saveInterviewResult(data) {
  return saveInterviewDecision(data.applicationId, data.roundId, data.department, data.resultStatus)
}

export function saveInterviewDecision(applicationId, roundNo, department, decision) {
  return request({
    url: `/qt/interview/admin/applications/${applicationId}/rounds/${roundNo}/departments/${department}/decision`,
    method: 'put',
    data: { decision: normalizeResultStatus(decision) },
  })
}

export function saveInterviewScore(applicationId, department, score) {
  return request({
    url: `/qt/interview/admin/applications/${applicationId}/round2/departments/${department}/score`,
    method: 'put',
    data: { score },
  })
}

export async function getNoticePreview(applicationId, department) {
  const response = await request({
    url: `/qt/interview/admin/applications/${applicationId}/notice-preview`,
    method: 'get',
    params: { department },
  })
  return response.data || {}
}

export function sendResultNotice(applicationId, department, qrCode) {
  const data = new FormData()
  data.append('department', department)
  if (qrCode) data.append('qrCode', qrCode)
  return request({
    url: `/qt/interview/admin/applications/${applicationId}/notice`,
    method: 'post',
    data,
  })
}

export function sendOffer(data) {
  return request({
    url: '/qt/interview/admin/offers',
    method: 'post',
    data: createOfferPayload(data),
  })
}

export function exportRecruitmentList(params) {
  return requestBlob(
    {
      url: '/qt/interview/admin/applications/export',
      method: 'post',
      params,
    },
    '招新名单.xlsx',
  )
}
