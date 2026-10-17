import { getToken } from '@/utils/token'
import { authHandlers } from './handlers/auth'
import { dashboardHandlers } from './handlers/dashboard'
import { memberHandlers } from './handlers/members'
import { recruitmentHandlers } from './handlers/recruitment'
import { sharingSignupHandlers } from './handlers/sharing-signups'
import { lectureSignupHandlers } from './handlers/lecture-signups'
import { bookBorrowHandlers } from './handlers/book-borrows'
import { materialHandlers } from './handlers/materials'
import { clothingOrderHandlers } from './handlers/clothing-orders'
import { metricsHandlers } from './handlers/metrics'
import { forumHandlers } from './handlers/forum'
import { memberProfileHandlers } from './handlers/member-profiles'

const handlers = [
  ...memberProfileHandlers,
  ...authHandlers,
  ...dashboardHandlers,
  ...memberHandlers,
  ...recruitmentHandlers,
  ...sharingSignupHandlers,
  ...lectureSignupHandlers,
  ...bookBorrowHandlers,
  ...materialHandlers,
  ...clothingOrderHandlers,
  ...metricsHandlers,
  ...forumHandlers,
]

function waitForMockLatency() {
  if (import.meta.env.MODE === 'test') return Promise.resolve()
  const delay = 200 + Math.floor(Math.random() * 301)
  return new Promise((resolve) => window.setTimeout(resolve, delay))
}

export async function mockRequest(config) {
  await waitForMockLatency()

  const method = String(config.method || 'get').toLowerCase()
  const path = String(config.url || '').split('?')[0]
  const handler = handlers.find(
    (item) => item.method === method && (item.path === path || item.match?.(path)),
  )

  if (!handler) {
    return { code: 404, msg: `Mock 接口不存在：${method.toUpperCase()} ${path}` }
  }

  const token = config.__partialMock ? 'mock-token-admin' : getToken()
  const headers = {
    ...(config.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  return handler.handle({ ...config, path, headers })
}
