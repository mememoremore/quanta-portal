import { findAccountByToken } from '../data/accounts'

function requireAccount(config) {
  const authorization = config.headers?.Authorization || config.headers?.authorization || ''
  return findAccountByToken(authorization)
}

function hasPermission(account, permission) {
  return account.permissions.includes('*:*:*') || account.permissions.includes(permission)
}

function forbidden() {
  return { code: 403, msg: '暂无权限执行此操作' }
}

const history = [
  {
    snapshotTime: '2026-09-15 13:50:00',
    qps: 0.42,
    requestCount: 25,
    avgCostMs: 18,
    maxCostMs: 96,
    error4xx: 0,
    error5xx: 0,
    inFlightMax: 2,
    dbActive: 3,
    dbMax: 50,
  },
  {
    snapshotTime: '2026-09-15 13:49:00',
    qps: 0.7,
    requestCount: 42,
    avgCostMs: 22,
    maxCostMs: 140,
    error4xx: 1,
    error5xx: 0,
    inFlightMax: 4,
    dbActive: 5,
    dbMax: 50,
  },
]

const hotUris = [
  { method: 'GET', uri: '/qt/book/list', requestCount: 30, qps: 0.25, avgCostMs: 16, maxCostMs: 80, error4xx: 0, error5xx: 0 },
  { method: 'POST', uri: '/login', requestCount: 8, qps: 0.07, avgCostMs: 40, maxCostMs: 120, error4xx: 1, error5xx: 0 },
]

export const metricsHandlers = [
  {
    method: 'get',
    path: '/qt/metrics/latest',
    handle(config) {
      const account = requireAccount(config)
      if (!account) return { code: 401, msg: '登录状态已失效' }
      if (!hasPermission(account, 'qt:metrics:list')) return forbidden()
      return {
        code: 200,
        msg: '操作成功',
        data: {
          snapshotTime: '2026-09-15 13:50:12',
          windowMs: 12000,
          global: {
            uri: '*',
            method: '*',
            requestCount: 8,
            qps: 0.67,
            avgCostMs: 19,
            maxCostMs: 88,
            error4xx: 0,
            error5xx: 0,
            inFlightMax: 2,
            jvmUsedMb: 380,
            jvmMaxMb: 1024,
            dbActive: 4,
            dbMax: 50,
          },
          topUris: hotUris,
        },
      }
    },
  },
  {
    method: 'get',
    path: '/qt/metrics/list',
    handle(config) {
      const account = requireAccount(config)
      if (!account) return { code: 401, msg: '登录状态已失效' }
      if (!hasPermission(account, 'qt:metrics:list')) return forbidden()
      return { code: 200, msg: '操作成功', data: history }
    },
  },
  {
    method: 'get',
    path: '/qt/metrics/uris',
    handle(config) {
      const account = requireAccount(config)
      if (!account) return { code: 401, msg: '登录状态已失效' }
      if (!hasPermission(account, 'qt:metrics:list')) return forbidden()
      return { code: 200, msg: '操作成功', data: hotUris }
    },
  },
]
