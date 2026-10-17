import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import UserPanel from '../components/UserPanel.vue'
import { useUserStore } from '@/stores/user'

describe('admin user panel labels', () => {
  it('renders Chinese department and job labels while keeping the original roles', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const userStore = useUserStore()
    userStore.user = { nickName: '刘同学', memberDepartment: 'FRONTEND', memberTitle: 'MANAGER' }
    userStore.roles = ['qt_manager']
    const router = createRouter({ history: createMemoryHistory(), routes: [] })
    const wrapper = mount(UserPanel, { global: { plugins: [pinia, ElementPlus, router] } })
    expect(wrapper.text()).toContain('前端部 · 经理')
    expect(userStore.roles).toEqual(['qt_manager'])
    expect(userStore.user.memberTitle).toBe('MANAGER')
    wrapper.unmount()
  })
})
