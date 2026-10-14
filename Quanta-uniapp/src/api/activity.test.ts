import { describe, expect, it } from 'vitest'
import { mapSignup, selectActivity } from './activity'

describe('activity backend mapping', () => {
  const rows = [
    { activityId: 1, activityType: 'GENERAL' as const, title: 'Quanta 招新宣讲', status: 'PUBLISHED' as const },
    { activityId: 2, activityType: 'GENERAL' as const, title: '精英分享会', status: 'PUBLISHED' as const },
  ]

  it('selects legacy general activities by title', () => {
    expect(selectActivity(rows, 'LECTURE')?.activityId).toBe(1)
    expect(selectActivity(rows, 'SHARING')?.activityId).toBe(2)
  })

  it('maps the authenticated users signup row', () => {
    expect(mapSignup({ activityId: 9, status: 'APPLIED', signupTime: '2026-09-14', remark: null })).toEqual({
      activityId: 9,
      status: 'APPLIED',
      signupTime: '2026-09-14',
      remark: '',
    })
  })
})
