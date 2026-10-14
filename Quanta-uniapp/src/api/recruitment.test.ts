import { describe, expect, it } from 'vitest'
import { mapApplication, mapApplyWindow, mapInterviewProcess, toDepartmentCode, toGenderCode } from './recruitment'

describe('recruitment backend mapping', () => {
  it('maps form enum values', () => {
    expect(toDepartmentCode('全栈(前端)')).toBe('FRONTEND')
    expect(toGenderCode('女')).toBe('1')
  })

  it('maps an existing application', () => {
    expect(mapApplication({
      application: { applicationId: 1, realName: '李同学', gender: '1', className: '软工2402', firstChoice: 'FRONTEND', secondChoice: 'BACKEND', photoUrl: '/profile/a.jpg', applyStatus: 'SUBMITTED' },
      profile: { selfIntro: '你好', codingExperience: '1' },
    })).toMatchObject({ gender: '女', firstChoice: '全栈(前端)', secondChoice: '全栈(后端)', selfIntro: '你好' })
  })

  it('maps interview results onto the existing timeline', () => {
    const application = { applicationId: 1, realName: '李同学', gender: '1', className: '软工2402', firstChoice: 'FRONTEND', secondChoice: 'BACKEND', photoUrl: '/profile/a.jpg', applyStatus: 'PROCESSING' }
    const departments = mapInterviewProcess(application, [{ roundId: 1, department: 'FRONTEND', resultStatus: 'PASS' }])
    expect(departments[0].stages[0].status).toBe('passed')
    expect(departments[0].stages[1].status).toBe('pending')
    expect(departments[1].stages[1].status).toBe('locked')
  })

  it('maps OUT and roundNo onto the timeline', () => {
    const application = { applicationId: 1, realName: '李同学', gender: '1', className: '软工2402', firstChoice: 'FRONTEND', secondChoice: 'BACKEND', photoUrl: '/profile/a.jpg', applyStatus: 'PROCESSING' }
    const departments = mapInterviewProcess(application, [
      { roundId: 11, roundNo: 1, department: 'FRONTEND', resultStatus: 'PASS' },
      { roundId: 12, roundNo: 2, department: 'FRONTEND', resultStatus: 'OUT' },
    ])
    expect(departments[0].stages[0].status).toBe('passed')
    expect(departments[0].stages[1].status).toBe('rejected')
  })

  it('maps first-round interview time onto scheduled status', () => {
    const application = { applicationId: 1, realName: '李同学', gender: '1', className: '软工2402', firstChoice: 'FRONTEND', secondChoice: 'BACKEND', photoUrl: '/profile/a.jpg', applyStatus: 'PROCESSING' }
    const departments = mapInterviewProcess(application, [
      { roundId: 1, department: 'FRONTEND', resultStatus: 'PENDING', interviewTime: '2026-09-22 18:30:00' },
    ])
    expect(departments[0].stages[0].status).toBe('scheduled')
    expect(departments[0].stages[0].detail.interviewTime).toBe('2026-09-22 18:30:00')
  })

  it('closes new applications while keeping existing updates', () => {
    expect(mapApplyWindow({ application: null, profile: null, newApplicationsOpen: false, canUpdate: false })).toEqual({
      newApplicationsOpen: false,
      canUpdate: false,
    })
    expect(mapApplyWindow({
      application: { applicationId: 1, realName: '李同学', gender: '1', className: '软工2402', firstChoice: 'FRONTEND', secondChoice: 'BACKEND', photoUrl: '/profile/a.jpg', applyStatus: 'SUBMITTED' },
      profile: {},
      newApplicationsOpen: false,
    })).toEqual({
      newApplicationsOpen: false,
      canUpdate: true,
    })
  })
})
