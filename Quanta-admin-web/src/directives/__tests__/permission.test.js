import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { useUserStore } from '@/stores/user'
import { permissionDirective } from '../permission'

const TestComponent = {
  template: '<div><button v-permission="[\'system:user:remove\']">删除</button></div>',
}

describe('permission directive', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
  })

  it('removes an element when permission is absent', () => {
    useUserStore().permissions = ['qt:member:list']
    const wrapper = mount(TestComponent, {
      global: { plugins: [pinia], directives: { permission: permissionDirective } },
    })
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('keeps an element for a super administrator', () => {
    useUserStore().permissions = ['*:*:*']
    const wrapper = mount(TestComponent, {
      global: { plugins: [pinia], directives: { permission: permissionDirective } },
    })
    expect(wrapper.find('button').exists()).toBe(true)
  })
})
