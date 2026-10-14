import { request } from '@/utils/request'

export const recruitmentDepartments = Object.freeze([
  { value: 'PRODUCT', label: '产品' },
  { value: 'DESIGN', label: '设计' },
  { value: 'FRONTEND', label: '全栈（前端）' },
  { value: 'BACKEND', label: '全栈（后端）' },
])

const departmentLabels = Object.fromEntries(
  recruitmentDepartments.map(({ value, label }) => [value, label]),
)
const departmentCodes = new Map(
  recruitmentDepartments.flatMap(({ value, label }) => [
    [value, value],
    [label, value],
    [label.replaceAll('（', '(').replaceAll('）', ')'), value],
  ]),
)
const genderCodes = new Map([
  ['0', '0'],
  ['1', '1'],
  ['男', '0'],
  ['女', '1'],
])
const genderLabels = { 0: '男', 1: '女' }

function requireDepartment(value) {
  const code = departmentCodes.get(value)
  if (!code) throw new Error('无效的志愿部门')
  return code
}

function requireGender(value) {
  const code = genderCodes.get(String(value ?? ''))
  if (!code) throw new Error('无效的性别')
  return code
}

export function mapApplication(data) {
  const application = data?.application
  if (!application) return null
  const profile = data.profile || {}
  return {
    applicationId: Number(application.applicationId),
    realName: application.realName || '',
    gender: genderLabels[application.gender] || '',
    className: application.className || '',
    firstChoice: application.firstChoice || '',
    firstChoiceLabel: departmentLabels[application.firstChoice] || application.firstChoice || '',
    secondChoice: application.secondChoice || '',
    secondChoiceLabel: departmentLabels[application.secondChoice] || application.secondChoice || '',
    photoUrl: application.photoAccessUrl || application.photoUrl || '',
    storedPhotoUrl: application.photoUrl || '',
    resumeUrl: application.resumeAccessUrl || application.resumeUrl || '',
    storedResumeUrl: application.resumeUrl || '',
    resumeFileName: application.resumeFileName || '',
    applyStatus: application.applyStatus || '',
    offeredDepartment: application.offeredDepartment || '',
    joinStatus: application.joinStatus || '',
    selfIntro: profile.selfIntro || '',
    codingExperience: profile.codingExperience || '0',
    codingExperienceDesc: profile.codingExperienceDesc || '',
    quantaUnderstanding: profile.quantaUnderstanding || '',
  }
}

function resultRoundNo(result) {
  const value = result?.roundNo ?? result?.roundId
  return Number(value)
}

function resultStageStatus(result, round, unlocked) {
  if (!result) return unlocked ? 'pending' : 'locked'
  if (result.resultStatus === 'FAIL' || result.resultStatus === 'OUT') return 'rejected'
  if (result.resultStatus === 'PASS') return 'passed'
  if (result.resultStatus === 'WAITING') {
    if (round === 2) return 'invited'
    return 'pending'
  }
  return 'pending'
}

function mapResultStage(key, title, status, result) {
  return {
    key,
    title,
    status,
    feedback: result?.feedback || '',
  }
}

export function mapInterviewProcess(application, results = [], offerAcceptance = null) {
  if (!application) return []
  const choices = [...new Set([application.firstChoice, application.secondChoice])].filter(
    (code) => departmentLabels[code],
  )

  return choices.map((departmentCode) => {
    const first = results.find(
      (item) => item.department === departmentCode && resultRoundNo(item) === 1,
    )
    const second = results.find(
      (item) => item.department === departmentCode && resultRoundNo(item) === 2,
    )
    const firstStatus = resultStageStatus(first, 1, true)
    const secondStatus = resultStageStatus(second, 2, firstStatus === 'passed')
    const finalStatus = offerAcceptance?.acceptedDepartment === departmentCode
      ? 'offered'
      : secondStatus === 'passed'
        ? 'pending'
        : secondStatus === 'rejected'
          ? 'rejected'
          : 'locked'

    return {
      departmentCode,
      departmentName: departmentLabels[departmentCode],
      stages: [
        mapResultStage('first', '一面', firstStatus, first),
        mapResultStage('second', '二面', secondStatus, second),
        mapResultStage('offer', '录用', finalStatus),
      ],
    }
  })
}

async function getMyApplicationBundle() {
  const response = await request({ url: '/qt/interview/my', method: 'get' })
  return response.data || { application: null, profile: null }
}

export function mapApplyWindow(data) {
  const newApplicationsOpen = data?.newApplicationsOpen === true
  const hasApplication = Boolean(data?.application)
  return {
    newApplicationsOpen,
    canUpdate: data?.canUpdate === true || newApplicationsOpen || hasApplication,
  }
}

export async function getMyApplication() {
  return mapApplication(await getMyApplicationBundle())
}

export async function getMyRecruitmentState() {
  const bundle = await getMyApplicationBundle()
  return {
    application: mapApplication(bundle),
    offerAcceptance: bundle.offerAcceptance || { ready: false, acceptedDepartment: null },
    ...mapApplyWindow(bundle),
  }
}

export async function getMyInterviewProcess() {
  const [bundle, resultResponse] = await Promise.all([
    getMyApplicationBundle(),
    request({ url: '/qt/interview/myResults', method: 'get' }),
  ])
  return mapInterviewProcess(bundle.application, resultResponse.data || [], bundle.offerAcceptance)
}

export function acceptOffer(department) {
  return request({
    url: '/qt/interview/my/accept',
    method: 'post',
    data: { department: requireDepartment(department) },
  })
}

export function toApplicationPayload(form) {
  const firstChoice = requireDepartment(form.firstChoice)
  const secondChoice = requireDepartment(form.secondChoice)
  if (firstChoice === secondChoice) throw new Error('两个志愿不能相同')

  return {
    realName: form.realName || '',
    gender: requireGender(form.gender),
    className: form.className || '',
    firstChoice,
    secondChoice,
    selfIntro: form.selfIntro || '',
    codingExperience:
      form.codingExperience || (String(form.codingExperienceDesc || '').trim() ? '1' : '0'),
    codingExperienceDesc: form.codingExperienceDesc || '',
    quantaUnderstanding: form.quantaUnderstanding || '',
    ...(form.storedPhotoUrl || form.photoUrl
      ? { photoUrl: form.storedPhotoUrl || form.photoUrl }
      : {}),
    ...(form.storedResumeUrl || form.resumeUrl
      ? { resumeUrl: form.storedResumeUrl || form.resumeUrl }
      : {}),
  }
}

export async function submitApplication(form) {
  const data = new FormData()
  Object.entries(toApplicationPayload(form)).forEach(([key, value]) => {
    data.append(key, value ?? '')
  })
  if (typeof File !== 'undefined' && form.photoFile instanceof File) {
    data.append('photoFile', form.photoFile)
  }
  if (typeof File !== 'undefined' && form.resumeFile instanceof File) {
    data.append('resumeFile', form.resumeFile)
  }
  return request({ url: '/qt/interview/apply', method: 'post', data })
}

/**
 * 塔员只读查看全部新生报名信息（仅基本信息，已剥离电话/照片等敏感字段）
 */
export async function getMemberApplications(params) {
  const response = await request({ url: '/qt/interview/member/applications', method: 'get', params })
  return response.data || []
}
