import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import {
  getMyActivitySignup,
  getPublishedActivities,
  signupActivity,
} from '../activities'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('freshman activity API', () => {
  beforeEach(() => request.mockReset())

  it('loads published activities from the hardened qt endpoint using GET params', async () => {
    request.mockResolvedValue({
      rows: [
        {
          activityId: '7',
          activityType: 'LECTURE',
          title: '新生宣讲会',
          capacity: '80',
          status: 'PUBLISHED',
        },
      ],
    })

    await expect(getPublishedActivities()).resolves.toEqual([
      expect.objectContaining({ activityId: 7, activityType: 'LECTURE', capacity: 80 }),
    ])
    expect(request).toHaveBeenCalledWith({
      url: '/qt/activity/list',
      method: 'get',
      params: { status: 'PUBLISHED', pageNum: 1, pageSize: 100 },
    })
  })

  it('loads only the current user signup using GET params', async () => {
    request.mockResolvedValue({
      rows: [{ signupId: 3, activityId: 7, status: 'APPLIED', signupTime: '2026-09-15' }],
    })

    await expect(getMyActivitySignup(7)).resolves.toEqual(
      expect.objectContaining({ signupId: 3, activityId: 7, status: 'APPLIED' }),
    )
    expect(request).toHaveBeenCalledWith({
      url: '/qt/signup/detailList',
      method: 'get',
      params: { activityId: 7, pageNum: 1, pageSize: 20 },
    })
  })

  it('submits an activity signup to the real qt endpoint', async () => {
    request.mockResolvedValue({ code: 200 })

    await signupActivity('7', '期待参加')

    expect(request).toHaveBeenCalledWith({
      url: '/qt/signup',
      method: 'post',
      data: { activityId: 7, remark: '期待参加' },
    })
  })
})
