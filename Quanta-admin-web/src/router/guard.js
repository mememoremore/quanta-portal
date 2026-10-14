import NProgress from 'nprogress'
import { pinia } from '@/stores'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { hasAnyPermission } from '@/composables/usePermission'
import { setUnauthorizedHandler } from '@/utils/unauthorized'
import { safeInternalRedirect } from '@/utils/redirect'
import {
  audienceForPath,
  getAudience,
  homePathFor,
  loginPathFor,
} from '@/utils/session-audience'

const publicPaths = new Set([
  '/',
  '/login',
  '/login/freshman',
  '/login/member',
  '/admin/login',
  '/403',
])

const audienceTitles = {
  freshman: '新生门户 - Quanta',
  member: '塔员中心 - Quanta',
  admin: '管理后台 - Quanta',
}

export function buildDocumentTitle(route) {
  if (route.path === '/') return 'Quanta 社团门户'
  const audience = route.meta?.portalAudience || audienceForPath(route.path)
  if (audienceTitles[audience]) return audienceTitles[audience]
  return route.meta?.title ? `${route.meta.title} - Quanta` : 'Quanta 社团门户'
}

export function setupRouterGuard(router) {
  NProgress.configure({ showSpinner: false })

  setUnauthorizedHandler(async () => {
    const userStore = useUserStore(pinia)
    const permissionStore = usePermissionStore(pinia)
    const redirect = router.currentRoute.value.fullPath
    const targetAudience = audienceForPath(redirect) || getAudience() || 'admin'
    userStore.reset()
    permissionStore.resetRoutes()
    const loginPath = loginPathFor(targetAudience)
    if (router.currentRoute.value.path !== loginPath) {
      await router.replace({
        path: loginPath,
        query: redirect === safeInternalRedirect(redirect, '') ? { redirect } : {},
      })
    }
  })

  router.beforeEach(async (to) => {
    NProgress.start()
    const userStore = useUserStore(pinia)
    const permissionStore = usePermissionStore(pinia)

    const targetAudience = audienceForPath(to.path)

    if (publicPaths.has(to.path)) {
      if (targetAudience && userStore.token) {
        try {
          if (!userStore.user) await userStore.fetchUserInfo()
          if (targetAudience === 'admin' && userStore.canAccessAdmin) {
            return homePathFor('admin')
          }
          if (targetAudience === userStore.serverAudience) {
            return homePathFor(targetAudience)
          }
          userStore.reset()
          permissionStore.resetRoutes()
          return true
        } catch {
          userStore.reset()
          permissionStore.resetRoutes()
        }
      }
      return true
    }

    if (!userStore.token) {
      return {
        path: loginPathFor(targetAudience || 'admin'),
        query:
          to.fullPath === safeInternalRedirect(to.fullPath, '')
            ? { redirect: to.fullPath }
            : {},
      }
    }

    try {
      if (!userStore.user) await userStore.fetchUserInfo()

      if (targetAudience === 'freshman' && userStore.serverAudience !== 'freshman') {
        return homePathFor(userStore.serverAudience || 'admin')
      }
      if (targetAudience === 'member' && userStore.serverAudience !== 'member') {
        return homePathFor(userStore.serverAudience || 'admin')
      }
      if (targetAudience === 'admin' && !userStore.canAccessAdmin) {
        return homePathFor(userStore.serverAudience || 'admin')
      }

      if (targetAudience === 'admin' && !permissionStore.initialized) {
        await permissionStore.generateRoutes(userStore.permissions)
        permissionStore.installRoutes(router)
        return { path: to.fullPath, replace: true }
      }

      const required = to.meta?.permission
      if (required && !hasAnyPermission(required, userStore.permissions)) {
        return '/403'
      }
      return true
    } catch {
      userStore.reset()
      permissionStore.resetRoutes()
      return {
        path: loginPathFor(targetAudience || getAudience() || 'admin'),
        query:
          to.fullPath === safeInternalRedirect(to.fullPath, '')
            ? { redirect: to.fullPath }
            : {},
      }
    }
  })

  router.afterEach((to) => {
    document.title = buildDocumentTitle(to)
    NProgress.done()
  })

  router.onError(() => NProgress.done())
}
