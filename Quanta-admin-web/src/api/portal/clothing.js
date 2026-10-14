import { request } from '@/utils/request'

function parseOptions(value) {
  try {
    const parsed = JSON.parse(value || '[]')
    return Array.isArray(parsed)
      ? parsed.filter((item) => typeof item === 'string' && item.trim())
      : []
  } catch {
    return []
  }
}

function optionalPrice(row) {
  const source = row.unitPrice ?? row.price
  if (source === null || source === undefined || source === '') return null
  const price = Number(source)
  return Number.isFinite(price) && price >= 0 ? price : null
}

function mapClothingItem(row = {}) {
  const price = optionalPrice(row)
  return {
    itemId: Number(row.itemId),
    itemName: row.itemName || 'Quanta 塔服',
    imageUrl: row.effectImageUrl || row.effectImagePath || '',
    colors: parseOptions(row.colorOptionsJson),
    sizes: parseOptions(row.sizeOptionsJson),
    status: row.status || '',
    price,
    priceLabel: price === null ? '以实物通知为准' : `¥${price.toFixed(2)}`,
  }
}

export async function getClothingItems() {
  const response = await request({
    url: '/qt/item/list',
    method: 'get',
    params: { status: '0', pageNum: 1, pageSize: 100 },
  })
  return (response.rows || []).filter((row) => row.status === '0').map(mapClothingItem)
}

export async function createClothingOrder(item, selection) {
  const quantity = Math.max(1, Number(selection.quantity || 1))
  const price = optionalPrice(item)
  const data = {
    itemId: Number(item.itemId),
    selectedColor: selection.color,
    selectedSize: selection.size,
    quantity,
    status: 'DRAFT',
  }
  if (price !== null) {
    data.unitPrice = price
    data.totalAmount = price * quantity
  }
  const response = await request({ url: '/qt/order', method: 'post', data })
  return { status: 'DRAFT', response }
}
