import { describe, expect, it } from 'vitest'
import { prefixAdminRoutes } from '../permission'

describe('admin route normalization', () => {
  it('prefixes top-level management routes and absolute redirects with /admin', () => {
    const input = [
      {
        path: '/dashboard',
        redirect: '/dashboard/overview',
        children: [{ path: 'overview' }],
      },
      { path: '/members' },
    ]

    expect(prefixAdminRoutes(input)).toEqual([
      {
        path: '/admin/dashboard',
        redirect: '/admin/dashboard/overview',
        children: [{ path: 'overview' }],
      },
      { path: '/admin/members' },
    ])
  })

  it('is idempotent and does not mutate the source routes', () => {
    const input = [{ path: '/admin/dashboard' }]

    expect(prefixAdminRoutes(input)).toEqual(input)
    expect(prefixAdminRoutes(input)).not.toBe(input)
  })
})
