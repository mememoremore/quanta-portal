import { beforeEach, describe, expect, it } from 'vitest'
import { materialHandlers } from '../materials'
import { resetMockMaterials } from '../../data/materials'

function upload(token, category, name = '资料.pdf') {
  const data = new FormData()
  data.append('file', new File(['ok'], name, { type: 'application/pdf' }))
  data.append('category', category)
  data.append('visibility', 'MEMBER')
  return materialHandlers[1].handle({ headers: { Authorization: `Bearer ${token}` }, data })
}

describe('material category mock permissions', () => {
  beforeEach(resetMockMaterials)

  it('lets a department manager upload only their own category', () => {
    expect(upload('mock-token-product-manager', 'PRODUCT').code).toBe(200)
    expect(upload('mock-token-product-manager', 'FRONTEND').code).toBe(403)
    expect(upload('mock-token-product-interviewer', 'PRODUCT').code).toBe(200)
    expect(upload('mock-token-product-interviewer', 'FRONTEND').code).toBe(403)
    expect(upload('mock-token-viewer', 'PRODUCT').code).toBe(403)
  })

  it('accepts markdown and allows an administrator to choose a category', () => {
    expect(upload('mock-token-admin', 'FRONTEND').code).toBe(200)
    expect(upload('mock-token-admin', 'FRONTEND', '笔记.md').code).toBe(200)
  })

  it('serves all standard categories to any authenticated member', () => {
    const result = materialHandlers[0].handle({ headers: { Authorization: 'Bearer mock-token-viewer' }, params: { categoryScope: 'STANDARD' } })
    expect(result.code).toBe(200)
    expect(new Set(result.rows.map((row) => row.category))).toEqual(new Set(['FRONTEND', 'BACKEND', 'PRODUCT', 'DESIGN']))
  })
})
