import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.stubGlobal('uni', { getStorageSync: vi.fn(() => '') })

describe('member backend mapping', () => {
  beforeEach(() => vi.resetModules())

  it('maps a lab member row to the contacts model', async () => {
    const { mapLabMember } = await import('./member')
    expect(mapLabMember({
      userId: 103,
      nickName: '塔员小王',
      memberNo: 'Q101',
      memberDepartment: 'FRONTEND',
      memberTitle: '成员',
      memberCohort: '21st',
    })).toMatchObject({
      name: '塔员小王',
      code: 'Q101',
      department: '全栈(前端)',
      batch: '21st',
    })
  })
})
