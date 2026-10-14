import { beforeEach, describe, expect, it } from 'vitest'
import { getSharingRegistrations } from '../sharing-signups'
import { setToken } from '@/utils/token'

describe('business record apis', () => {
  beforeEach(() => setToken('mock-token-product-manager'))

  it('loads sharing registrations with the registration quota', async () => {
    const result = await getSharingRegistrations()
    expect(result).toMatchObject({ total: 10, quota: 50 })
    expect(result.rows[0]).toMatchObject({ name: '陈佳琪' })
  })

})
