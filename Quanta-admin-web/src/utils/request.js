import axios from 'axios'
import { getToken, setToken } from './token'
import { notifyUnauthorized } from './unauthorized'
import { mockRequest } from '@/mock'
import { isFullMockEnabled, shouldUseMock } from '@/config/mock-api'
import { getTraceId, withTraceId } from './trace-id'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  withCredentials: true,
})

let refreshPromise = null

export function refreshSession() {
  return request({
    url: '/auth/refresh',
    method: 'post',
    headers: { isToken: false },
    __skipAuthRefresh: true,
  })
}

function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshSession()
      .then((payload) => {
        const token = payload?.token || ''
        if (token) setToken(token)
        return token
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

service.interceptors.request.use((config) => {
  const tracedConfig = withTraceId(config)
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

export async function request(config) {
  const requestConfig = withTraceId(config)
  const fullMockEnabled = isMockEnabled()
  const useMock = shouldUseMock(requestConfig, fullMockEnabled)
  try {
    if (useMock && !fullMockEnabled && import.meta.env.DEV) {
      console.warn(`[Quanta Web] 使用 Mock 接口：${String(requestConfig.method || 'get').toUpperCase()} ${requestConfig.url}`)
    }
    const response = useMock
      ? await mockRequest({ ...requestConfig, __partialMock: !fullMockEnabled })
      : await service.request(requestConfig)
    const payload = useMock ? response : response.data
    return normalizeRuoYiResponse(payload)
  } catch (rawError) {
    const code = Number(
      rawError?.code ?? rawError?.response?.data?.code ?? rawError?.response?.status,
    )
    const isBusinessError = Number.isFinite(code) && rawError?.payload
    const error = isBusinessError
      ? rawError
      : createRequestError(
          rawError?.response?.data?.msg || rawError?.message || '网络连接异常',
          code || 0,
          rawError?.response?.data,
        )

    error.traceId =
      getTraceId(rawError?.config) ||
      getTraceId(rawError?.response?.config) ||
      getTraceId(requestConfig)
    const skipRefresh = Boolean(requestConfig.__skipAuthRefresh)
    const retried = Boolean(requestConfig.__isRetry)
    if (Number(error.code) === 401 && !skipRefresh && !retried) {
      let token = ''
      try {
        token = await refreshAccessToken()
      } catch {
        token = ''
      }
      if (token) {
        return request({
          ...requestConfig,
          __isRetry: true,
          headers: {
            ...(requestConfig.headers || {}),
            Authorization: `Bearer ${token}`,
          },
        })
      }
      await notifyUnauthorized(error)
    } else if (Number(error.code) === 401 && retried) {
      await notifyUnauthorized(error)
    }
    throw error
  }
}

export { service }
