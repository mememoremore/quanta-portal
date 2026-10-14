import { describe, expect, it } from 'vitest'
import { sharingSignupHandlers } from '../sharing-signups'

function config(token) {
  return { headers: { Authorization: `Bearer ${token}` } }
}

describe('business record permissions', () => {
  it('allows management to read sharing records', () => {
    expect(sharingSignupHandlers[0].handle(config('mock-token-product-manager')).code).toBe(200)
  })

  it('rejects the manager role from management-only record pages', () => {
    expect(sharingSignupHandlers[0].handle(config('mock-token-product-interviewer')).code).toBe(403)
  })
})
