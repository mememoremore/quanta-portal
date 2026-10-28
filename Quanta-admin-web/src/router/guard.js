import NProgress from 'nprogress'
import { pinia } from '@/stores'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { hasAnyPermission } from '@/composables/usePermission'
import { setUnauthorizedHandler } from '@/utils/unauthorized'
import { safeInternalRedirect } from '@/utils/redirect'
import { isDualTokenEnabled } from '@/utils/request'
import {
  audienceForPath,
  entryPathFor,
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
    // OAuth owns its inline login state and must retain the transaction.
    if (router.currentRoute.value.meta?.oauthInteraction) return
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

    if (to.meta?.oauthInteraction === true) return true
    const targetAudience = audienceForPath(to.path)

    if (publicPaths.has(to.path)) {
      if (targetAudience && userStore.token) {
        try {
          if (!userStore.user) await userStore.fetchUserInfo()
          // 已登录时按“入口归属”一次性算出落点：同域回该端首页、管理员进后台，
          // 其余情况留在登录页。跨入口访问不等于要求退出，所以这里不做任何登出：
          // 登出只能由用户显式触发（用户菜单），否则点错一次卡片就会丢掉整个会话。
          const entryPath = entryPathFor({
            audience: targetAudience,
            serverAudience: userStore.serverAudience,
            canAccessAdmin: userStore.canAccessAdmin,
          })
          // 落点与当前地址相同时直接放行，避免守卫返回同一地址造成无限重定向。
          return to.path === entryPath ? true : entryPath
        } catch {
          userStore.reset()
          permissionStore.resetRoutes()
        }
      }
      return true
    }

    if (!userStore.token && isDualTokenEnabled()) {
      await userStore.restoreSession()
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

      if (['freshman', 'member'].includes(targetAudience) && userStore.serverAudience !== targetAudience) {
        // Unknown identity is not an admin identity. The catch below preserves the requested portal.
        if (!['freshman', 'member'].includes(userStore.serverAudience)) throw new Error('无法确认账号身份')
        return homePathFor(userStore.serverAudience)
      }
      if (targetAudience === 'admin' && !userStore.canAccessAdmin) {
        await userStore.logout().catch(() => {})
        permissionStore.resetRoutes()
        return { path: '/admin/login', query: { denied: '1' } }
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
