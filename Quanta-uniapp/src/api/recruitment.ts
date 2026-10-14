import { API_BASE_URL } from '../config/runtime'
import { TOKEN_KEY } from '../utils/storage'
import request, { handleUnauthorized } from '../utils/request'
import {
  createDepartment,
  normalizeDepartmentProcess,
} from '../utils/mockRecruitment'
import type { AjaxResponse } from './contracts'
import { resolveApiAssetUrl } from './mappers'

export interface RecruitmentForm {
  photo: string
  photoUrl?: string
  realName: string
  gender: string
  className: string
  firstChoice: string
  secondChoice: string
  selfIntro: string
  codingExperience: string
  codingExperienceDesc: string
  quantaUnderstanding: string
}

interface InterviewApplicationDto {
  applicationId: number
  realName: string
  gender: string
  className: string
  firstChoice: string
  secondChoice: string
  photoUrl: string
  photoAccessUrl?: string
  applyStatus: string
  offeredDepartment?: string
  joinStatus?: string
}

interface InterviewProfileDto {
  selfIntro?: string
  codingExperience?: string
  codingExperienceDesc?: string
  quantaUnderstanding?: string
}

export interface InterviewResultDto {
  roundId: number
  roundNo?: number
  roundName?: string
  department: string
  resultStatus: 'PENDING' | 'PASS' | 'FAIL' | 'WAITING' | 'OUT'
  interviewTime?: string
  feedback?: string
}

interface MyApplicationData {
  application: InterviewApplicationDto | null
  profile: InterviewProfileDto | null
  newApplicationsOpen?: boolean
  canUpdate?: boolean
}

const DEPARTMENT_CODES: Record<string, string> = {
  产品: 'PRODUCT',
  产品部: 'PRODUCT',
  设计: 'DESIGN',
  设计部: 'DESIGN',
  '全栈(前端)': 'FRONTEND',
  前端: 'FRONTEND',
  '全栈(后端)': 'BACKEND',
  后端: 'BACKEND',
  安卓: 'ANDROID',
  安卓组: 'ANDROID',
}

const DEPARTMENT_NAMES: Record<string, string> = {
  PRODUCT: '产品',
  DESIGN: '设计',
  FRONTEND: '全栈(前端)',
  BACKEND: '全栈(后端)',
  ANDROID: '安卓',
}

const GENDER_CODES: Record<string, string> = { 男: '0', 女: '1', 未知: '2' }
const GENDER_NAMES: Record<string, string> = { '0': '男', '1': '女', '2': '未知' }

export const toDepartmentCode = (value: string) => DEPARTMENT_CODES[value] || value
export const fromDepartmentCode = (value: string) => DEPARTMENT_NAMES[value] || value
export const toGenderCode = (value: string) => GENDER_CODES[value] || value
export const fromGenderCode = (value: string) => GENDER_NAMES[value] || value

export const mapApplication = (data: MyApplicationData): RecruitmentForm | null => {
  if (!data.application) return null
  const application = data.application
  const profile = data.profile || {}
  return {
    photo: resolveApiAssetUrl(application.photoAccessUrl || application.photoUrl || ''),
    photoUrl: application.photoUrl || '',
    realName: application.realName || '',
    gender: fromGenderCode(application.gender),
    className: application.className || '',
    firstChoice: fromDepartmentCode(application.firstChoice),
    secondChoice: fromDepartmentCode(application.secondChoice),
    selfIntro: profile.selfIntro || '',
    codingExperience: profile.codingExperience || '0',
    codingExperienceDesc: profile.codingExperienceDesc || '',
    quantaUnderstanding: profile.quantaUnderstanding || '',
  }
}

export const mapApplyWindow = (data?: MyApplicationData | null) => {
  const newApplicationsOpen = data?.newApplicationsOpen === true
  const hasApplication = Boolean(data?.application)
  return {
    newApplicationsOpen,
    canUpdate: data?.canUpdate === true || newApplicationsOpen || hasApplication,
  }
}

const scheduleDetail = (result?: InterviewResultDto) => result?.interviewTime
  ? { interviewTime: result.interviewTime, reminder: result.feedback || '' }
  : {}

