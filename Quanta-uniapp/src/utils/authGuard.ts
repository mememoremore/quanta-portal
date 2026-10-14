import type { SessionSnapshot, UserRole } from '../types/session'
import { restoreSession, sessionState } from '../stores/user'

export const ROLE_SELECT_PATH = '/pages/login/select-role'

export const homeForRole = (role: UserRole) =>
  role === 'tower' ? '/pages/member/home/home' : '/pages/freshman/home'

export const normalizeRoutePath = (url: string) => {
  const path = (url || '').split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

export const resolveGuardTarget = (path: string, session: SessionSnapshot): string | null => {
  const normalizedPath = normalizeRoutePath(path)
  if (normalizedPath.startsWith('/pages/login/')) return null
  if (!session.token || !session.role) return ROLE_SELECT_PATH
  if (normalizedPath.startsWith('/pages/member/') && session.role !== 'tower') {
    return homeForRole(session.role)
  }
  if (normalizedPath.startsWith('/pages/freshman/') && session.role !== 'freshman') {
    return homeForRole(session.role)
  }
  return null
}

let installed = false
let redirecting = false

const redirectToTarget = (target: string) => {
  if (redirecting) return
  redirecting = true
  uni.reLaunch({
    url: target,
    complete: () => {
      redirecting = false
    },
  })
}

export const guardCurrentPage = () => {
  if (!sessionState.initialized) restoreSession()
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  if (!current?.route) return true
  const target = resolveGuardTarget(current.route, sessionState)
  if (!target) return true
  redirectToTarget(target)
  return false
}

export const installAuthGuard = () => {
  if (installed) return
  installed = true
  ;(['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'] as const).forEach((method) => {
    uni.addInterceptor(method, {
      invoke(args: { url?: string }) {
        if (!args?.url) return true
        const target = resolveGuardTarget(args.url, sessionState)
        if (!target || normalizeRoutePath(args.url) === target) return true
        redirectToTarget(target)
        return false
      },
    })
  })
}
