import { request } from '@/utils/request'
import { displayPersonName } from '@/utils/person-name'

export async function getBookBorrowRecords(params = {}) {
  const response = await request({ url: '/qt/borrow/detailList', method: 'get', params })
  return {
    rows: (response.rows || []).map((row) => ({ ...row, nickName: displayPersonName(row) })),
    total: Number(response.total) || 0,
  }
}
