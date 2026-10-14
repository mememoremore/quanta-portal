import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createOfferPayload,
  departmentLabels,
  getNoticePreview,
  getRecruitmentApplications,
  getRecruitmentStatistics,
  mapApplication,
  mapEvaluation,
  mergeApplicationDetail,
  normalizeEvaluation,
  sendOffer,
  sendResultNotice,
} from '../recruitment'
import * as requestModule from '@/utils/request'
import { findMockApplication, resetMockRecruitment } from '@/mock/data/recruitment'
import { setToken } from '@/utils/token'

describe('recruitment api', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    resetMockRecruitment()
    setToken('mock-token-product-manager')
  })

  it('maps a department-scoped application list', async () => {
    const result = await getRecruitmentApplications({ roundId: 1 })
    expect(result.total).toBeGreaterThan(0)
    expect(result.rows[0].choices[0].label).toBeTruthy()
    expect(result.rows.every((row) => row.choices.some((choice) => choice.department === 'PRODUCT'))).toBe(true)
  })

  it('returns unique candidate statistics', async () => {
    const result = await getRecruitmentStatistics()
    expect(result).toMatchObject({ submissions: 6, firstPassed: 1, secondPassed: 0 })
  })

  it('saves the second-round result with its notification', async () => {
    const response = await sendOffer({
      applicationId: 1003,
      department: 'PRODUCT',
      decision: 'PASS',
      content: '祝贺你通过面试。',
    })

    const application = findMockApplication(1003)
    expect(application.tracks[1].rounds[2].status).toBe('PASS')
    expect(application.applicationStatus).toBe('OFFERED')
    expect(response.data.emailSent).toBe(true)
  })

  it('maps flat backend choices to the stable page model', () => {
    const result = mapApplication({
      applicationId: 8,
      realName: '吴晓萌',
      firstChoice: 'DESIGN',
      secondChoice: 'PRODUCT',
      firstChoiceStatus: 'PASS',
      secondChoiceStatus: 'OUT',
      resumeAccessUrl: 'https://example.com/resume.pdf?token=x',
      resumeFileName: '吴晓萌-简历.pdf',
      createTime: '2026-08-30 10:00:00',
    })

    expect(result.choices.map((item) => item.department)).toEqual(['DESIGN', 'PRODUCT'])
    expect(result.choices[1].rounds[1].status).toBe('OUT')
    expect(result.appliedAt).toBe('2026-08-30 10:00:00')
    expect(result.resumeUrl).toBe('https://example.com/resume.pdf?token=x')
    expect(result.resumeFileName).toBe('吴晓萌-简历.pdf')
  })

  it('maps evaluation login names onto interviewer fields', () => {
    expect(
      mapEvaluation({
        evaluationId: 1,
        evaluatorUserId: 8,
        evaluatorUserName: 'tower_a',
        evaluatorName: 'nick',
        content: 'ok',
      }),
    ).toMatchObject({
      interviewerId: 8,
      interviewerName: 'tower_a',
      evaluatorUserName: 'tower_a',
    })
  })

  it('merges detail sections and translates the offer contract', () => {
    expect(
      mergeApplicationDetail({
        application: { applicationId: 8, firstChoice: 'PRODUCT' },
        profile: { realName: '吴晓萌', major: '数字媒体' },
      }),
    ).toMatchObject({ applicationId: 8, name: '吴晓萌', major: '数字媒体' })

    expect(
      createOfferPayload({
        applicationId: 8,
        department: 'PRODUCT',
        decision: 'FAIL',
        content: '通知内容',
      }),
    ).toEqual({
      applicationId: 8,
      department: 'PRODUCT',
      decision: 'OUT',
      notice: '通知内容',
      roundId: 2,
    })
  })

  it('advances both choices when both pass the first round', () => {
    const result = mapApplication({
      applicationId: 9,
      firstChoice: 'FRONTEND',
      secondChoice: 'DESIGN',
      firstChoiceFirstRoundStatus: 'PASS',
      secondChoiceFirstRoundStatus: 'PASS',
      firstChoiceSecondRoundStatus: 'PENDING',
      secondChoiceSecondRoundStatus: 'PENDING',
      firstChoiceSecondRoundScore: 91,
      secondChoiceSecondRoundScore: 87,
    })
    expect(result.choices.map((choice) => choice.rounds[2].advanced)).toEqual([true, true])
    expect(result.choices.map((choice) => choice.rounds[2].score)).toEqual([91, 87])
  })

  it('exposes only the four active departments', () => {
    expect(Object.keys(departmentLabels)).toEqual(['BACKEND', 'PRODUCT', 'DESIGN', 'FRONTEND'])
    expect(JSON.stringify(departmentLabels)).not.toContain('ANDROID')
    expect(JSON.stringify(departmentLabels)).not.toContain('安卓')
  })

  it('maps FAIL as OUT and keeps second-round score metadata', () => {
    const result = mapApplication({
      applicationId: 11,
      firstChoice: 'BACKEND',
      firstChoiceFirstRoundStatus: 'PASS',
      firstChoiceSecondRoundStatus: 'FAIL',
      firstChoiceSecondRoundScore: 76,
      firstChoiceSecondRoundScoreUpdateBy: 'tower_a',
      firstChoiceSecondRoundScoreUpdateTime: '2026-09-18 12:00:00',
    })
    expect(result.choices[0].rounds[2].status).toBe('OUT')
    expect(result.choices[0].rounds[2].score).toBe(76)
    expect(result.choices[0].rounds[2].updatedBy).toBe('tower_a')
    expect(result.choices[0].rounds[2].updatedTime).toBe('2026-09-18 12:00:00')
  })

  it('requests the selected department preview and sends its QR image', async () => {
    const request = vi.spyOn(requestModule, 'request').mockResolvedValue({ data: { department: 'DESIGN' } })
    const file = new File(['qr'], 'design.png', { type: 'image/png' })
    await getNoticePreview(7, 'DESIGN')
    expect(request).toHaveBeenCalledWith({
      url: '/qt/interview/admin/applications/7/notice-preview', method: 'get', params: { department: 'DESIGN' },
    })
    await sendResultNotice(7, 'DESIGN', file)
    const config = request.mock.calls[1][0]
    expect(config).toMatchObject({ url: '/qt/interview/admin/applications/7/notice', method: 'post' })
    expect(config.data.get('department')).toBe('DESIGN')
    expect(config.data.get('qrCode')).toBe(file)
  })

  it('retains independent mail statuses for each department', () => {
    expect(mapApplication({ applicationId: 7, noticeStatuses: { PRODUCT: 'SENT', DESIGN: 'UNSENT' } }).noticeStatuses)
      .toEqual({ PRODUCT: 'SENT', DESIGN: 'UNSENT' })
  })

  it('normalizes the backend evaluation identity fields', () => {
    expect(normalizeEvaluation({ evaluatorUserId: 8, evaluatorName: '张三' })).toMatchObject({
      evaluatorUserId: 8,
      evaluatorName: '张三',
    })
    expect(normalizeEvaluation({ interviewerId: 9, interviewerName: '兼容数据' })).toMatchObject({
      evaluatorUserId: 9,
      evaluatorName: '兼容数据',
    })
  })
})
