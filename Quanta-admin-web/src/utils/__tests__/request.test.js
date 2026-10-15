import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getInfo, login } from '@/api/auth'
import { getMetricsLatest } from '@/api/metrics'
import { refreshMock } from '@/mock/handlers/auth'
import { getToken, removeToken, setToken } from '@/utils/token'
import { setUnauthorizedHandler } from '@/utils/unauthorized'
import { isMockApiRequest, shouldUseMock } from '@/config/mock-api'
import { request, service } from '@/utils/request'
import { requestBlob } from '@/utils/download'

describe('request and mock adapter', () => {
  beforeEach(() => {
    removeToken()
    setUnauthorizedHandler(null)
  })

  afterEach(() => vi.unstubAllEnvs())

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
    expect(shouldUseMock({ url: '/auth/refresh', method: 'post' }, false, routes)).toBe(false)
  })

  it('refreshes once for concurrent 401 responses and retries the original request', async () => {
    refreshMock.mode = 'ok'
    refreshMock.token = 'mock-token-admin'
    setToken('invalid-token')

    const [first, second] = await Promise.all([getInfo(), getInfo()])

    expect(first.user.userName).toBe('admin')
    expect(second.user.userName).toBe('admin')
    expect(refreshMock.calls).toBe(1)
    expect(getToken()).toBe('mock-token-admin')
  })

  it('logs out when refresh fails and does not refresh a 403', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    setToken('invalid-token')
    await expect(getInfo()).rejects.toMatchObject({ code: 401 })
    expect(handler).toHaveBeenCalledOnce()
    expect(refreshMock.calls).toBe(1)

    handler.mockClear()
    refreshMock.calls = 0
    setToken('mock-token-viewer')
    await expect(getMetricsLatest()).rejects.toMatchObject({ code: 403 })
    expect(refreshMock.calls).toBe(0)
    expect(handler).not.toHaveBeenCalled()
  })

  it('uses one refresh for concurrent business 401 responses and retries both requests', async () => {
    vi.stubEnv('VITE_USE_MOCK', 'false')
    vi.stubEnv('VITE_DUAL_TOKEN_AUTH', 'true')
    setToken('expired-access')
    let refreshed = false
    const send = vi.spyOn(service, 'request').mockImplementation(async ({ url }) => {
      if (url === '/auth/refresh') {
        refreshed = true
        return { data: { code: 200, token: 'new-access', expiresIn: 900 } }
      }
      return { data: refreshed ? { code: 200, data: 'ok' } : { code: 401, msg: '过期' } }
    })

    const responses = await Promise.all([
      request({ url: '/getInfo', method: 'get' }),
      request({ url: '/getRouters', method: 'get' }),
    ])

    expect(responses.map((item) => item.data)).toEqual(['ok', 'ok'])
    expect(send.mock.calls.filter(([config]) => config.url === '/auth/refresh')).toHaveLength(1)
    expect(getToken()).toBe('new-access')
  })

  it('does not refresh on 403 or on a public login failure', async () => {
    vi.stubEnv('VITE_USE_MOCK', 'false')
    vi.stubEnv('VITE_DUAL_TOKEN_AUTH', 'true')
    const send = vi.spyOn(service, 'request').mockImplementation(async ({ url }) => ({
      data: { code: url === '/login' ? 401 : 403, msg: '拒绝访问' },
    }))
    await expect(request({ url: '/qt/materials', method: 'get' })).rejects.toMatchObject({ code: 403 })
    await expect(request({ url: '/login', method: 'post' })).rejects.toMatchObject({ code: 401 })
    expect(send).toHaveBeenCalledTimes(2)
  })

  it('clears access and reports unauthorized when refresh fails', async () => {
    vi.stubEnv('VITE_USE_MOCK', 'false')
    vi.stubEnv('VITE_DUAL_TOKEN_AUTH', 'true')
    setToken('expired-access')
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.spyOn(service, 'request').mockImplementation(async ({ url }) => ({
      data: { code: 401, msg: url === '/auth/refresh' ? 'Refresh 已失效' : 'Access 已失效' },
    }))
    await expect(request({ url: '/getInfo', method: 'get' })).rejects.toMatchObject({ code: 401 })
    expect(getToken()).toBe('')
    expect(handler).toHaveBeenCalledOnce()
  })

  it('refreshes and retries a blob download with a JSON business 401', async () => {
    vi.stubEnv('VITE_USE_MOCK', 'false')
    vi.stubEnv('VITE_DUAL_TOKEN_AUTH', 'true')
    setToken('expired-access')
    let refreshed = false
    vi.spyOn(service, 'request').mockImplementation(async ({ url }) => {
      if (url === '/auth/refresh') {
        refreshed = true
        return { data: { code: 200, token: 'new-access' } }
      }
      return refreshed
        ? { data: new Blob(['pdf'], { type: 'application/pdf' }), headers: {} }
        : { data: new Blob([JSON.stringify({ code: 401, msg: '过期' })], { type: 'application/json' }) }
    })
    const result = await requestBlob({ url: '/qt/materials/download', method: 'get' })
    expect(await result.blob.text()).toBe('pdf')
    expect(getToken()).toBe('new-access')
  })

  it('does not treat a real backend failure as a reason to use mock', () => {
    const routes = [{ method: 'get', path: '/qt/member/list' }]
    expect(shouldUseMock({ url: '/qt/member/cohorts', method: 'get' }, false, routes)).toBe(false)
  })

  it('routes the confirmed broken cohort endpoint through the explicit Web mock list', () => {
    expect(shouldUseMock({ url: '/qt/member/cohorts', method: 'get' }, false)).toBe(true)
  })
})
