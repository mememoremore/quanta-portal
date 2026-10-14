import { beforeEach, describe, expect, it } from 'vitest'
import { getMaterials, mapMaterial } from '../materials'
import { getClothingOrders } from '../clothing-orders'
import { setToken } from '@/utils/token'

describe('materials and clothing APIs', () => {
  beforeEach(() => setToken('mock-token-product-manager'))

  it('loads learning materials for management', async () => {
    expect((await getMaterials()).rows[0].category).toBe('FRONTEND')
  })

  it('loads clothing orders for management', async () => {
    expect((await getClothingOrders()).rows.some((row) => row.status === 'SUBMITTED')).toBe(true)
  })

  it('maps backend material fields to the page model', () => {
    expect(
      mapMaterial({
        materialId: 1,
        fileName: '设计规范.pdf',
        uploaderName: '张晓雪',
        uploadTime: '2026-08-30 09:00:00',
        downloadUrl: '/qt/materials/1/download',
      }),
    ).toMatchObject({
      uploader: '张晓雪',
      uploadedAt: '2026-08-30 09:00:00',
      downloadUrl: '/qt/materials/1/download',
    })
  })
})
