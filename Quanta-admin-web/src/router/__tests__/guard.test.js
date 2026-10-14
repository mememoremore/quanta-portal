import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { pinia } from '@/stores'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { staticRoutes } from '../routes'
import { buildDocumentTitle, setupRouterGuard } from '../guard'
import { setAudience } from '@/utils/session-audience'
import { setToken } from '@/utils/token'

function createTestRouter() {
  const routes = staticRoutes.map((route) => ({
    ...route,
    component: { template: '<div />' },
  }))
  const router = createRouter({
    history: createMemoryHistory(),
    routes,
  })
  setupRouterGuard(router)
  return router
}

describe('global route guard', () => {
  let userStore
  let permissionStore

  beforeEach(() => {
    userStore = useUserStore(pinia)
    permissionStore = usePermissionStore(pinia)
    userStore.reset()
    permissionStore.resetRoutes()
  })

  it('uses concise titles for each portal audience', () => {
    expect(buildDocumentTitle({ path: '/', meta: {} })).toBe('Quanta 社团门户')
    expect(buildDocumentTitle({ path: '/freshman/home', meta: { portalAudience: 'freshman' } })).toBe('新生门户 - Quanta')
    expect(buildDocumentTitle({ path: '/member/home', meta: { portalAudience: 'member' } })).toBe('塔员中心 - Quanta')
    expect(buildDocumentTitle({ path: '/admin/dashboard', meta: {} })).toBe('管理后台 - Quanta')
  })

  it('redirects unauthenticated visitors and keeps the intended path', async () => {
    const router = createTestRouter()
    await router.push('/members')

    expect(router.currentRoute.value.path).toBe('/admin/login')
    expect(router.currentRoute.value.query.redirect).toBe('/members')
  })

  it('uses the target portal login for unauthenticated visitors', async () => {
    const router = createTestRouter()
    await router.push('/freshman/home')
    expect(router.currentRoute.value.path).toBe('/login/freshman')
    expect(router.currentRoute.value.query.redirect).toBe('/freshman/home')
  })

  it('restores user info and installs prefixed dynamic routes on a cold deep link', async () => {
    const router = createTestRouter()
    await userStore.login({ username: 'admin', password: 'quanta123' }, 'admin')
    await router.push('/admin/dashboard')

    expect(router.currentRoute.value.name).toBe('Dashboard')
    expect(userStore.displayName).toBe('李明华')
    expect(permissionStore.initialized).toBe(true)
    expect(router.hasRoute('Members')).toBe(true)
    expect(permissionStore.routes.every((route) => route.path.startsWith('/admin/'))).toBe(true)
  }, 30000)

  it('sends a logged-in user to 403 when page permission is missing', async () => {
    const router = createTestRouter()
    router.addRoute('Root', {
      path: '/admin-only',
      name: 'AdminOnly',
      component: { template: '<div>admin only</div>' },
      meta: { permission: 'system:secret:manage' },
    })
    await userStore.login({ username: 'viewer', password: 'quanta123' }, 'admin')
    await router.push('/admin-only')

    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('trusts server member identity rather than a tampered local audience', async () => {
    const router = createTestRouter()
    setAudience('member')
    setToken('test-token')
    userStore.token = 'test-token'
    userStore.user = { userName: 'freshman', isQuantaMember: '0' }
    await router.push('/member/home')
    expect(router.currentRoute.value.path).toBe('/freshman/home')
  })

  it('clears a member session when opening the freshman login entry', async () => {
    const router = createTestRouter()
    setAudience('member')
    setToken('test-token')
    userStore.token = 'test-token'
    userStore.user = { userName: 'member', isQuantaMember: '1' }

    await router.push('/login/freshman')

    expect(router.currentRoute.value.path).toBe('/login/freshman')
    expect(userStore.token).toBe('')
    expect(userStore.user).toBeNull()
  })

  it('allows a member with management permissions into admin routes', async () => {
    const router = createTestRouter()
    await userStore.login(
      { username: 'product_interviewer', password: 'quanta123' },
      'member',
    )
    userStore.user = { userName: 'manager', isQuantaMember: '1' }
    userStore.roles = ['qt_manager']
    userStore.permissions = ['qt:interview:admin:list']
    await router.push('/admin/recruitment')
    expect(router.currentRoute.value.path).toBe('/admin/recruitment')
  })
})
