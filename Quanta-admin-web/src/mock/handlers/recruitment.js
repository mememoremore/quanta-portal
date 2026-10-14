import { findAccountByToken } from '../data/accounts'
import {
  findMockApplication,
  mockRecruitmentApplications,
  recomputeAdvancement,
} from '../data/recruitment'

function accountOf(config) {
  const authorization = config.headers?.Authorization || config.headers?.authorization || ''
  return findAccountByToken(authorization)
}

function unauthorized() {
  return { code: 401, msg: '登录状态已失效' }
}

function forbidden(message = '没有该操作权限') {
  return { code: 403, msg: message }
}

function hasPermission(account, permission) {
  return account?.permissions?.includes('*:*:*') || account?.permissions?.includes(permission)
}

function isCeo(account) {
  return account?.roles?.some((role) => role === 'ceo' || role === 'admin')
}

function isManagement(account) {
  return account?.roles?.includes('qt_mgmt') || isCeo(account)
}

function isManager(account) {
  return account?.roles?.includes('qt_manager')
}

function departmentOf(account) {
  return account?.user?.deptCode || account?.user?.dept?.deptCode || ''
}

function allowedDepartment(account, department) {
  return isCeo(account) || (department && department === departmentOf(account))
}

function visibleApplication(account, application, roundId) {
  if (isCeo(account)) return true
  const department = departmentOf(account)
  return application.tracks.some((track) => {
    if (track.department !== department) return false
    return Number(roundId) === 2 ? track.rounds[2].advanced : true
  })
}

function requireApplicationAccess(account, application, roundId = 1, department = '') {
  if (!application) return { code: 404, msg: '候选人不存在' }
  if (department && !allowedDepartment(account, department)) {
    return forbidden('无权操作其他部门的招新数据')
  }
  if (!visibleApplication(account, application, roundId)) {
    return forbidden('无权查看其他部门的候选人')
  }
  return null
}

function serializeApplication(application, roundId = 1) {
  const first = application.tracks.find((track) => track.choiceOrder === 1)
  const second = application.tracks.find((track) => track.choiceOrder === 2)
  const statusAt = (track, round) => track?.rounds?.[round]?.status || 'PENDING'
  const { appliedAt, applicationStatus, phone, ...rest } = application
  delete rest.tracks
  return {
    ...rest,
    phonenumber: phone,
    createTime: appliedAt,
    applyStatus: applicationStatus,
    firstChoice: first?.department,
    secondChoice: second?.department,
    firstChoiceStatus: statusAt(first, roundId),
    secondChoiceStatus: statusAt(second, roundId),
    firstChoiceFirstRoundStatus: statusAt(first, 1),
    secondChoiceFirstRoundStatus: statusAt(second, 1),
    firstChoiceSecondRoundStatus: statusAt(first, 2),
    secondChoiceSecondRoundStatus: statusAt(second, 2),
    firstRoundInterviewTime:
      application.firstRoundInterviewTime ||
      first?.rounds?.[1]?.interviewTime ||
      second?.rounds?.[1]?.interviewTime ||
      '',
  }
}

function statistics(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:list')) return forbidden()
  const visible = mockRecruitmentApplications.filter((application) =>
    visibleApplication(account, application, 1),
  )
  const inScope = (track) => isCeo(account) || track.department === departmentOf(account)
  return {
    code: 200,
    data: {
      submissions: visible.length,
      firstPassed: visible.filter((application) =>
        application.tracks.some((track) => inScope(track) && track.rounds[2].advanced),
      ).length,
      secondPassed: visible.filter((application) =>
        application.tracks.some(
          (track) =>
            inScope(track) && track.rounds[2].advanced && track.rounds[2].status === 'PASS',
        ),
      ).length,
      joined: visible.filter((application) => application.applicationStatus === 'JOINED').length,
    },
  }
}

