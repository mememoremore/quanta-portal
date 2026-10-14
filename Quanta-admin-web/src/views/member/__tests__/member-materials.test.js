import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MemberMaterials from '../materials/index.vue'
import { getPortalMaterials } from '@/api/portal/content'

vi.mock('@/api/portal/content', () => ({ getPortalMaterials: vi.fn(), downloadPortalMaterial: vi.fn() }))

describe('member learning materials', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getPortalMaterials.mockResolvedValue({
      rows: [{ materialId: 1, fileName: '教程.pdf', category: 'FRONTEND', uploader: '小王', uploadedAt: '2026-10-14' }],
      total: 1,
    })
  })

  it('shows all four categories to every member and labels mixed results', async () => {
    const wrapper = mount(MemberMaterials)
    await flushPromises()
    expect(wrapper.get('[role="tablist"]').text()).toContain('前端')
    expect(wrapper.get('[role="tablist"]').text()).toContain('后端')
    expect(wrapper.get('[role="tablist"]').text()).toContain('产品')
    expect(wrapper.get('[role="tablist"]').text()).toContain('设计')
    expect(wrapper.find('.member-materials__category').text()).toBe('前端')
    await wrapper.findAll('[role="tab"]')[2].trigger('click')
    await flushPromises()
    expect(getPortalMaterials).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20, category: 'BACKEND' })
  })
})
