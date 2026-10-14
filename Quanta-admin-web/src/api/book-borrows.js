import { request } from '@/utils/request'

export async function getBookBorrowRecords(params = {}) {
  const response = await request({ url: '/qt/borrow/detailList', method: 'get', params })
  return { rows: response.rows || [], total: Number(response.total) || 0 }
}
