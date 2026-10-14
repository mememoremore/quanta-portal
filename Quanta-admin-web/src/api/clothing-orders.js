import { request } from '@/utils/request'

export async function getClothingOrders(params = {}) {
  const response = await request({ url: '/qt/order/detailList', method: 'get', params })
  return { rows: response.rows || [], total: Number(response.total) || 0 }
}

export function approveClothingOrder(orderId) {
  return request({ url: `/qt/order/${orderId}/approve`, method: 'put' })
}
