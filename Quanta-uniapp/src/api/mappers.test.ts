import { describe, expect, it } from 'vitest'
import { API_BASE_URL } from '../config/runtime'
import { mapSysUserProfile, resolveApiAssetUrl } from './mappers'

describe('shared backend mappers', () => {
  it('resolves relative backend asset paths', () => {
    expect(resolveApiAssetUrl('/profile/upload/a.png')).toBe(`${API_BASE_URL}/profile/upload/a.png`)
    expect(resolveApiAssetUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
    expect(resolveApiAssetUrl('/static/picture/a.png')).toBe('/static/picture/a.png')
    expect(resolveApiAssetUrl('http://127.0.0.1:8080/profile/profile/upload/a.png')).toBe(`${API_BASE_URL}/profile/upload/a.png`)
  })

  it('maps the RuoYi user into the mini-program profile', () => {
    expect(mapSysUserProfile({
      userId: 103,
      userName: 'qt_member',
      nickName: '塔员小王',
      memberDepartment: 'FRONTEND',
      memberCohort: '21st',
    }, 'tower')).toMatchObject({
      id: '103',
      account: 'qt_member',
      name: '塔员小王',
      department: '全栈(前端)',
      batch: '21st',
    })
  })
})
