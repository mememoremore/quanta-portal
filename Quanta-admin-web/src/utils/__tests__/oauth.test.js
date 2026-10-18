import { describe, expect, it } from 'vitest'
import { interactionId, isOAuthMember, validateInteraction, validateApproval, validateDeniedRedirect } from '../oauth'

const id = 'a'.repeat(43)
const now = Date.parse('2026-10-18T05:00:00Z')
const context = () => ({ interactionId: id, status: 'pending', expiresAt: new Date(now + 600000).toISOString(), csrfToken: 's'.repeat(43), consentRequired: true, scopes: ['openid', 'profile'], client: { clientId: 'quanta-challenge', name: 'Quanta Challenge', origin: 'https://challenge.quantacenter.com', redirectUri: 'https://challenge.quantacenter.com/api/auth/quanta/callback' } })

describe('OAuth interaction boundary', () => {
  it('accepts only a single opaque interaction identifier', () => {
    expect(interactionId(id)).toBe(id)
    for (const value of [undefined, [id], '../../login', 'abc', '<script>', 'a'.repeat(129)]) expect(() => interactionId(value)).toThrow()
  })
  it('never infers member eligibility from roles or a local audience', () => {
    for (const flag of [true, 1, '1']) expect(isOAuthMember({ isQuantaMember: flag })).toBe(true)
    for (const value of [null, {}, { roles: ['ceo'] }, { isQuantaMember: '0' }]) expect(isOAuthMember(value)).toBe(false)
  })
  it('validates server-owned client identity, callback, scope and expiry', () => {
    expect(validateInteraction(context(), id, now).client.name).toBe('Quanta Challenge')
    for (const bad of [
      { interactionId: 'b'.repeat(43) }, { expiresAt: 'bad' }, { expiresAt: new Date(now).toISOString() },
      { scopes: ['openid', 'admin'] }, { scopes: ['openid'] }, { csrfToken: '' }, { status: 'unknown' },
      { client: { ...context().client, origin: 'https://evil.test' } },
      { client: { ...context().client, redirectUri: 'https://challenge.quantacenter.com.evil.test/api/auth/quanta/callback' } },
    ]) expect(() => validateInteraction({ ...context(), ...bad }, id, now)).toThrow()
  })
  it('approval points only at the same transaction continuation, never an arbitrary redirect', () => {
    const approval = { status: 'approved', continuePath: `/oauth2/interaction/${id}/continue`, expiresAt: new Date(now + 60000).toISOString() }
    expect(validateApproval(approval, id, now)).toEqual(approval)
    for (const continuePath of ['//evil.test', 'https://evil.test', '/oauth2/interaction/other/continue', `${approval.continuePath}?token=x`]) expect(() => validateApproval({ ...approval, continuePath }, id, now)).toThrow()
  })
  it('only follows the exact registered callback after denial', () => {
    const uri = context().client.redirectUri
    expect(validateDeniedRedirect(`${uri}?error=access_denied&state=opaque`, uri)).toContain('access_denied')
    for (const target of ['https://evil.test', `${uri}/evil?error=access_denied`, `${uri}?code=secret`, `${uri}?error=access_denied#error`, `${uri}?error=access_denied&state=x&state=y`]) expect(() => validateDeniedRedirect(target, uri)).toThrow()
  })
})
