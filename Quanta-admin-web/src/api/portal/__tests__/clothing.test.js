import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { createClothingOrder, getClothingItems } from '../clothing'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('member clothing API', () => {
  beforeEach(() => request.mockReset())

  it('loads active clothing items with Axios GET params and no invented price', async () => {
    request.mockResolvedValue({
      rows: [
        {
          itemId: '4',
          itemName: 'Quanta 塔服',
          effectImageUrl: 'https://www.quantacenter.com/profile/shirt.png',
          colorOptionsJson: '["黑色","白色"]',
          sizeOptionsJson: '["M","L"]',
          status: '0',
        },
      ],
    })

    const items = await getClothingItems()

    expect(items[0]).toEqual(
      expect.objectContaining({
        itemId: 4,
        colors: ['黑色', '白色'],
        sizes: ['M', 'L'],
        price: null,
        priceLabel: '以实物通知为准',
      }),
    )
    expect(request).toHaveBeenCalledWith({
      url: '/qt/item/list',
      method: 'get',
      params: { status: '0', pageNum: 1, pageSize: 100 },
    })
  })

  it('creates only a DRAFT order when the backend item has no price', async () => {
    request.mockResolvedValue({ code: 200 })

    await expect(
      createClothingOrder(
        { itemId: 4, price: null },
        { color: '黑色', size: 'L', quantity: 2 },
      ),
    ).resolves.toEqual(expect.objectContaining({ status: 'DRAFT' }))

    const config = request.mock.calls[0][0]
    expect(config).toMatchObject({
      url: '/qt/order',
      method: 'post',
      data: {
        itemId: 4,
        selectedColor: '黑色',
        selectedSize: 'L',
        quantity: 2,
        status: 'DRAFT',
      },
    })
    expect(config.data).not.toHaveProperty('unitPrice')
    expect(config.data).not.toHaveProperty('totalAmount')
  })
})
