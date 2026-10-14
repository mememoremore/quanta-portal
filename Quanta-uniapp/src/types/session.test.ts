import { describe, expect, it } from 'vitest'
import { normalizeSession, type UserProfile } from './session'
import { resolveGuardTarget } from '../utils/authGuard'

const towerProfile: UserProfile = {
  id: 'PM2301',
  name: '方东升',
  account: 'PM2301',
  role: 'tower',
  department: '产品部',
  batch: '20th',
}

describe('resolveGuardTarget', () => {
  it('redirects anonymous business routes to role selection', () => {
    expect(resolveGuardTarget('/pages/member/home/home', { token: '', role: null })).toBe('/pages/login/select-role')
  })

  it('isolates freshman and tower routes', () => {
    expect(resolveGuardTarget('/pages/member/home/home', { token: 't', role: 'freshman' })).toBe('/pages/freshman/home')
    expect(resolveGuardTarget('/pages/freshman/home', { token: 't', role: 'tower' })).toBe('/pages/member/home/home')
    expect(resolveGuardTarget('/pages/member/profile/profile', { token: 't', role: 'tower' })).toBeNull()
  })

  it('keeps login routes public', () => {
    expect(resolveGuardTarget('/pages/login/tower', { token: '', role: null })).toBeNull()
  })
})

describe('normalizeSession', () => {
  it('clears invalid roles', () => {
    expect(normalizeSession({ token: 'x', role: 'bad', profile: null })).toEqual({ token: '', role: null, profile: null })
  })

  it('keeps a valid tower session', () => {
    expect(normalizeSession({ token: 'x', role: 'tower', profile: towerProfile })).toEqual({
      token: 'x',
      role: 'tower',
      profile: towerProfile,
    })
  })
})
