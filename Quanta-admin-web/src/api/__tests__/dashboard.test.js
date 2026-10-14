import { describe, expect, it } from 'vitest'
import { mapDashboardStats } from '@/api/dashboard'

describe('dashboard API mapping', () => {
  it('maps real backend statistic fields to the Web view model', () => {
    expect(
      mapDashboardStats({
        memberCount: 2,
        todayResumeCount: 3,
        pendingPaymentCount: 5,
      }),
    ).toEqual({
      members: 2,
      resumesToday: 3,
      pendingPayments: 5,
    })
  })

  it('keeps the existing mock field names compatible', () => {
    expect(mapDashboardStats({ members: 1, resumesToday: 2 })).toMatchObject({
      members: 1,
      resumesToday: 2,
    })
  })
})
