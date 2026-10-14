const DEFAULT_API_BASE_URL = 'http://127.0.0.1:8080'

export const resolveApiBaseUrl = (value?: string): string => {
  const configured = value?.trim()
  return configured ? configured.replace(/\/+$/, '') : DEFAULT_API_BASE_URL
}

export const resolveUseMock = (value?: string): boolean => value === 'true'

export const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL)
export const USE_LOGIN_MOCK = resolveUseMock(import.meta.env.VITE_USE_MOCK)
