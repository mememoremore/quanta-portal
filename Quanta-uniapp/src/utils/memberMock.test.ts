import { describe, expect, it } from 'vitest'
import { canOpenBusinessCard, filterContacts, normalizeBusinessCardBio, validateShirtSelection, type MemberContact } from './memberMock'

const contacts: MemberContact[] = [
  { id: 1, name: '方东升', code: 'PM2301', batch: '20th', role: 'COO', roleColor: '#FBBF89', department: '产品部', position: '产品经理', hasBusinessCard: true, isActiveTalent: true, bio: '' },
  { id: 2, name: '林子里', code: 'PM2502', batch: '21st', role: '实习生', roleColor: '#82E0F5', department: '产品部', position: '实习生', hasBusinessCard: false, isActiveTalent: false, bio: '' },
]

describe('member directory filtering', () => {
  it('filters by batch and search text', () => {
    expect(filterContacts(contacts, '20th', '方')).toHaveLength(1)
    expect(filterContacts(contacts, '20th', 'pm2301')).toHaveLength(1)
    expect(filterContacts(contacts, '20th', 'PM2502')).toHaveLength(0)
  })
})

describe('business card access', () => {
  it('only opens created cards and normalizes biographies', () => {
    expect(canOpenBusinessCard({ hasBusinessCard: true })).toBe(true)
    expect(canOpenBusinessCard({ hasBusinessCard: false })).toBe(false)
    expect(normalizeBusinessCardBio('  hello  ')).toBe('hello')
    expect(normalizeBusinessCardBio('x'.repeat(801))).toHaveLength(800)
  })
})

describe('shirt selection validation', () => {
  it('requires a color and size', () => {
    expect(validateShirtSelection({ color: '', size: 'XL' })).toBe('请选择塔服颜色')
    expect(validateShirtSelection({ color: '星曜黑', size: '' })).toBe('请选择塔服尺码')
    expect(validateShirtSelection({ color: '星曜黑', size: 'XL' })).toBeNull()
  })
})
