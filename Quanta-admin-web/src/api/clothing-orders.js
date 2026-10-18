import { request } from '@/utils/request'
import { displayPersonName } from '@/utils/person-name'

export async function getClothingOrders(params = {}) {
  const response = await request({ url: '/qt/order/detailList', method: 'get', params })
  return {
    rows: (response.rows || []).map((row) => ({ ...row, nickName: displayPersonName(row) })),
    total: Number(response.total) || 0,
  }
}

export function approveClothingOrder(orderId) {
  return request({ url: `/qt/order/${orderId}/approve`, method: 'put' })
}