function listApplications(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:list')) return forbidden()
  const params = config.params || {}
  const roundId = Number(params.roundId) || 1
  const keyword = String(params.keyword || '').trim().toLowerCase()
  const department = String(params.department || '').trim()
  const pageNum = Math.max(1, Number(params.pageNum) || 1)
  const pageSize = Math.max(1, Number(params.pageSize) || 20)
  const rows = mockRecruitmentApplications.filter((application) => {
    if (!visibleApplication(account, application, roundId)) return false
    if (roundId === 2 && !application.tracks.some((track) => track.rounds[2].advanced)) return false
    if (department && !application.tracks.some((track) => track.department === department)) return false
    if (keyword && !`${application.realName}${application.studentNo}`.toLowerCase().includes(keyword)) {
      return false
    }
    return true
  })
  const start = (pageNum - 1) * pageSize
  return {
    code: 200,
    rows: rows.slice(start, start + pageSize).map((item) => serializeApplication(item, roundId)),
    total: rows.length,
  }
}

function applicationDetail(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:list')) return forbidden()
  const applicationId = config.path.match(/^\/qt\/interview\/admin\/applications\/(\d+)$/)?.[1]
  const application = findMockApplication(applicationId)
  const denied = requireApplicationAccess(account, application)
  if (denied) return denied
  const {
    realName,
    studentNo,
    major,
    className,
    email,
    phone,
    selfIntro,
    codingExperienceDesc,
    quantaUnderstanding,
    ...applicationFields
  } = application
  return {
    code: 200,
    data: {
      application: serializeApplication(applicationFields),
      profile: {
        realName,
        studentNo,
        major,
        className,
        email,
        phone,
        selfIntro,
        codingExperienceDesc,
        quantaUnderstanding,
      },
    },
  }
}

function listEvaluations(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:list')) return forbidden()
  const { applicationId, roundId = 1, department } = config.params || {}
  const application = findMockApplication(applicationId)
  const denied = requireApplicationAccess(account, application, roundId, department)
  if (denied) return denied
  const track = application.tracks.find((item) => item.department === department)
  return { code: 200, data: track?.rounds?.[roundId]?.evaluations || [] }
}

function saveEvaluation(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:evaluate') || !isManager(account)) {
    return forbidden()
  }
  const { applicationId, roundId = 1, department, content } = config.data || {}
  if (Number(roundId) !== 2) return forbidden('经理层仅可编辑本部门二面面评')
  const application = findMockApplication(applicationId)
  const denied = requireApplicationAccess(account, application, roundId, department)
  if (denied) return denied
  if (!String(content || '').trim()) return { code: 400, msg: '请输入面评内容' }
  const track = application.tracks.find((item) => item.department === department)
  const evaluations = track?.rounds?.[roundId]?.evaluations
  if (!evaluations) return { code: 400, msg: '评审轨道不存在' }
  const existing = evaluations.find((item) => item.interviewerId === account.user.userId)
  if (existing) existing.content = String(content).trim()
  else {
    evaluations.push({
      evaluationId: Date.now(),
      interviewerId: account.user.userId,
      interviewerName: account.user.nickName,
      content: String(content).trim(),
    })
  }
  return { code: 200, msg: '面评已保存', data: evaluations }
}

function saveResult(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:evaluate') || !isManagement(account)) {
    return forbidden()
  }
  const { applicationId, roundId = 1, department, resultStatus } = config.data || {}
  const application = findMockApplication(applicationId)
  const denied = requireApplicationAccess(account, application, roundId, department)
  if (denied) return denied
  if (!['PASS', 'FAIL'].includes(resultStatus)) {
    return { code: 400, msg: '评定状态不正确' }
  }
  const track = application.tracks.find((item) => item.department === department)
  if (!track) return { code: 400, msg: '评审轨道不存在' }
  if (Number(roundId) === 2 && !track.rounds[2].advanced) {
    return { code: 409, msg: '该志愿未进入二面' }
  }
  track.rounds[roundId].status = resultStatus
  recomputeAdvancement(application)
  return { code: 200, msg: '评定结果已更新', data: { emailSent: true } }
}

