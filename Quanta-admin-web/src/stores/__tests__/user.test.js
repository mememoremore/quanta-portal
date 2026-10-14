import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '../user'
import { getToken } from '@/utils/token'
import { getAudience } from '@/utils/session-audience'
import * as authApi from '@/api/auth'

vi.mock('@/api/auth', () => ({
  login: vi.fn().mockResolvedValue({ code: 200, token: 'test-token' }),
  getInfo: vi.fn().mockResolvedValue({
    code: 200,
    user: { userName: 'tester', nickName: '测试员', isQuantaMember: '0' },
    roles: ['qt_manager'],
    permissions: ['qt:member:list'],
  }),
  logout: vi.fn().mockResolvedValue({ code: 200 }),
}))

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    authApi.login.mockClear()
  })

  it('submits the correct login type and persists audience only after success', async () => {
    const store = useUserStore()
    await store.login({ username: 'freshman', password: 'secret' }, 'freshman')
    expect(authApi.login).toHaveBeenCalledWith(expect.objectContaining({ loginType: '0' }))
    expect(getAudience()).toBe('freshman')

    await store.login({ username: 'member', password: 'secret' }, 'member')
    expect(authApi.login).toHaveBeenLastCalledWith(expect.objectContaining({ loginType: '1' }))
    expect(getAudience()).toBe('member')

    await store.login({ username: 'admin', password: 'secret' }, 'admin')
    expect(authApi.login).toHaveBeenLastCalledWith(expect.objectContaining({ loginType: '1' }))
    expect(getAudience()).toBe('admin')
  })

  it('persists token after login and stores user permissions', async () => {
    const store = useUserStore()
    await store.login({ username: 'tester', password: 'secret' }, 'member')
    await store.fetchUserInfo()

    expect(store.token).toBe('test-token')
    expect(getToken()).toBe('test-token')
    expect(store.displayName).toBe('测试员')
    expect(store.permissions).toEqual(['qt:member:list'])
  })

  it('clears the complete session', async () => {
    const store = useUserStore()
    await store.login({ username: 'tester', password: 'secret' }, 'member')
    await store.fetchUserInfo()
    store.reset()

    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(store.roles).toEqual([])
    expect(store.permissions).toEqual([])
    expect(getToken()).toBe('')
    expect(getAudience()).toBe('')
  })

  it('derives portal identity from getInfo instead of local audience', async () => {
    const store = useUserStore()
    await store.login({ username: 'tester', password: 'secret' }, 'freshman')
    await store.fetchUserInfo()
    expect(store.serverAudience).toBe('freshman')

    store.user.isQuantaMember = '1'
    expect(store.serverAudience).toBe('member')
  })

  it('does not treat an ordinary member permission as management access', () => {
    const store = useUserStore()
    store.user = { userName: 'member', isQuantaMember: '1' }
    store.roles = ['qt_member']
    store.permissions = ['system:user:profile']

    expect(store.serverAudience).toBe('member')
    expect(store.canAccessAdmin).toBe(false)
  })

  it('normalizes the real memberDepartment field for recruitment scope', () => {
    const store = useUserStore()
    store.user = { memberDepartment: '产品部' }
    expect(store.departmentCode).toBe('PRODUCT')

    store.user = { memberDepartment: 'BACKEND' }
    expect(store.departmentCode).toBe('BACKEND')

    store.user = { memberDepartment: 'ANDROID' }
    expect(store.departmentCode).toBe('')
  })
})
