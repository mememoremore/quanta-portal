import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import {
  acceptOffer,
  getMyApplication,
  getMyInterviewProcess,
  getMyRecruitmentState,
  mapApplyWindow,
  mapInterviewProcess,
  recruitmentDepartments,
  submitApplication,
} from '../recruitment'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

const application = {
  applicationId: 1,
  realName: '新生小李',
  gender: '1',
  className: '软工2402',
  firstChoice: 'FRONTEND',
  secondChoice: 'BACKEND',
  photoUrl: '/profile/a.jpg',
  photoAccessUrl: 'https://www.quantacenter.com/profile/a.jpg?token=x',
  resumeUrl: '/profile/resume.pdf',
  resumeAccessUrl: 'https://www.quantacenter.com/profile/resume.pdf?token=x',
  resumeFileName: '新生小李-简历.pdf',
  applyStatus: 'PROCESSING',
}

describe('freshman recruitment API', () => {
  beforeEach(() => request.mockReset())

  it('exposes only the four active departments', () => {
    expect(recruitmentDepartments.map(({ value }) => value)).toEqual([
      'PRODUCT',
      'DESIGN',
      'FRONTEND',
      'BACKEND',
    ])
    expect(JSON.stringify(recruitmentDepartments)).not.toContain('ANDROID')
    expect(JSON.stringify(recruitmentDepartments)).not.toContain('安卓')
  })

  it('loads and maps the current application from the real endpoint', async () => {
    request.mockResolvedValue({
      data: {
        application,
        profile: { selfIntro: '你好', codingExperience: '1' },
      },
    })

    await expect(getMyApplication()).resolves.toEqual(
      expect.objectContaining({
        applicationId: 1,
        realName: '新生小李',
        gender: '女',
        firstChoice: 'FRONTEND',
        firstChoiceLabel: '全栈（前端）',
        photoUrl: 'https://www.quantacenter.com/profile/a.jpg?token=x',
        resumeUrl: 'https://www.quantacenter.com/profile/resume.pdf?token=x',
        storedResumeUrl: '/profile/resume.pdf',
        resumeFileName: '新生小李-简历.pdf',
        selfIntro: '你好',
      }),
    )
    expect(request).toHaveBeenCalledWith({ url: '/qt/interview/my', method: 'get' })
  })

  it('maps the apply window from GET /qt/interview/my', async () => {
    request.mockResolvedValue({
      data: {
        application: null,
        profile: null,
        newApplicationsOpen: false,
        canUpdate: false,
      },
    })

    await expect(getMyRecruitmentState()).resolves.toEqual({
      application: null,
      newApplicationsOpen: false,
      canUpdate: false,
      offerAcceptance: { ready: false, acceptedDepartment: null },
    })
    expect(mapApplyWindow({ application, newApplicationsOpen: false })).toEqual({
      newApplicationsOpen: false,
      canUpdate: true,
    })
  })

  it('loads interview results as read-only process data', async () => {
    request
      .mockResolvedValueOnce({ data: { application, profile: {} } })
      .mockResolvedValueOnce({
        data: [
          { roundId: 1, department: 'FRONTEND', resultStatus: 'PASS' },
          {
            roundId: 2,
            department: 'FRONTEND',
            resultStatus: 'WAITING',
            interviewTime: '2026-09-20 10:00:00',
          },
        ],
      })

    const processes = await getMyInterviewProcess()

    expect(request.mock.calls.map(([config]) => config)).toEqual([
      { url: '/qt/interview/my', method: 'get' },
      { url: '/qt/interview/myResults', method: 'get' },
    ])
    expect(processes[0]).toEqual(
      expect.objectContaining({
        departmentCode: 'FRONTEND',
        stages: expect.arrayContaining([
          expect.objectContaining({ key: 'second', status: 'invited' }),
        ]),
      }),
    )
    expect(JSON.stringify(processes)).not.toContain('interviewTime')
    expect(JSON.stringify(processes)).not.toMatch(/accept|rejectAction/i)
  })

  it('submits the browser form and photo as FormData', async () => {
    request.mockResolvedValue({ code: 200 })
    const photoFile = new File(['photo'], 'photo.jpg', { type: 'image/jpeg' })
    const resumeFile = new File(['resume'], 'resume.pdf', { type: 'application/pdf' })

    await submitApplication({
      realName: '新生小李',
      gender: '女',
      className: '软工2402',
      firstChoice: 'FRONTEND',
      secondChoice: 'BACKEND',
      selfIntro: '你好',
      codingExperience: '1',
      codingExperienceDesc: 'Vue',
      quantaUnderstanding: '开放与创造',
      photoFile,
      resumeFile,
    })

    const config = request.mock.calls[0][0]
    expect(config).toMatchObject({ url: '/qt/interview/apply', method: 'post' })
    expect(config.data).toBeInstanceOf(FormData)
    expect(config.data.get('firstChoice')).toBe('FRONTEND')
    expect(config.data.get('secondChoice')).toBe('BACKEND')
    expect(config.data.get('gender')).toBe('1')
    expect(config.data.get('photoFile')).toBe(photoFile)
    expect(config.data.get('resumeFile')).toBe(resumeFile)
  })

  it('rejects a removed or unknown department before making a request', async () => {
    await expect(
      submitApplication({
        realName: '新生小李',
        gender: '女',
        className: '软工2402',
        firstChoice: 'ANDROID',
        secondChoice: 'BACKEND',
      }),
    ).rejects.toThrow('无效的志愿部门')
    expect(request).not.toHaveBeenCalled()
  })
})

