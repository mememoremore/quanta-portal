import { describe, expect, it } from 'vitest'
import { isKnownComponent, unknownRouteComponent } from '../component-map'
import { transformRoute, transformRoutes } from '../route-transformer'
import { selectWebRoutes } from '../route-source'

describe('route transformer', () => {
  it('transforms nested RuoYi route fields', () => {
    const [route] = transformRoutes([
      {
        path: '/parent',
        name: 'Parent',
        component: 'Layout',
        redirect: '/parent/child',
        alwaysShow: true,
        meta: { title: '父级' },
        children: [
          {
            path: 'child',
            name: 'Child',
            component: 'placeholder/index',
            hidden: true,
            meta: { title: '子级' },
          },
        ],
      },
    ])

    expect(route.name).toBe('Parent')
    expect(route.redirect).toBe('/parent/child')
    expect(route.meta.alwaysShow).toBe(true)
    expect(route.children[0].meta.hidden).toBe(true)
  })

  it('maps unknown component paths to a safe error page', () => {
    const route = transformRoute({
      path: '/unsafe',
      name: 'Unsafe',
      component: '../../arbitrary-file',
    })
    expect(route.component).toBe(unknownRouteComponent)
    expect(isKnownComponent('../../arbitrary-file')).toBe(false)
  })

  it('recognizes the recruitment page component', () => {
    expect(isKnownComponent('recruitment/index')).toBe(true)
    expect(isKnownComponent('activity-signups/index')).toBe(true)
    expect(isKnownComponent('workstations/index')).toBe(false)
    expect(isKnownComponent('sharing-signups/index')).toBe(false)
    expect(isKnownComponent('lecture-signups/index')).toBe(false)
    expect(isKnownComponent('book-borrows/index')).toBe(true)
    expect(isKnownComponent('learning-materials/index')).toBe(true)
    expect(isKnownComponent('clothing-orders/index')).toBe(true)
    expect(isKnownComponent('metrics/index')).toBe(true)
  })

  it('uses the local Web route catalog when backend menus contain no supported page', () => {
    const backendRoutes = [
      {
        path: '/system',
        name: 'System',
        component: 'Layout',
        children: [{ path: 'user', name: 'User', component: 'system/user/index' }],
      },
    ]

    const routes = selectWebRoutes(backendRoutes, ['*:*:*'])

    expect(routes.map((route) => route.name)).toContain('Dashboard')
    expect(routes.map((route) => route.name)).toContain('Members')
    expect(routes.map((route) => route.name)).not.toContain('System')
  })

  it('filters the local Web route catalog with real backend permissions', () => {
    const routes = selectWebRoutes([], ['qt:dashboard:stats', 'qt:borrow:list'])

    expect(routes.map((route) => route.name)).toEqual(['Dashboard', 'BookBorrows'])
  })

  it('drops retired workstation routes supplied by the backend', () => {
    const routes = selectWebRoutes([
      { path: '/workstations', component: 'workstations/index', name: 'Workstations' },
      { path: '/dashboard', component: 'dashboard/index', name: 'Dashboard' },
    ])
    expect(routes.map((route) => route.name)).toEqual(['Dashboard'])
  })
})
