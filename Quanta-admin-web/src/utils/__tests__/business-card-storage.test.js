import { describe, expect, it } from 'vitest'
import { loadBusinessCard, saveBusinessCard } from '../business-card-storage'

describe('business card browser storage', () => {
  it('stores cards per member in this browser', () => {
    saveBusinessCard({ memberId: 'QT021', name: '小李', bio: '  Keep learning.  ' })
    expect(loadBusinessCard('QT021')).toMatchObject({ name: '小李', bio: 'Keep learning.' })
    expect(loadBusinessCard('QT022')).toBeNull()
  })

  it('limits the local biography to 800 characters', () => {
    saveBusinessCard({ memberId: 'QT021', bio: 'a'.repeat(900) })
    expect(loadBusinessCard('QT021').bio).toHaveLength(800)
  })
})
