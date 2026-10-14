import { findAccountByToken } from '../data/accounts'
import { mockClothingOrders } from '../data/clothing-orders'

const has = (account, permission) => account?.permissions.includes('*:*:*') || account?.permissions.includes(permission)
const account = (config) => findAccountByToken(config.headers?.Authorization || '')

export const clothingOrderHandlers = [
  {
    method: 'get', path: '/qt/order/detailList', handle(config) {
      const current = account(config)
      if (!current) return { code: 401, msg: '登录状态已失效' }
      if (!has(current, 'qt:order:list')) return { code: 403, msg: '没有查看塔服订单的权限' }
      const status = config.params?.status
      const rows = status ? mockClothingOrders.filter((item) => item.status === status) : mockClothingOrders
      return { code: 200, rows, total: rows.length }
    },
  },
  {
    method: 'put', match: (path) => /^\/system\/order\/\d+\/approve$/.test(path), handle(config) {
      const current = account(config)
      if (!current) return { code: 401, msg: '登录状态已失效' }
      if (!has(current, 'qt:order:approve')) return { code: 403, msg: '没有确认收款的权限' }
      const id = Number(config.path.split('/')[3])
      const order = mockClothingOrders.find((item) => item.orderId === id)
      if (!order) return { code: 404, msg: '订单不存在' }
      order.status = 'APPROVED'
      return { code: 200, msg: '收款确认成功' }
    },
  },
]
