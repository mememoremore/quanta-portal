import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { getCurrentProfile, getMemberDirectory } from '../members'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('member portal profile API', () => {
  beforeEach(() => request.mockReset())

  it('loads the member directory with Axios GET params and maps member fields', async () => {
    request.mockResolvedValue({
      rows: [{
        userId: '7',
        nickName: '李同学',
        memberNo: 'QT007',
        memberDepartment: 'PRODUCT',
        memberStatus: 'ACTIVE',
        isQuantaMember: '1',
      }],
      total: 1,
    })

    await expect(getMemberDirectory({ nickName: '李' })).resolves.toEqual({
      rows: [expect.objectContaining({
        id: 7,
        name: '李同学',
        memberNo: 'QT007',
        department: '产品',
        isActive: true,
      })],
      total: 1,
    })
    expect(request).toHaveBeenCalledWith({
      url: '/qt/member/list',
      method: 'get',
      params: { pageNum: 1, pageSize: 500, nickName: '李' },
    })
  })

  it('maps the current user from getInfo', async () => {
    request.mockResolvedValue({
      user: { userId: 9, nickName: '王同学', memberNo: 'QT009', memberDepartment: 'FRONTEND' },
      roles: ['qt_member'],
    })

    await expect(getCurrentProfile()).resolves.toMatchObject({
      id: 9,
      name: '王同学',
      department: '前端',
      roles: ['qt_member'],
    })
    expect(request).toHaveBeenCalledWith({ url: '/getInfo', method: 'get' })
  })
})
