import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '../utils/request'
import { createDraftShirtOrder, mapClothingItem, parseOptionArray } from './clothing'

vi.mock('../utils/request', () => ({
  default: vi.fn(),
}))

describe('clothing backend mapping', () => {
  beforeEach(() => {
    vi.mocked(request).mockReset()
  })

  it('parses option arrays safely', () => {
    expect(parseOptionArray('["黑色","白色"]')).toEqual(['黑色', '白色'])
    expect(parseOptionArray('not-json')).toEqual([])
  })

  it('maps an active clothing item', () => {
    expect(mapClothingItem({ itemId: 2, effectImageUrl: '/profile/a.png', colorOptionsJson: '["黑色"]', sizeOptionsJson: '["L"]' })).toMatchObject({ itemId: 2, colors: ['黑色'], sizes: ['L'], price: null })
  })

  it('creates a draft order without client prices or status', async () => {
    vi.mocked(request).mockResolvedValue({ code: 200 })
    await expect(
      createDraftShirtOrder(
        { itemId: 2, name: 'Quanta 塔服', images: [], colors: ['黑色'], sizes: ['L'], price: 45 },
        { color: '黑色', size: 'L' },
      ),
    ).resolves.toEqual({ status: 'DRAFT' })
    const config = vi.mocked(request).mock.calls[0][0] as { data: Record<string, unknown> }
    expect(config.data).toEqual({
      itemId: 2,
      selectedColor: '黑色',
      selectedSize: 'L',
      quantity: 1,
    })
    expect(config.data).not.toHaveProperty('unitPrice')
    expect(config.data).not.toHaveProperty('totalAmount')
    expect(config.data).not.toHaveProperty('status')
    expect(config.data).not.toHaveProperty('orderNo')
  })
})
