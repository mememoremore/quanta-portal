import { findAccountByToken } from '../data/accounts'

export const dashboardHandlers = [
  {
    method: 'get',
    path: '/dashboard/stats',
    handle(config) {
      const authorization = config.headers?.Authorization || config.headers?.authorization || ''
      if (!findAccountByToken(authorization)) {
        return { code: 401, msg: '登录状态已失效' }
      }
      return {
        code: 200,
        msg: '操作成功',
        data: {
          members: 5,
          resumesToday: 2,
          pendingPayments: 1,
        },
      }
    },
  },
]
