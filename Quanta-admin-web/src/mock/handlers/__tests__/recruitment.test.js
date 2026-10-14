import { beforeEach, describe, expect, it } from 'vitest'
import { recruitmentHandlers } from '../recruitment'
import { findMockApplication, resetMockRecruitment } from '../../data/recruitment'

function handler(method, path) {
  return recruitmentHandlers.find(
    (item) => item.method === method && (item.path === path || item.match?.(path)),
  )
}

function config(token, extra = {}) {
  return { headers: { Authorization: `Bearer ${token}` }, ...extra }
}

describe('recruitment mock handlers', () => {
  beforeEach(resetMockRecruitment)

  it('limits department managers to their own department', () => {
    const response = handler('get', '/qt/interview/admin/applications').handle(
      config('mock-token-product-manager', { params: { roundId: 1 } }),
    )

    expect(response.code).toBe(200)
    expect(response.rows.length).toBeGreaterThan(0)
    expect(
      response.rows.every(
        (row) => row.firstChoice === 'PRODUCT' || row.secondChoice === 'PRODUCT',
      ),
    ).toBe(true)
  })

  it('rejects a forged result for another department', () => {
    const response = handler('post', '/qt/interview/result').handle(
      config('mock-token-product-manager', {
        data: { applicationId: 1001, roundId: 1, department: 'DESIGN', resultStatus: 'FAIL' },
      }),
    )

    expect(response.code).toBe(403)
  })

  it('advances only the first choice when both choices pass', () => {
    const application = findMockApplication(1001)
    expect(application.tracks[0].rounds[2].advanced).toBe(true)
    expect(application.tracks[1].rounds[1].status).toBe('PASS')
    expect(application.tracks[1].rounds[2].advanced).toBe(false)
  })

  it('advances the second choice when the first fails', () => {
    const application = findMockApplication(1005)
    const saveResult = handler('post', '/qt/interview/result')
    saveResult.handle(
      config('mock-token-ceo', {
        data: { applicationId: 1005, roundId: 1, department: 'PRODUCT', resultStatus: 'FAIL' },
      }),
    )
    saveResult.handle(
      config('mock-token-ceo', {
        data: { applicationId: 1005, roundId: 1, department: 'DESIGN', resultStatus: 'PASS' },
      }),
    )

    expect(application.tracks[0].rounds[2].advanced).toBe(false)
    expect(application.tracks[1].rounds[2].advanced).toBe(true)
  })

  it('lets managers edit second-round feedback but rejects their result decisions', () => {
    const save = handler('post', '/qt/interview/admin/evaluations')
    const edit = save.handle(
      config('mock-token-product-interviewer', {
        data: { applicationId: 1003, roundId: 2, department: 'PRODUCT', content: '经理面评' },
      }),
    )
    const decide = handler('post', '/qt/interview/result').handle(
      config('mock-token-product-interviewer', {
        data: { applicationId: 1001, roundId: 1, department: 'PRODUCT', resultStatus: 'PASS' },
      }),
    )

    expect(edit.code).toBe(200)
    expect(decide.code).toBe(403)
  })

  it('rejects management feedback edits but lets management decide', () => {
    const edit = handler('post', '/qt/interview/admin/evaluations').handle(
      config('mock-token-product-manager', {
        data: { applicationId: 1001, roundId: 1, department: 'PRODUCT', content: '管理层面评' },
      }),
    )
    const decide = handler('post', '/qt/interview/result').handle(
      config('mock-token-product-manager', {
        data: { applicationId: 1001, roundId: 1, department: 'PRODUCT', resultStatus: 'PASS' },
      }),
    )

    expect(edit.code).toBe(403)
    expect(decide.code).toBe(200)
  })

  it('rejects the removed Waiting result', () => {
    const response = handler('post', '/qt/interview/result').handle(
      config('mock-token-product-manager', {
        data: {
          applicationId: 1001,
          roundId: 1,
          department: 'PRODUCT',
          resultStatus: 'WAITING',
        },
      }),
    )

    expect(response.code).toBe(400)
  })

  it('lets department management send an atomic Out notification in its own department', () => {
    const response = handler('post', '/qt/interview/admin/offers').handle(
      config('mock-token-product-manager', {
        data: {
          applicationId: 1003,
          department: 'PRODUCT',
          decision: 'FAIL',
          content: '很遗憾，本次未能录用你。',
        },
      }),
    )
    const application = findMockApplication(1003)
    const track = application.tracks.find((item) => item.department === 'PRODUCT')

    expect(response.code).toBe(200)
    expect(track.rounds[2].status).toBe('FAIL')
    expect(application.applicationStatus).toBe('REJECTED')
    expect(response.data.emailSent).toBe(true)
  })

  it('rejects a management offer for another department', () => {
    const response = handler('post', '/qt/interview/admin/offers').handle(
      config('mock-token-product-manager', {
        data: {
          applicationId: 1001,
          department: 'DESIGN',
          decision: 'PASS',
          content: '祝贺你通过面试。',
        },
      }),
    )

    expect(response.code).toBe(403)
  })

  it('saves a shared first-round interview time', () => {
    const path = '/qt/interview/admin/applications/1001/rounds/1/interview-time'
    const response = handler('put', path).handle(
      config('mock-token-ceo', {
        path,
        data: { interviewTime: '2026-09-22 18:30:00' },
      }),
    )
    const application = findMockApplication(1001)

    expect(response.code).toBe(200)
    expect(response.data.interviewTime).toBe('2026-09-22 18:30:00')
    expect(application.firstRoundInterviewTime).toBe('2026-09-22 18:30:00')
    expect(application.tracks.every((track) => track.rounds[1].interviewTime === '2026-09-22 18:30:00')).toBe(true)
  })
})
