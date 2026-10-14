import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus, { ElPagination } from 'element-plus'
import RecruitmentPagination from '../components/RecruitmentPagination.vue'

describe('RecruitmentPagination', () => {
  it('emits page and page-size changes', async () => {
    const wrapper = mount(RecruitmentPagination, {
      props: { page: 2, pageSize: 10, total: 86 },
      global: { plugins: [ElementPlus] },
    })

    const pagination = wrapper.findComponent(ElPagination)
    expect(pagination.props('currentPage')).toBe(2)
    expect(pagination.props('pageSize')).toBe(10)
    expect(pagination.props('total')).toBe(86)

    pagination.vm.$emit('current-change', 3)
    pagination.vm.$emit('size-change', 20)
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:page')?.[0]).toEqual([3])
    expect(wrapper.emitted('update:pageSize')?.[0]).toEqual([20])
  })

  it('does not render for a single page', () => {
    const wrapper = mount(RecruitmentPagination, {
      props: { page: 1, pageSize: 10, total: 10 },
      global: { plugins: [ElementPlus] },
    })

    expect(wrapper.find('[data-testid="recruitment-pagination"]').exists()).toBe(false)
  })
})
