import axios from 'axios'
import { getToken, removeToken, setToken } from './token'
import { notifyUnauthorized } from './unauthorized'
import { mockRequest } from '@/mock'
import { isFullMockEnabled, shouldUseMock } from '@/config/mock-api'
import { getTraceId, withTraceId } from './trace-id'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

export function isDualTokenEnabled() {
  return String(import.meta.env.VITE_DUAL_TOKEN_AUTH ?? 'true') === 'true'
}

const publicAuthPaths = new Set([
  '/login', '/logout', '/auth/refresh', '/register', '/register/emailCode',
  '/captchaImage', '/system/password/emailCode', '/system/password/reset',
])

let refreshPromise = null
service.interceptors.request.use((config) => {
  const tracedConfig = withTraceId(config)
  if (tracedConfig.withCredentials === undefined) {
    tracedConfig.withCredentials = isDualTokenEnabled()
  }
  const token = getToken()
  if (token && tracedConfig.headers?.isToken !== false) {
    tracedConfig.headers.Authorization = `Bearer ${token}`
  }
  return tracedConfig
})

function createRequestError(message, code, payload) {
  const error = new Error(message || '请求失败，请稍后重试')
  error.code = code
  error.payload = payload
  return error
}

export function normalizeRuoYiResponse(payload) {
  if (!payload || typeof payload !== 'object') return payload

  const code = Number(payload.code ?? 200)
  if (code !== 200) {
    throw createRequestError(payload.msg, code, payload)
  }
  return payload
}

export const isMockEnabled = isFullMockEnabled

async function readResponse(response, config) {
  if (config.responseType === 'blob') {
    if (response.data?.type?.includes('json')) {
      normalizeRuoYiResponse(JSON.parse(await response.data.text()))
    }
    return response
  }
  return normalizeRuoYiResponse(response.data)
}

function normalizeError(rawError, config) {
  const code = Number(
    rawError?.payload?.code ?? rawError?.response?.data?.code ??
    rawError?.response?.status ?? rawError?.code,
  )
  const error = rawError?.payload
    ? rawError
    : createRequestError(
        rawError?.response?.data?.msg || rawError?.message || '网络连接异常',
        Number.isFinite(code) ? code : 0,
        rawError?.response?.data,
      )
  error.traceId = getTraceId(rawError?.config) ||
    getTraceId(rawError?.response?.config) || getTraceId(config)
  return error
}

async function sendRefresh() {
  const response = await service.request({
    url: '/auth/refresh', method: 'post', data: {}, headers: { isToken: false },
  })
  const payload = normalizeRuoYiResponse(response.data)
  if (!payload.token) throw createRequestError('刷新接口未返回 Access Token', 0, payload)
  setToken(payload.token)
  return payload
}

// 同一页面单飞；支持 Web Locks 的浏览器还能避免多标签页同时轮换 Refresh Cookie。
export function refreshSession(expectedToken = getToken()) {
  if (!isDualTokenEnabled()) return Promise.reject(new Error('双 Token 尚未启用'))
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refresh = () => {
        const currentToken = getToken()
        return currentToken && currentToken !== expectedToken
          ? { code: 200, token: currentToken }
          : isMockEnabled()
            ? request({ url: '/auth/refresh', method: 'post', headers: { isToken: false } })
                .then((payload) => {
                  if (!payload.token) throw createRequestError('刷新接口未返回 Access Token', 0, payload)
                  setToken(payload.token)
                  return payload
                })
            : sendRefresh()
      }
      return !isMockEnabled() && navigator.locks?.request
        ? navigator.locks.request('quanta-refresh', refresh)
        : refresh()
    })().finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

async function executeRequest(config, useMock, tokenAtStart) {
  try {
    const response = useMock
      ? await mockRequest({ ...config, __partialMock: !isMockEnabled() })
      : await service.request(config)
    return useMock ? normalizeRuoYiResponse(response) : await readResponse(response, config)
  } catch (rawError) {
    const error = normalizeError(rawError, config)
    const isUnauthorized = Number(error.code) === 401
    const isPublicAuth = publicAuthPaths.has(String(config.url || '').split('?')[0])
    if (isUnauthorized && isDualTokenEnabled() && !config.__authRetried && !isPublicAuth) {
      let refreshed = false
      try {
        if (!getToken() || getToken() === tokenAtStart) await refreshSession(tokenAtStart)
        refreshed = true
        return await executeRequest({ ...config, __authRetried: true }, useMock, getToken())
      } catch (refreshError) {
        // 重试请求自身的错误由内层处理；刷新失败才从这里统一退出。
        if (refreshed && refreshError?.__authRetryHandled) throw refreshError
        removeToken()
        await notifyUnauthorized(error)
        error.__authRetryHandled = true
        throw error
      }
    }
    if (isUnauthorized && !isPublicAuth) {
      if (isDualTokenEnabled()) removeToken()
      await notifyUnauthorized(error)
    }
    error.__authRetryHandled = true
    throw error
  }
}

export async function request(config) {
  const requestConfig = withTraceId(config)
  const fullMockEnabled = isMockEnabled()
  const useMock = shouldUseMock(requestConfig, fullMockEnabled)
  if (useMock && !fullMockEnabled && import.meta.env.DEV) {
    console.warn(`[Quanta Web] 使用 Mock 接口：${String(requestConfig.method || 'get').toUpperCase()} ${requestConfig.url}`)
  }
  return executeRequest(requestConfig, useMock, getToken())
}

export { service }
