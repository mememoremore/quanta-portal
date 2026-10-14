import { describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { getMyServices } from '../services'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('member services API', () => {
  it('loads only current-user detail lists with Axios GET params', async () => {
    request
      .mockResolvedValueOnce({ rows: [{ borrowId: 3, bookId: 8, status: 'BORROWED' }] })
      .mockResolvedValueOnce({ rows: [{ orderId: 5, itemId: 4, status: 'DRAFT' }] })

    const result = await getMyServices()

    const params = { pageNum: 1, pageSize: 500 }
    expect(request.mock.calls.map(([config]) => config)).toEqual([
      { url: '/qt/borrow/detailList', method: 'get', params },
      { url: '/qt/order/detailList', method: 'get', params },
    ])
    expect(result).toEqual({
      borrows: [expect.objectContaining({ borrowId: 3, bookId: 8 })],
      orders: [expect.objectContaining({ orderId: 5, itemId: 4, status: 'DRAFT' })],
    })
  })
})
