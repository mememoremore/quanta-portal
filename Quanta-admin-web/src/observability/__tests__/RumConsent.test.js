import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RumConsent from '../RumConsent.vue'
import { readRumConsent, saveRumConsent } from '../rum'

vi.mock('../rum', async (importOriginal) => {
  const original = await importOriginal()
  return { ...original, loadRumSdk: vi.fn(() => true) }
})

import { loadRumSdk } from '../rum'

describe('RUM consent control', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('offers a non-blocking choice when consent has not been recorded', () => {
    const wrapper = mount(RumConsent, { props: { enabled: true } })
    expect(wrapper.text()).toContain('性能监控')
    expect(wrapper.findAll('button').map((button) => button.text())).toEqual(['同意监控', '暂不同意'])
    expect(loadRumSdk).not.toHaveBeenCalled()
  })

  it('loads the SDK only after acceptance and keeps settings accessible', async () => {
    const wrapper = mount(RumConsent, { props: { enabled: true } })
    await wrapper.get('button[data-rum-choice="accepted"]').trigger('click')
    expect(readRumConsent(localStorage)).toBe('accepted')
    expect(loadRumSdk).toHaveBeenCalledOnce()
    expect(wrapper.get('button[data-rum-settings]').text()).toBe('监控设置')
  })

  it('does not load the SDK when declined and permits later acceptance', async () => {
    const wrapper = mount(RumConsent, { props: { enabled: true } })
    await wrapper.get('button[data-rum-choice="declined"]').trigger('click')
    expect(readRumConsent(localStorage)).toBe('declined')
    expect(loadRumSdk).not.toHaveBeenCalled()
    await wrapper.get('button[data-rum-settings]').trigger('click')
    await wrapper.get('button[data-rum-choice="accepted"]').trigger('click')
    expect(loadRumSdk).toHaveBeenCalledOnce()
  })

  it('reloads after withdrawing acceptance to unload an active SDK', async () => {
    saveRumConsent('accepted', localStorage)
    const reloadPage = vi.fn()
    const wrapper = mount(RumConsent, { props: { enabled: true, reloadPage } })
    await wrapper.get('button[data-rum-settings]').trigger('click')
    await wrapper.get('button[data-rum-choice="declined"]').trigger('click')
    expect(readRumConsent(localStorage)).toBe('declined')
    expect(reloadPage).toHaveBeenCalledOnce()
  })

  it('renders nothing outside the production site', () => {
    const wrapper = mount(RumConsent, { props: { enabled: false } })
    expect(wrapper.html()).toBe('<!--v-if-->')
  })
})