function sendOffer(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:offer') || !isManagement(account)) return forbidden()
  const { applicationId, department, decision, content, notice } = config.data || {}
  const application = findMockApplication(applicationId)
  const denied = requireApplicationAccess(account, application, 2, department)
  if (denied) return denied
  if (!['PASS', 'OUT', 'FAIL'].includes(decision)) return { code: 400, msg: '录用结果不正确' }
  if (!String(notice || content || '').trim()) return { code: 400, msg: '请输入通知内容' }
  const track = application.tracks.find(
    (item) => item.department === department && item.rounds[2].advanced,
  )
  if (!track) return { code: 400, msg: '录用轨道不存在' }
  track.rounds[2].status = decision === 'OUT' ? 'FAIL' : decision
  recomputeAdvancement(application)
  return {
    code: 200,
    msg: decision === 'PASS' ? '录用通知已发送' : '淘汰通知已发送',
    data: { emailSent: true },
  }
}

function saveInterviewTime(config) {
  const account = accountOf(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:interview:admin:list')) return forbidden()
  const matched = String(config.path || '').match(
    /^\/qt\/interview\/admin\/applications\/(\d+)\/rounds\/(\d+)\/interview-time$/,
  )
  const application = findMockApplication(matched?.[1])
  const denied = requireApplicationAccess(account, application, Number(matched?.[2]) || 1)
  if (denied) return denied
  if (Number(matched?.[2]) !== 1) return { code: 400, msg: '仅支持设置一面面试时间' }
  const interviewTime = String(config.data?.interviewTime || '').trim()
  if (!['2026-09-22 18:30:00', '2026-09-23 18:30:00'].includes(interviewTime)) {
    return { code: 400, msg: '请选择有效的一面面试时间' }
  }
  application.firstRoundInterviewTime = interviewTime
  application.tracks.forEach((track) => {
    if (track.rounds?.[1]) track.rounds[1].interviewTime = interviewTime
  })
  return { code: 200, msg: '操作成功', data: { applicationId: application.applicationId, roundNo: 1, interviewTime } }
}

export const recruitmentHandlers = [
  { method: 'get', path: '/qt/interview/admin/statistics', handle: statistics },
  { method: 'get', path: '/qt/interview/admin/applications', handle: listApplications },
  {
    method: 'get',
    match: (path) => /^\/qt\/interview\/admin\/applications\/\d+$/.test(path),
    handle: applicationDetail,
  },
  { method: 'get', path: '/qt/interview/admin/evaluations', handle: listEvaluations },
  { method: 'post', path: '/qt/interview/admin/evaluations', handle: saveEvaluation },
  { method: 'post', path: '/qt/interview/result', handle: saveResult },
  { method: 'post', path: '/qt/interview/admin/offers', handle: sendOffer },
  {
    method: 'get',
    path: '/qt/interview/admin/applications/export',
    handle(config) {
      const account = accountOf(config)
      if (!account) return unauthorized()
      if (!hasPermission(account, 'qt:interview:admin:export')) return forbidden()
      return { code: 200, msg: '导出任务已创建' }
    },
  },
  {
    method: 'post',
    path: '/qt/interview/admin/applications/export',
    handle(config) {
      const account = accountOf(config)
      if (!account) return unauthorized()
      if (!hasPermission(account, 'qt:interview:admin:export')) return forbidden()
      return {
        code: 200,
        data: { fileName: '招新名单.xlsx', content: 'Mock recruitment export' },
      }
    },
  },
  {
    method: 'put',
    match: (path) => /^\/qt\/interview\/admin\/applications\/\d+\/rounds\/\d+\/interview-time$/.test(path),
    handle: saveInterviewTime,
  },
]
