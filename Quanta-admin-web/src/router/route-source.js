import { isKnownComponent } from './component-map'
import { mockRouters as localWebRoutes } from '@/mock/data/routers'

function withoutRetiredRoutes(routes = []) {
  return routes.flatMap((route) => {
    if (route.component === 'workstations/index' || route.path === '/workstations') return []
    const children = withoutRetiredRoutes(route.children || [])
    if (route.children?.length && !children.length && route.component === 'Layout') return []
    return [{ ...route, ...(route.children ? { children } : {}) }]
  })
}

function containsSupportedPage(routes = []) {
  return routes.some((route) => {
    const isPage = route.component !== 'Layout' && isKnownComponent(route.component)
    return isPage || containsSupportedPage(route.children || [])
  })
}

function canAccessRoute(route, permissions = []) {
  const required = route.meta?.permission
  return !required || permissions.includes('*:*:*') || permissions.includes(required)
}

export function selectWebRoutes(backendRoutes = [], permissions = []) {
  const activeBackendRoutes = withoutRetiredRoutes(backendRoutes)
  if (containsSupportedPage(activeBackendRoutes)) return activeBackendRoutes
  return localWebRoutes.filter((route) => canAccessRoute(route, permissions))
}

