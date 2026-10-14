import { describe, expect, it } from 'vitest'
import { portalLocalData } from '../portal-local-data'

describe('portal local-data boundary', () => {
  it('keeps the registry explicit and minimal', () => {
    expect(portalLocalData.map((item) => item.key)).toEqual(['business-card', 'home-banners'])
    expect(portalLocalData.some((item) => item.key.includes('workstation'))).toBe(false)
    expect(portalLocalData.some((item) => item.key.includes('recruitment'))).toBe(false)
  })
})
