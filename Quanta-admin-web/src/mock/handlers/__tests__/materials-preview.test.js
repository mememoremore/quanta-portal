import { beforeEach, describe, expect, it } from 'vitest'
import { materialHandlers } from '../materials'
import { mockMaterials, resetMockMaterials } from '../../data/materials'

const detail = materialHandlers.find(handler => handler.method === 'get' && handler.match?.('/qt/materials/1'))
describe('mock material detail for independent preview', () => {
  beforeEach(resetMockMaterials)

  it('keeps local demo read permissions consistent with the existing material list', () => {
    const first = mockMaterials[0]
    expect(detail.handle({ path: `/qt/materials/${first.materialId}`, headers: { Authorization: 'Bearer mock-token-viewer' } })).toMatchObject({ code: 200, data: { materialId: first.materialId } })
  })

  it('rejects unknown sessions and returns a missing-file error', () => {
    expect(detail.handle({ path: '/qt/materials/1', headers: {} }).code).toBe(401)
    expect(detail.handle({ path: '/qt/materials/999999', headers: { Authorization: 'Bearer mock-token-viewer' } }).code).toBe(404)
  })
})
