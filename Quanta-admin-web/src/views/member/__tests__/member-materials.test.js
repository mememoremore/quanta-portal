import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MemberMaterials from '../materials/index.vue'
import { getPortalMaterials } from '@/api/portal/content'
import { getMaterialFolders } from '@/api/material-folders'

vi.mock('@/api/portal/content', () => ({ getPortalMaterials: vi.fn(), downloadPortalMaterial: vi.fn() }))
vi.mock('@/api/material-folders', () => ({ getMaterialFolders: vi.fn() }))

describe('member learning materials', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMaterialFolders.mockResolvedValue([{ folderId: 12, category: 'FRONTEND', parentId: 0, folderName: '培训资料' }])
    getPortalMaterials.mockResolvedValue({
      rows: [{ materialId: 1, fileName: '教程.pdf', category: 'FRONTEND', uploader: '小王', uploadedAt: '2026-10-14' }],
      total: 1,
    })
  })

  it('shows all four categories to every member and labels mixed results', async () => {
    const wrapper = mount(MemberMaterials)
    await flushPromises()
    expect(wrapper.get('.member-materials__tabs').text()).toContain('全部')
    expect(wrapper.get('.member-materials__tabs').text()).toContain('前端')
    expect(wrapper.get('.member-materials__tabs').text()).toContain('后端')
    expect(wrapper.get('.member-materials__tabs').text()).toContain('产品')
    expect(wrapper.get('.member-materials__tabs').text()).toContain('设计')
    expect(getPortalMaterials).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20 })
    expect(wrapper.get('.member-page__heading p').text()).toBe('LEARNING CENTER')
    expect(wrapper.find('.member-materials__library').exists()).toBe(false)
    expect(wrapper.find('.member-materials__category').text()).toBe('前端')
    await wrapper.findAll('.member-materials__tabs button')[2].trigger('click')
    await flushPromises()
    expect(getPortalMaterials).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20, category: 'BACKEND' })
  })

  it('clears a submitted search when the input is emptied, without refreshing', async () => {
    const wrapper = mount(MemberMaterials)
    await flushPromises()
    await wrapper.get('input[aria-label="搜索资料名称"]').setValue('Vue')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(getPortalMaterials.mock.lastCall[0].fileName).toBe('Vue')
    expect(wrapper.text()).toContain('清除搜索')
    await wrapper.get('input[aria-label="搜索资料名称"]').setValue('')
    await flushPromises()
    expect(getPortalMaterials.mock.lastCall[0]).not.toHaveProperty('fileName')
    expect(wrapper.find('.member-materials__search-state').exists()).toBe(false)
  })

  it('exposes exactly three fixed types and no folder management actions', async () => {
    const wrapper = mount(MemberMaterials)
    await flushPromises()
    await wrapper.findAll('.member-materials__tabs button')[1].trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.member-materials__types [data-material-type]').map(button => button.text())).toEqual(['培训资料', '技术分享', '项目实践'])
    await wrapper.find('.member-materials__types [data-material-type]').trigger('click')
    await flushPromises()
    expect(getPortalMaterials.mock.lastCall[0].folderId).toBe(12)
    expect(wrapper.get('.member-materials__details p').text()).toBe('前端未知大小 · 小王 · 2026-10-14')
    expect(wrapper.text()).not.toContain('新建文件夹')
    expect(wrapper.find('[data-testid="open-material-upload"]').exists()).toBe(false)
    await wrapper.findAll('.member-materials__tabs button')[0].trigger('click')
    await flushPromises()
    expect(getPortalMaterials).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20 })
    expect(wrapper.find('.member-materials__types').exists()).toBe(false)
  })
})
