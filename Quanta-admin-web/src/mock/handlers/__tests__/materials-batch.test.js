import { beforeEach, describe, expect, it } from 'vitest'
import { materialHandlers } from '../materials'
import { resetMockMaterials, mockMaterials } from '../../data/materials'
import { isMockApiRequest } from '@/config/mock-api'

const handler = materialHandlers.find(item => item.path === '/qt/materials/batch-delete')
function remove(token, materialIds) { return handler.handle({ headers: { Authorization: `Bearer ${token}` }, data: { materialIds } }) }

describe('full-demo material batch endpoint (not a production fallback)', () => {
  beforeEach(resetMockMaterials)
  it('allows ceo, management, and manager with delete permission, and still rejects admin or a manager without it', () => {
    expect(remove('mock-token-admin', [1]).code).toBe(403)
    expect(remove('mock-token-product-interviewer', [1]).code).toBe(403)
    expect(remove('', [1]).code).toBe(401)
    expect(mockMaterials).toHaveLength(4)
    expect(remove('mock-token-product-manager', [1]).code).toBe(200)
    expect(mockMaterials.map(row => row.materialId)).not.toContain(1)
  })
  it('validates first and safely handles a repeated deletion', () => {
    expect(remove('mock-token-ceo', [1, 1]).code).toBe(400)
    expect(mockMaterials).toHaveLength(4)
    expect(remove('mock-token-ceo', [1, 2]).data).toEqual({ deletedIds: [1, 2], alreadyDeletedIds: [] })
    expect(remove('mock-token-ceo', [1, 2]).data).toEqual({ deletedIds: [], alreadyDeletedIds: [1, 2] })
    expect(mockMaterials).toHaveLength(2)
  })
  it('is never allowlisted as a partial mock in real backend mode', () => {
    expect(isMockApiRequest({ method: 'post', url: '/qt/materials/batch-delete' })).toBe(false)
  })
})
