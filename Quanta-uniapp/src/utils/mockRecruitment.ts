export const APPLICATION_FORM_KEY = 'application_form'
export const APPLICATION_SUBMITTED_KEY = 'application_submitted'
export const APPLICATION_PROCESS_KEY = 'application_process'

export const departmentOptions = ['产品', '设计', '全栈(前端)', '全栈(后端)']
export const genderOptions = ['男', '女']

export const stageDefinitions = [
  { key: 'first', title: '一面', description: '基础素质评估' },
  { key: 'second', title: '二面', description: '专业素质与潜力考察' },
  { key: 'offer', title: '录用', description: '发放Offer并入职' },
]

export const recruitmentStageStatuses = [
  'locked',
  'pending',
  'scheduled',
  'passed',
  'invited',
  'accepted',
  'declined',
  'rejected',
  'offered',
]

export const processPresetOptions = [
  { key: 'submitted', label: '刚投递' },
  { key: 'firstScheduled', label: '一面通知' },
  { key: 'firstRejected', label: '一面未通过' },
  { key: 'secondInvited', label: '二面邀请' },
  { key: 'secondAccepted', label: '已接受二面' },
  { key: 'secondDeclined', label: '已拒绝二面' },
  { key: 'secondRejected', label: '二面未通过' },
  { key: 'offered', label: '已录用' },
  { key: 'dualSecondInvited', label: '双部门二面邀请' },
]

const defaultInterviewDetail = {
  interviewTime: '2026.9.15 17:00',
  interviewRoom: '教学楼E205',
  waitingRoom: '教学楼E206',
  reminder: '注意准时到场参与哦，不要与我擦肩而过，让我们来一场美好的相遇～',
}

export const departmentIcons = {
  产品: '▥',
  设计: '◇',
  '全栈(前端)': '▦',
  '全栈(后端)': '<>',
}

const legacyDepartmentMap = {
  前端: '全栈(前端)',
  后端: '全栈(后端)',
}

export const normalizeDepartmentName = (name) => legacyDepartmentMap[name] || name

const createStage = (definition, index) => ({
  ...definition,
  status: index === 0 ? 'pending' : 'locked',
  detail: {},
})

const normalizeStage = (stage, definition, index) => {
  const status = recruitmentStageStatuses.includes(stage?.status)
    ? stage.status
    : index === 0
      ? 'pending'
      : 'locked'

  return {
    ...definition,
    ...stage,
    key: definition.key,
    status,
    detail: stage?.detail && typeof stage.detail === 'object' ? stage.detail : {},
  }
}

export const createDepartment = (name) => {
  const normalizedName = normalizeDepartmentName(name)
  return {
    name: normalizedName,
    icon: departmentIcons[normalizedName] || '?',
    stages: stageDefinitions.map(createStage),
  }
}

export const normalizeDepartmentProcess = (department, fallbackName = '') => {
  const normalizedName = normalizeDepartmentName(department?.name || fallbackName)
  const cachedStages = Array.isArray(department?.stages) ? department.stages : []

  return {
    ...department,
    name: normalizedName,
    icon: departmentIcons[normalizedName] || department?.icon || '?',
    stages: stageDefinitions.map((definition, index) => {
      const cachedStage = cachedStages.find((stage) => stage?.key === definition.key)
      return normalizeStage(cachedStage, definition, index)
    }),
  }
}

const presetStatuses = {
  submitted: ['pending', 'locked', 'locked'],
  firstScheduled: ['scheduled', 'locked', 'locked'],
  firstRejected: ['rejected', 'locked', 'locked'],
  secondInvited: ['passed', 'invited', 'locked'],
  secondAccepted: ['passed', 'accepted', 'locked'],
  secondDeclined: ['passed', 'declined', 'locked'],
  secondRejected: ['passed', 'rejected', 'locked'],
  offered: ['passed', 'passed', 'offered'],
}

export const applyProcessPreset = (department, presetKey) => {
  const normalized = normalizeDepartmentProcess(department)
  const effectivePreset = presetKey === 'dualSecondInvited' ? 'secondInvited' : presetKey
  const statuses = presetStatuses[effectivePreset] || presetStatuses.submitted

  return {
    ...normalized,
    stages: normalized.stages.map((stage, index) => ({
      ...stage,
      status: statuses[index],
      detail: statuses[index] === 'scheduled' || statuses[index] === 'invited'
        ? { ...defaultInterviewDetail }
        : {},
    })),
  }
}

export const getStoredProcess = () => {
  const stored = uni.getStorageSync(APPLICATION_PROCESS_KEY)
  if (!Array.isArray(stored)) return []
  return stored.map((department) => normalizeDepartmentProcess(department))
}

export const saveStoredProcess = (process) => {
  const normalized = Array.isArray(process)
    ? process.map((department) => normalizeDepartmentProcess(department))
    : []
  uni.setStorageSync(APPLICATION_PROCESS_KEY, normalized)
  return normalized
}

export const getSubmitted = () => Boolean(uni.getStorageSync(APPLICATION_SUBMITTED_KEY))

export const saveApplication = (form) => {
  const normalizedForm = {
    ...form,
    firstChoice: normalizeDepartmentName(form.firstChoice),
    secondChoice: normalizeDepartmentName(form.secondChoice),
  }

  uni.setStorageSync(APPLICATION_FORM_KEY, normalizedForm)
  uni.setStorageSync(APPLICATION_SUBMITTED_KEY, true)

  const cached = getStoredProcess()
  const process = [normalizedForm.firstChoice, normalizedForm.secondChoice]
    .filter((name) => departmentOptions.includes(name))
    .map((name) => {
      const cachedDepartment = cached.find((item) => normalizeDepartmentName(item.name) === name)
      return cachedDepartment ? normalizeDepartmentProcess(cachedDepartment, name) : createDepartment(name)
    })

  saveStoredProcess(process)
  return process
}

export const saveDraft = (form) => {
  uni.setStorageSync(APPLICATION_FORM_KEY, {
    ...form,
    firstChoice: normalizeDepartmentName(form.firstChoice),
    secondChoice: normalizeDepartmentName(form.secondChoice),
  })
  uni.removeStorageSync(APPLICATION_SUBMITTED_KEY)
}

export const createEmptyApplication = () => ({
  photo: '',
  realName: '',
  gender: '',
  className: '',
  firstChoice: '',
  secondChoice: '',
  selfIntro: '',
  codingExperienceDesc: '',
  quantaUnderstanding: '',
})
