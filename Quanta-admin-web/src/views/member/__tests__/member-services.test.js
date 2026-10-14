import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MemberServices from '@/views/member/services/index.vue'
import MemberLibrary from '@/views/member/library/index.vue'
import MemberClothing from '@/views/member/clothing/index.vue'
import { getMyServices } from '@/api/portal/services'
import { borrowBook, getBooks } from '@/api/portal/library'
import { createClothingOrder, getClothingItems } from '@/api/portal/clothing'
import { ElMessage } from 'element-plus'

vi.mock('@/api/portal/services', () => ({ getMyServices: vi.fn() }))
vi.mock('@/api/portal/library', () => ({ getBooks: vi.fn(), borrowBook: vi.fn() }))
vi.mock('@/api/portal/clothing', () => ({
  getClothingItems: vi.fn(),
  createClothingOrder: vi.fn(),
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}))

beforeEach(() => {
  vi.clearAllMocks()
  getMyServices.mockResolvedValue({ borrows: [], orders: [] })
  getBooks.mockResolvedValue({
    rows: [{ bookId: 8, bookName: 'Vue.js 设计与实现', bookType: 'TEXTBOOK', available: true }],
    total: 1,
  })
  getClothingItems.mockResolvedValue([{
    itemId: 4,
    itemName: 'Quanta 塔服',
    colors: ['黑色'],
    sizes: ['L'],
    price: null,
    priceLabel: '以实物通知为准',
  }])
})

describe('member self-service pages', () => {
  it('renders service history without workstation reservations', async () => {
    const wrapper = mount(MemberServices)
    await flushPromises()

    expect(wrapper.get('[role="tablist"]').text()).not.toContain('工位预约')
    expect(wrapper.get('[role="tablist"]').text()).toContain('图书借阅')
    expect(wrapper.get('[role="tablist"]').text()).toContain('塔服订单')
  })

  it('submits a selected book with a date-only due time', async () => {
    borrowBook.mockResolvedValue({ code: 200 })
    const wrapper = mount(MemberLibrary)
    await flushPromises()

    await wrapper.get('[data-testid="borrow-book-8"]').trigger('click')
    await wrapper.get('input[name="dueTime"]').setValue('2026-10-01')
    await wrapper.get('[data-testid="confirm-borrow"]').trigger('click')
    await flushPromises()

    expect(borrowBook).toHaveBeenCalledWith(8, '2026-10-01')
    expect(getBooks).toHaveBeenCalledTimes(2)
    expect(ElMessage.success).toHaveBeenCalledWith('图书借阅成功')
  })

  it('saves clothing only as a DRAFT without inventing a price', async () => {
    createClothingOrder.mockResolvedValue({ status: 'DRAFT' })
    const wrapper = mount(MemberClothing)
    await flushPromises()

    expect(wrapper.text()).toContain('以实物通知为准')
    expect(wrapper.get('[data-testid="save-order-draft"]').text()).toBe('保存订购草稿')
    await wrapper.get('[data-testid="save-order-draft"]').trigger('click')
    await flushPromises()

    expect(createClothingOrder).toHaveBeenCalledWith(
      expect.objectContaining({ itemId: 4, price: null }),
      { color: '黑色', size: 'L', quantity: 1 },
    )
    expect(ElMessage.success).toHaveBeenCalledWith('订购草稿已保存')
  })
})
