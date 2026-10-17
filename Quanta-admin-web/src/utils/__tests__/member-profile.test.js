import { afterEach, describe, expect, it, vi } from 'vitest'
import { normalizeProfile, validateProfile, memberDestination, safeProfileReturn, assetUrl } from '../member-profile'

describe('member profile contract', () => {
  afterEach(() => vi.unstubAllEnvs())
  it('does not double-prefix avatars when the production API base is relative', () => {
    vi.stubEnv('VITE_API_BASE_URL', '/api')
    expect(assetUrl('/profile/a.jpg')).toBe('/api/profile/a.jpg')
    expect(assetUrl(assetUrl('/profile/a.jpg'))).toBe('/api/profile/a.jpg')
  })
  it('keeps only editable fields and normalizes unique skills', () => {
    expect(normalizeProfile({ userId: 1, name: '不能改名', skills: ['Vue', 'Vue', ' TS '], bio: ' hi ', email: 'login@private.com' })).toEqual({ headline: '', bio: 'hi', skills: ['Vue', 'TS'], githubUrl: '', websiteUrl: '', contactEmail: '', showContactEmail: false })
  })
  it('rejects unsafe links, too many tags and invalid contact email', () => {
    expect(validateProfile({ websiteUrl: 'javascript:alert(1)' })).toContain('http')
    expect(validateProfile({ skills: ['1','2','3','4','5','6'] })).toContain('5')
    expect(validateProfile({ contactEmail: 'bad', showContactEmail: true })).toContain('邮箱')
    expect(validateProfile({ headline: '长'.repeat(81) })).toContain('80')
  })
  it('only navigates to valid member IDs and allows safe member returns', () => {
    expect(memberDestination(7, 7)).toBe('/member/profile')
    expect(memberDestination(8, 7)).toBe('/member/people/8')
    expect(memberDestination(undefined, 7)).toBe('')
    expect(safeProfileReturn('//evil.com')).toBe('/member/community')
    expect(safeProfileReturn('/member/community/2?page=3')).toBe('/member/community/2?page=3')
    expect(assetUrl('javascript:alert(1)')).toBe('')
    expect(assetUrl('data:image/svg+xml,<svg/>')).toBe('')
  })
})
