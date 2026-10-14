import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getRouters } from '@/api/auth'
import { transformRoutes } from '@/router/route-transformer'
import { selectWebRoutes } from '@/router/route-source'

function withAdminPrefix(path) {
  if (typeof path !== 'string' || !path.startsWith('/')) return path
  if (path === '/admin' || path.startsWith('/admin/')) return path
  return `/admin${path}`
}

function prefixAdminRoute(route, topLevel = false) {
  const normalized = {
    ...route,
    path: topLevel || route.path?.startsWith('/') ? withAdminPrefix(route.path) : route.path,
  }
  if (typeof route.redirect === 'string') {
    normalized.redirect = withAdminPrefix(route.redirect)
  }
  if (route.children) {
    normalized.children = route.children.map((child) => prefixAdminRoute(child))
  }
  return normalized
}

export function prefixAdminRoutes(routes = []) {
  return routes.map((route) => prefixAdminRoute(route, true))
}

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref([])
  const initialized = ref(false)
  const initializing = ref(null)
  const routeRemovers = []

  async function generateRoutes(permissions = []) {
    if (initialized.value) return routes.value
    if (initializing.value) return initializing.value

    initializing.value = getRouters()
      .then((response) => {
        routes.value = prefixAdminRoutes(
          transformRoutes(selectWebRoutes(response.data || [], permissions)),
        )
        return routes.value
      })
      .finally(() => {
        initializing.value = null
      })

    return initializing.value
  }

  function installRoutes(router) {
    if (initialized.value) return
    routes.value.forEach((route) => {
      if (route.name && router.hasRoute(route.name)) router.removeRoute(route.name)
      routeRemovers.push(router.addRoute('Root', route))
    })
    initialized.value = true
  }

  function resetRoutes() {
    while (routeRemovers.length) {
      routeRemovers.pop()?.()
    }
    routes.value = []
    initialized.value = false
    initializing.value = null
  }

  return {
    routes,
    initialized,
    initializing,
    generateRoutes,
    installRoutes,
    resetRoutes,
  }
})
