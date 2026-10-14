import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { borrowBook, getBooks } from '../library'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('member library API', () => {
  beforeEach(() => request.mockReset())

  it('loads books with Axios GET params and maps availability', async () => {
    request.mockResolvedValue({
      rows: [
        {
          bookId: '8',
          bookName: 'Vue.js 设计与实现',
          totalCount: '2',
          availableCount: '1',
          status: '0',
          bookType: 'TEXTBOOK',
        },
      ],
      total: 1,
    })

    await expect(getBooks({ bookName: 'Vue' })).resolves.toEqual({
      rows: [
        expect.objectContaining({
          bookId: 8,
          totalCount: 2,
          availableCount: 1,
          available: true,
          bookType: 'TEXTBOOK',
        }),
      ],
      total: 1,
    })
    expect(request).toHaveBeenCalledWith({
      url: '/qt/book/list',
      method: 'get',
      params: { pageNum: 1, pageSize: 20, bookName: 'Vue' },
    })
  })

  it('submits dueTime as yyyy-MM-dd only', async () => {
    request.mockResolvedValue({ code: 200 })

    await borrowBook('8', '2026-10-01 23:59:59')

    expect(request).toHaveBeenCalledWith({
      url: '/qt/borrow',
      method: 'post',
      data: { bookId: 8, dueTime: '2026-10-01' },
    })
  })
})