const resultStatus = (result: InterviewResultDto | undefined, round: number, unlocked: boolean) => {
  if (!result) return unlocked ? 'pending' : 'locked'
  if (result.resultStatus === 'FAIL' || result.resultStatus === 'OUT') return 'rejected'
  if (result.resultStatus === 'PASS') return 'passed'
  if (result.resultStatus === 'WAITING') return round === 2 ? 'invited' : result.interviewTime ? 'scheduled' : 'pending'
  if (result.resultStatus === 'PENDING' && round === 1 && result.interviewTime) return 'scheduled'
  return 'pending'
}

const resultRoundNo = (result: InterviewResultDto) => Number(result.roundNo ?? result.roundId)

export const mapInterviewProcess = (
  application: InterviewApplicationDto | null,
  results: InterviewResultDto[],
) => {
  if (!application) return []
  const choices = [...new Set([application.firstChoice, application.secondChoice].filter(Boolean))]
  return choices.map((code) => {
    const department = normalizeDepartmentProcess(createDepartment(fromDepartmentCode(code)))
    const first = results.find((item) => item.department === code && resultRoundNo(item) === 1)
    const second = results.find((item) => item.department === code && resultRoundNo(item) === 2)
    const firstStatus = resultStatus(first, 1, true)
    const secondStatus = resultStatus(second, 2, firstStatus === 'passed')
    department.stages[0] = { ...department.stages[0], status: firstStatus, detail: scheduleDetail(first) }
    department.stages[1] = { ...department.stages[1], status: secondStatus, detail: scheduleDetail(second) }
    department.stages[2] = {
      ...department.stages[2],
      status: application.applyStatus === 'OFFERED' && (!application.offeredDepartment || application.offeredDepartment === code)
        ? 'offered'
        : application.applyStatus === 'REJECTED'
          ? 'rejected'
          : secondStatus === 'passed' ? 'pending' : 'locked',
      detail: {},
    }
    return department
  })
}

export const getMyApplicationBundle = async () => {
  const response = await request<AjaxResponse<MyApplicationData>>({ url: '/qt/interview/my' })
  return response.data || { application: null, profile: null }
}

export const getMyApplication = async () => mapApplication(await getMyApplicationBundle())

export const getMyRecruitmentState = async () => {
  const bundle = await getMyApplicationBundle()
  return {
    application: mapApplication(bundle),
    ...mapApplyWindow(bundle),
  }
}

export const getMyInterviewProcess = async () => {
  const [bundle, resultResponse] = await Promise.all([
    getMyApplicationBundle(),
    request<AjaxResponse<InterviewResultDto[]>>({ url: '/qt/interview/myResults' }),
  ])
  return mapInterviewProcess(bundle.application, resultResponse.data || [])
}

const formDataFor = (form: RecruitmentForm) => ({
  realName: form.realName,
  gender: toGenderCode(form.gender),
  className: form.className,
  firstChoice: toDepartmentCode(form.firstChoice),
  secondChoice: toDepartmentCode(form.secondChoice),
  selfIntro: form.selfIntro,
  codingExperience: form.codingExperience || (form.codingExperienceDesc.trim() ? '1' : '0'),
  codingExperienceDesc: form.codingExperienceDesc,
  quantaUnderstanding: form.quantaUnderstanding,
  ...(form.photoUrl ? { photoUrl: form.photoUrl } : {}),
})

const isRemotePhoto = (value: string) => /^https?:\/\//.test(value) && !/^https?:\/\/(tmp|localhost\/tmp)/.test(value)

export const submitApplication = async (form: RecruitmentForm) => {
  const data = formDataFor(form)
  if (!form.photo || isRemotePhoto(form.photo)) {
    return request<AjaxResponse>({
      url: '/qt/interview/apply',
      method: 'POST',
      data,
      header: { 'Content-Type': 'application/x-www-form-urlencoded' },
    })
  }

  return new Promise<AjaxResponse>((resolve, reject) => {
    const token = uni.getStorageSync(TOKEN_KEY)
    uni.uploadFile({
      url: `${API_BASE_URL}/qt/interview/apply`,
      filePath: form.photo,
      name: 'photoFile',
      timeout: 10000,
      formData: data,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (response) => {
        let body: AjaxResponse
        try { body = JSON.parse(response.data) } catch { reject(new Error('投递响应格式异常')); return }
        if (response.statusCode === 401 || body.code === 401) {
          handleUnauthorized()
          reject(new Error('登录已失效'))
          return
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 200) {
          reject(new Error(body.msg || '投递失败'))
          return
        }
        resolve(body)
      },
      fail: (error) => reject(new Error(error.errMsg || '证件照上传失败')),
    })
  })
}
