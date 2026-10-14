import { resolveRouteComponent } from './component-map'

export function transformRoute(rawRoute) {
  const route = {
    path: rawRoute.path,
    name: rawRoute.name,
    component: resolveRouteComponent(rawRoute.component),
    meta: { ...(rawRoute.meta || {}) },
  }

  if (rawRoute.redirect) route.redirect = rawRoute.redirect
  if (rawRoute.hidden !== undefined) route.meta.hidden = rawRoute.hidden
  if (rawRoute.alwaysShow !== undefined) route.meta.alwaysShow = rawRoute.alwaysShow
  if (rawRoute.children?.length) {
    route.children = rawRoute.children.map(transformRoute)
  }
  return route
}

export function transformRoutes(rawRoutes = []) {
  return rawRoutes.map(transformRoute)
}

