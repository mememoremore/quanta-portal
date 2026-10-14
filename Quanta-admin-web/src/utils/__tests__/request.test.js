import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getInfo, login } from '@/api/auth'
import { removeToken, setToken } from '@/utils/token'
import { setUnauthorizedHandler } from '@/utils/unauthorized'
import { isMockApiRequest, shouldUseMock } from '@/config/mock-api'

describe('request and mock adapter', () => {
  beforeEach(() => {
    removeToken()
    setUnauthorizedHandler(null)
  })

  it('returns a RuoYi-shaped login response', async () => {
    const response = await login({ username: 'admin', password: 'quanta123' })
    expect(response).toMatchObject({ code: 200, token: 'mock-token-admin' })
  })

  it('rejects invalid credentials with the business message', async () => {
    await expect(login({ username: 'admin', password: 'wrong' })).rejects.toMatchObject({
      code: 500,
      message: '用户名或密码错误',
      traceId: expect.stringMatching(/^[a-f0-9]{32}$/),
    })
  })

  it('notifies the app when a token is invalid', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    setToken('invalid-token')

    await expect(getInfo()).rejects.toMatchObject({ code: 401 })
    expect(handler).toHaveBeenCalledOnce()
  })

  it('uses the real backend by default when full mock mode is disabled', () => {
    expect(shouldUseMock({ url: '/dashboard/stats', method: 'get' }, false, [])).toBe(false)
  })

  it('matches an explicitly listed mock endpoint by method and path', () => {
    const routes = [{ method: 'get', path: '/dashboard/stats' }]
    expect(isMockApiRequest({ url: '/dashboard/stats?year=2026', method: 'GET' }, routes)).toBe(true)
    expect(shouldUseMock({ url: '/dashboard/stats', method: 'get' }, false, routes)).toBe(true)
  })

  it('never enables partial mock routing for authentication endpoints', () => {
    const routes = [{ method: 'post', path: '/login' }]
    expect(shouldUseMock({ url: '/login', method: 'post' }, false, routes)).toBe(false)
    expect(shouldUseMock({ url: '/register', method: 'post' }, false, routes)).toBe(false)
    expect(shouldUseMock({ url: '/register/emailCode', method: 'post' }, false, routes)).toBe(false)
    expect(shouldUseMock({ url: '/getInfo', method: 'get' }, false, routes)).toBe(false)
  })

  it('does not treat a real backend failure as a reason to use mock', () => {
    const routes = [{ method: 'get', path: '/qt/member/list' }]
    expect(shouldUseMock({ url: '/qt/member/cohorts', method: 'get' }, false, routes)).toBe(false)
  })

  it('routes the confirmed broken cohort endpoint through the explicit Web mock list', () => {
    expect(shouldUseMock({ url: '/qt/member/cohorts', method: 'get' }, false)).toBe(true)
  })
})
