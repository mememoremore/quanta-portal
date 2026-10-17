import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import LearningMaterialsView from '../index.vue'
import { useUserStore } from '@/stores/user'
import { resetMockMaterials } from '@/mock/data/materials'
import { setToken } from '@/utils/token'
import { findAccountByToken } from '@/mock/data/accounts'

function mountFor(token, permissions, roles) {
  setToken(token)
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useUserStore()
  store.permissions = permissions
  store.roles = roles
  store.user = findAccountByToken(`Bearer ${token}`)?.user || null
  return mount(LearningMaterialsView, { global: { plugins: [pinia, ElementPlus] } })
}

describe('learning materials view', () => {
  beforeEach(resetMockMaterials)

  it('shows upload and delete controls to management', async () => {
    const wrapper = mountFor(
      'mock-token-product-manager',
      ['qt:material:list', 'qt:material:add', 'qt:material:remove'],
      ['qt_mgmt'],
    )
    await flushPromises()
    await wrapper.get('[data-testid="open-material-upload"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('点击选择，或拖拽文件到这里')
    expect(wrapper.text()).toContain('确认上传')
    expect(wrapper.text()).toContain('产品部')
    expect(wrapper.text()).toContain('删除')
    wrapper.unmount()
  })

  it('shows upload to managers without qt:material:add while keeping delete hidden', async () => {
    const wrapper = mountFor('mock-token-product-interviewer', ['qt:material:list'], ['qt_manager'])
    await flushPromises()
    expect(wrapper.text()).toContain('React 入门教程.pdf')
    expect(wrapper.text()).not.toContain('删除')
    await wrapper.get('[data-testid="open-material-upload"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('产品部')
    expect(wrapper.text()).toContain('确认上传')
    wrapper.unmount()
  })

  it('restores the list after clearing a search and supports the fixed type filter', async () => {
    const wrapper = mountFor('mock-token-product-manager', ['qt:material:list', 'qt:material:add'], ['qt_mgmt'])
    await flushPromises()
    const input = wrapper.get('input[aria-label="搜索资料名称"]')
    await input.setValue('找不到的资料')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).not.toContain('React 入门教程.pdf')
    await input.setValue('')
    await flushPromises()
    expect(wrapper.text()).toContain('React 入门教程.pdf')
    await wrapper.findAll('.material-filters__types > div button')[2].trigger('click')
    await flushPromises()
    expect(wrapper.text()).not.toContain('React 入门教程.pdf')
    await wrapper.get('.material-filters__reset').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('React 入门教程.pdf')
    wrapper.unmount()
  })
})