describe('interview status mapping', () => {
  it('ignores backend interview times and keeps first-round waiting results pending', () => {
    const processes = mapInterviewProcess(application, [
      {
        roundId: 1,
        department: 'FRONTEND',
        resultStatus: 'WAITING',
        interviewTime: '2026-09-22 18:30:00',
      },
    ])

    expect(processes[0].stages[0]).toMatchObject({ key: 'first', status: 'pending' })
    expect(processes[0].stages[0]).not.toHaveProperty('interviewTime')
  })

  it('keeps invitations read-only and maps only accepted offers', () => {
    const processes = mapInterviewProcess(
      { ...application, applyStatus: 'OFFERED', offeredDepartment: 'BACKEND' },
      [{ roundId: 1, department: 'BACKEND', resultStatus: 'PASS' }],
    )

    expect(processes.find(({ departmentCode }) => departmentCode === 'BACKEND').stages[2].status).toBe(
      'locked',
    )
  })

  it('keeps two PASS results separate until one department is accepted', () => {
    const results = ['FRONTEND', 'BACKEND'].flatMap((department) => [
      { roundNo: 1, department, resultStatus: 'PASS' },
      { roundNo: 2, department, resultStatus: 'PASS' },
    ])
    const pending = mapInterviewProcess(application, results, { ready: true, acceptedDepartment: null })
    expect(pending.map((item) => item.stages[1].status)).toEqual(['passed', 'passed'])
    expect(pending.map((item) => item.stages[2].status)).toEqual(['pending', 'pending'])
    const accepted = mapInterviewProcess(application, results, { ready: true, acceptedDepartment: 'BACKEND' })
    expect(accepted.map((item) => item.stages[2].status)).toEqual(['pending', 'offered'])
  })

  it('sends a department acceptance request', async () => {
    request.mockResolvedValue({ code: 200, data: { acceptedDepartment: 'FRONTEND' } })
    await acceptOffer('FRONTEND')
    expect(request).toHaveBeenCalledWith({
      url: '/qt/interview/my/accept', method: 'post', data: { department: 'FRONTEND' },
    })
  })

  it('treats OUT the same as FAIL on the timeline', () => {
    const processes = mapInterviewProcess(application, [
      { roundId: 1, department: 'FRONTEND', resultStatus: 'PASS' },
      { roundId: 2, department: 'FRONTEND', resultStatus: 'OUT' },
    ])

    expect(processes[0].stages[1].status).toBe('rejected')
  })

  it('matches rounds by roundNo when the stored roundId is not 1 or 2', () => {
    const processes = mapInterviewProcess(application, [
      { roundId: 11, roundNo: 1, department: 'FRONTEND', resultStatus: 'PASS' },
      { roundId: 12, roundNo: 2, department: 'FRONTEND', resultStatus: 'FAIL' },
    ])

    expect(processes[0].stages[0].status).toBe('passed')
    expect(processes[0].stages[1].status).toBe('rejected')
  })
})
