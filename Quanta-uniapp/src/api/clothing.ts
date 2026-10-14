import request from '../utils/request'
import type { ShirtSelection } from '../utils/memberMock'
import type { TableResponse } from './contracts'
import { resolveApiAssetUrl } from './mappers'

export interface ClothingItemDto {
  itemId: number
  itemName?: string
  effectImagePath?: string
  effectImageUrl?: string
  colorOptionsJson?: string
  sizeOptionsJson?: string
  status?: string
}

export interface ShirtProduct {
  itemId: number
  name: string
  images: string[]
  colors: string[]
  sizes: string[]
  price: number | null
}

export const parseOptionArray = (value = ''): string[] => {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string' && Boolean(item.trim())) : []
  } catch {
    return []
  }
}

export const mapClothingItem = (row: ClothingItemDto): ShirtProduct => {
  const image = resolveApiAssetUrl(row.effectImageUrl || row.effectImagePath || '')
  return {
    itemId: Number(row.itemId),
    name: row.itemName || 'Quanta 塔服',
    images: image ? [image] : [],
    colors: parseOptionArray(row.colorOptionsJson),
    sizes: parseOptionArray(row.sizeOptionsJson),
    price: null,
  }
}

export const getShirtProduct = async () => {
  const response = await request<TableResponse<ClothingItemDto>>({ url: '/qt/item/list', data: { status: '0', pageNum: 1, pageSize: 100 } })
  const item = (response.rows || []).find((row) => row.status === '0')
  if (!item) throw new Error('暂无上架塔服')
  return mapClothingItem(item)
}

export const createDraftShirtOrder = async (product: ShirtProduct, selection: ShirtSelection) => {
  await request({
    url: '/qt/order',
    method: 'POST',
    data: {
      itemId: product.itemId,
      selectedColor: selection.color,
      selectedSize: selection.size,
      quantity: 1,
    },
  })
  return { status: 'DRAFT' as const }
}
