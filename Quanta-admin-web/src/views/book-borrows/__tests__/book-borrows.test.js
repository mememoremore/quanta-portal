import { beforeEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import BookBorrowsView from '../index.vue'
import { setToken } from '@/utils/token'

describe('book borrow records view', () => {
  beforeEach(() => setToken('mock-token-ceo'))

  it('renders borrow statuses and marks overdue rows', async () => {
    const wrapper = mount(BookBorrowsView, { global: { plugins: [ElementPlus] } })
    await flushPromises()
    expect(wrapper.text()).toContain('JavaScript高级程序设计')
    expect(wrapper.text()).toContain('已逾期')
    expect(wrapper.find('.book-borrows-table__row--overdue').exists()).toBe(true)
  })
})
