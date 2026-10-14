import { beforeEach, describe, expect, it } from 'vitest'
import { materialHandlers } from '../materials'
import { clothingOrderHandlers } from '../clothing-orders'
import { resetMockClothingOrders } from '@/mock/data/clothing-orders'

const config = (token, extra = {}) => ({ headers: { Authorization: `Bearer ${token}` }, ...extra })

describe('materials and clothing permissions', () => {
  beforeEach(resetMockClothingOrders)

  it('keeps manager materials read-only and hides order access', () => {
    expect(materialHandlers[0].handle(config('mock-token-product-interviewer')).code).toBe(200)
    const removeHandler = materialHandlers.find((item) => item.method === 'delete')
    expect(removeHandler.handle(config('mock-token-product-interviewer', { path: '/qt/materials/1' })).code).toBe(403)
    expect(clothingOrderHandlers[0].handle(config('mock-token-product-interviewer')).code).toBe(403)
  })

  it('allows management to confirm a submitted order', () => {
    const response = clothingOrderHandlers[1].handle(config('mock-token-product-manager', { path: '/qt/order/6/approve' }))
    expect(response.code).toBe(200)
  })
})
