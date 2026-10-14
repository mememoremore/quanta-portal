import { describe, expect, it } from 'vitest'
import { safeAudienceRedirect, safeInternalRedirect } from '../redirect'

describe('safeInternalRedirect', () => {
  it('accepts site-local paths', () => {
    expect(safeInternalRedirect('/members?cohort=21')).toBe('/members?cohort=21')
  })

  it('rejects protocol-relative and external paths', () => {
    expect(safeInternalRedirect('//evil.example')).toBe('/admin/dashboard')
    expect(safeInternalRedirect('https://evil.example')).toBe('/admin/dashboard')
    expect(safeInternalRedirect('/\\evil.example')).toBe('/admin/dashboard')
  })
})

describe('safeAudienceRedirect', () => {
  it('accepts only redirects inside the selected audience', () => {
    expect(safeAudienceRedirect('/member/security', 'member')).toBe('/member/security')
    expect(safeAudienceRedirect('/freshman/home', 'freshman')).toBe('/freshman/home')
  })

  it('falls back for the identity entry and cross-audience redirects', () => {
    expect(safeAudienceRedirect('/', 'member')).toBe('/member/home')
    expect(safeAudienceRedirect('/freshman/home', 'member')).toBe('/member/home')
    expect(safeAudienceRedirect('//evil.example', 'member')).toBe('/member/home')
  })
})
