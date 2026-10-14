import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { requestBlob } from '@/utils/download'
import {
  downloadPortalMaterial,
  getPortalMaterials,
  getTopNotices,
  markNoticeRead,
} from '../content'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))
vi.mock('@/utils/download', () => ({ requestBlob: vi.fn() }))

describe('member portal content API', () => {
  beforeEach(() => {
    request.mockReset()
    requestBlob.mockReset()
  })

  it('loads top notices and preserves the top-level unread count', async () => {
    request.mockResolvedValue({ data: [{ noticeId: 3, noticeTitle: '例会通知' }], unreadCount: '1' })
    await expect(getTopNotices()).resolves.toEqual({
      notices: [expect.objectContaining({ id: 3, title: '例会通知' })],
      unreadCount: 1,
    })
    expect(request).toHaveBeenCalledWith({ url: '/system/notice/listTop', method: 'get' })
  })

  it('marks a notice read with POST params rather than a request body', async () => {
    request.mockResolvedValue({ code: 200 })
    await markNoticeRead(3)
    expect(request).toHaveBeenCalledWith({
      url: '/system/notice/markRead',
      method: 'post',
      params: { noticeId: 3 },
    })
  })

  it('loads materials with GET params and downloads through requestBlob', async () => {
    request.mockResolvedValue({ rows: [{ materialId: 5, fileName: '学习资料.pdf' }], total: 1 })
    const result = await getPortalMaterials({ category: 'FRONTEND' })
    expect(request).toHaveBeenCalledWith({
      url: '/qt/materials',
      method: 'get',
      params: { pageNum: 1, pageSize: 20, categoryScope: 'STANDARD', category: 'FRONTEND' },
    })
    requestBlob.mockResolvedValue({ blob: new Blob(['ok']), fileName: '学习资料.pdf' })
    await downloadPortalMaterial(result.rows[0])
    expect(requestBlob).toHaveBeenCalledWith(
      { url: '/qt/materials/5/download', method: 'get' },
      '学习资料.pdf',
    )
  })
})
