const REAL_AUTH_PATHS = new Set([
  '/login',
  '/register',
  '/register/emailCode',
  '/captchaImage',
  '/getInfo',
  '/getRouters',
  '/logout',
  '/system/password/emailCode',
  '/system/password/reset',
])

// 只登记已经过真实后端联调、确认暂不可用的接口。
// 每一项都必须同步记录到 docs/Quanta管理端Web接口Mock清单.md。
export const mockApiRoutes = [{ method: 'get', path: '/qt/member/cohorts' }]

function normalizeRequest(config = {}) {
  return {
    method: String(config.method || 'get').toLowerCase(),
    path: String(config.url || '').split('?')[0],
  }
}

export function isFullMockEnabled(value = import.meta.env.VITE_USE_MOCK) {
  return String(value ?? 'true') !== 'false'
}

export function isMockApiRequest(config, routes = mockApiRoutes) {
  const request = normalizeRequest(config)
  if (REAL_AUTH_PATHS.has(request.path)) return false

  return routes.some((route) => {
    if (String(route.method || 'get').toLowerCase() !== request.method) return false
    return route.path instanceof RegExp
      ? route.path.test(request.path)
      : String(route.path) === request.path
  })
}

export function shouldUseMock(
  config,
  fullMockEnabled = isFullMockEnabled(),
  routes = mockApiRoutes,
) {
  return Boolean(fullMockEnabled) || isMockApiRequest(config, routes)
}
