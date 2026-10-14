import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import MembersView from '../index.vue'
import { resetMockMembers } from '@/mock/data/members'
import { useUserStore } from '@/stores/user'
import { setToken } from '@/utils/token'

describe('members view', () => {
  beforeEach(() => {
    resetMockMembers()
    setToken('mock-token-ceo')
  })

  it('loads the current cohort and member table', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useUserStore().permissions = ['qt:member:list', 'system:user:import']
    useUserStore().roles = ['ceo']
    const wrapper = mount(MembersView, {
      global: { plugins: [pinia, ElementPlus] },
    })

    await flushPromises()
    expect(wrapper.text()).toContain('第21届（当前）')
    expect(wrapper.text()).toContain('陈思远')
    expect(wrapper.text()).toContain('导入名单')
  })
})
