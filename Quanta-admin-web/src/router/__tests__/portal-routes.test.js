import { describe, expect, it } from 'vitest'
import viteConfig from '../../../vite.config.js'
import { portalRoutes } from '../portal-routes'
import { staticRoutes } from '../routes'

describe('portal route contract', () => {
  it('exposes the public entry and the three confirmed route domains', () => {
    expect(staticRoutes.find((route) => route.path === '/')).toBeTruthy()
    expect(staticRoutes.find((route) => route.path === '/admin')).toBeTruthy()
    expect(staticRoutes.find((route) => route.path === '/admin/login')).toBeTruthy()
    expect(staticRoutes.find((route) => route.path === '/login/freshman')).toBeTruthy()
    expect(staticRoutes.find((route) => route.path === '/login/member')).toBeTruthy()
    expect(portalRoutes.map((route) => route.path)).toEqual(['/freshman', '/member'])
  })

  it('keeps the legacy login URL as an admin-login redirect', () => {
    expect(staticRoutes.find((route) => route.path === '/login')).toMatchObject({
      redirect: '/admin/login',
    })
  })

  it('labels every portal page with its audience', () => {
    portalRoutes.forEach((route) => {
      expect(['freshman', 'member']).toContain(route.meta.portalAudience)
      route.children
        .filter((child) => child.component)
        .forEach((child) => {
          expect(child.meta.portalAudience).toBe(route.meta.portalAudience)
        })
    })
  })

  it('includes the confirmed security and member material destinations', () => {
    const freshman = portalRoutes.find((route) => route.path === '/freshman')
    const member = portalRoutes.find((route) => route.path === '/member')

    expect(freshman.children.map((child) => child.path)).toContain('security')
    expect(member.children.map((child) => child.path)).toEqual(
      expect.arrayContaining(['materials', 'security']),
    )
    expect(member.children.map((child) => child.path)).not.toContain('notifications')
  })

  it('builds production assets from the site root', () => {
    expect(viteConfig({ mode: 'production' }).base).toBe('/')
  })
})
