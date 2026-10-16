import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import LoginBrand from '../LoginBrand.vue'
import BackToIdentity from '../BackToIdentity.vue'

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div />' } }],
  })
}

describe('shared login identity elements', () => {
  it('renders the same brand as a link on portal login', () => {
    const wrapper = mount(LoginBrand, {
      props: { link: true },
      global: { plugins: [makeRouter()] },
    })

    expect(wrapper.element.tagName).toBe('A')
    expect(wrapper.attributes('href')).toBe('/')
    expect(wrapper.text()).toContain('Quanta')
    expect(wrapper.text()).toContain('DIGITAL CLUB')
    expect(wrapper.get('img[alt="Quanta 社团 Logo"]').attributes('src')).toContain('quanta-logo-small.jpg')
  })

  it('renders a non-link dark brand on the admin mobile card', () => {
    const wrapper = mount(LoginBrand, { props: { tone: 'dark' } })

    expect(wrapper.element.tagName).toBe('DIV')
    expect(wrapper.classes()).toContain('login-brand--dark')
  })

  it('links back to the identity selector with shared wording', () => {
    const wrapper = mount(BackToIdentity, {
      global: { plugins: [makeRouter()] },
    })

    expect(wrapper.get('a').text()).toBe('← 返回身份选择')
    expect(wrapper.get('a').attributes('href')).toBe('/')
  })
})
