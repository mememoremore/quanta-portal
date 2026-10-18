const REAL_AUTH_PATHS = new Set([
  '/login',
  '/register',
  '/register/emailCode',
  '/captchaImage',
  '/getInfo',
  '/getRouters',
  '/logout',
  '/auth/refresh',
  '/system/password/emailCode',
  '/system/password/reset',
])

// 只登记已经过真实后端联调、确认暂不可用的接口。
// 届次列表由真实后端返回数字编号。不要放回这里：部分 Mock 会把编号改成 21st，分配权限会被拒绝。
export const mockApiRoutes = []

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
