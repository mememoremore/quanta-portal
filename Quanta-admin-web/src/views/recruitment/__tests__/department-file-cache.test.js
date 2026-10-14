import { describe, expect, it } from 'vitest'
import { createDepartmentFileCache } from '../department-file-cache'

describe('department file cache', () => {
  it('isolates cached files by department and supports clearing', () => {
    const cache = createDepartmentFileCache()
    const frontend = new File(['front'], 'frontend.png', { type: 'image/png' })
    const backend = new File(['back'], 'backend.png', { type: 'image/png' })

    cache.set('FRONTEND', frontend)
    cache.set('BACKEND', backend)

    expect(cache.get('FRONTEND')).toBe(frontend)
    expect(cache.get('BACKEND')).toBe(backend)

    cache.clear('FRONTEND')

    expect(cache.get('FRONTEND')).toBeNull()
    expect(cache.get('BACKEND')).toBe(backend)
  })
})
