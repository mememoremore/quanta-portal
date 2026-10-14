import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearAudience,
  getAudience,
  homePathFor,
  loginPathFor,
  loginTypeFor,
  setAudience,
} from '../session-audience'

describe('session audience', () => {
  beforeEach(() => localStorage.clear())

  it('persists only a known audience', () => {
    setAudience('freshman')
    expect(getAudience()).toBe('freshman')
    clearAudience()
    expect(getAudience()).toBe('')
    expect(() => setAudience('unknown')).toThrow('未知登录身份')
  })

  it('maps audiences to login types and safe destinations', () => {
    expect(loginTypeFor('freshman')).toBe('0')
    expect(loginTypeFor('member')).toBe('1')
    expect(loginTypeFor('admin')).toBe('1')
    expect(loginPathFor('freshman')).toBe('/login/freshman')
    expect(loginPathFor('member')).toBe('/login/member')
    expect(loginPathFor('admin')).toBe('/admin/login')
    expect(homePathFor('admin')).toBe('/admin/dashboard')
  })
})
