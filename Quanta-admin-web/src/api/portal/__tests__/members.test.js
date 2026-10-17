import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { getCurrentProfile, getMemberDirectory, getOwnMemberProfile, saveMemberProfile, getPublicMemberProfile, uploadMemberAvatar } from '../members'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('member portal profile API', () => {
  beforeEach(() => request.mockReset())

  it('saves only editable cloud fields, never login email or identity', async () => {
    request.mockResolvedValue({ code: 200 })
    await saveMemberProfile({ headline: 'Hi', userId: 9, memberDepartment: 'CEO', email: 'login@private.com', skills: ['Vue'], contactEmail: 'me@example.com' })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({ url: '/qt/member/profile', method: 'put', data: { headline: 'Hi', bio: '', skills: ['Vue'], githubUrl: '', websiteUrl: '', contactEmail: 'me@example.com', showContactEmail: false } }))
  })
  it('loads the two distinct private/public endpoints', async () => {
    request.mockResolvedValue({ data: { userId: 9, nickName: '王同学', bio: '公开简介' } })
    expect((await getOwnMemberProfile()).bio).toBe('公开简介')
    await getPublicMemberProfile(9)
    expect(request.mock.calls.map(([config]) => config.url)).toEqual(['/qt/member/profile', '/qt/member/9/profile'])
  })
  it('uploads avatarfile and refuses missing image URLs', async () => {
    request.mockResolvedValueOnce({ imgUrl: '/profile/avatar/a.jpg' }).mockResolvedValueOnce({ code: 200 })
    const file = new File(['jpeg'], 'avatar.jpg', { type: 'image/jpeg' })
    expect(await uploadMemberAvatar(file)).toContain('/profile/avatar/a.jpg')
    expect(request.mock.calls[0][0].data.get('avatarfile')).toBe(file)
    await expect(uploadMemberAvatar(file)).rejects.toThrow('图片地址')
  })
  it('exposes only public profile fields even if a server returns extra identity data', async () => {
    request.mockResolvedValue({data:{userId:9,nickName:'王',email:'login@example.com',phonenumber:'13800000000',userName:'private-login',memberNo:'QT009',contactEmail:'private@example.com',showContactEmail:false}})
    const profile = await getPublicMemberProfile(9)
    expect(profile).not.toHaveProperty('phone')
    expect(profile).not.toHaveProperty('userName')
    expect(profile).not.toHaveProperty('memberNo')
    expect(profile.contactEmail).toBe('')
  })

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
