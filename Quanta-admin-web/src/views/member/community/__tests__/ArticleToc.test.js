import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ArticleToc from '../ArticleToc.vue'

const headings = [{ id: 'toc-test-a', text: '开头', level: 2 }, { id: 'toc-test-b', text: '结尾', level: 2 }]
let reduceMotion, wrapper

beforeEach(() => {
  reduceMotion = false
  vi.stubGlobal('matchMedia', vi.fn(query => ({ matches: query.includes('min-width') || reduceMotion, addEventListener: vi.fn(), removeEventListener: vi.fn() })))
  vi.stubGlobal('requestAnimationFrame', vi.fn(() => 1))
  vi.stubGlobal('cancelAnimationFrame', vi.fn())
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  vi.spyOn(document.documentElement, 'scrollHeight', 'get').mockReturnValue(4000)
  for (const [index, heading] of headings.entries()) {
    const el = document.createElement('h2')
    el.id = heading.id
    el.getBoundingClientRect = () => ({ top: 500 + index * 500 })
    document.body.appendChild(el)
  }
})

afterEach(() => { wrapper?.unmount(); wrapper = null; vi.restoreAllMocks(); vi.unstubAllGlobals() })

describe('article TOC navigation', () => {
  it('scrolls the document with header clearance instead of scrolling ancestor containers', async () => {
    wrapper = mount(ArticleToc, { props: { headings } })
    await flushPromises()
    await wrapper.findAll('nav a')[1].trigger('click')
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 904, behavior: 'smooth' })
    expect(wrapper.findAll('nav a')[1].attributes('aria-current')).toBe('location')
  })

  it('uses immediate navigation for reduced-motion preferences', async () => {
    reduceMotion = true
    wrapper = mount(ArticleToc, { props: { headings } })
    await flushPromises()
    await wrapper.findAll('nav a')[0].trigger('click')
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 404, behavior: 'instant' })
  })

  it('removes all scroll and interaction listeners when unmounted', async () => {
    const remove = vi.spyOn(window, 'removeEventListener')
    wrapper = mount(ArticleToc, { props: { headings } })
    await flushPromises()
    wrapper.unmount(); wrapper = null
    for (const event of ['scroll', 'resize', 'scrollend', 'wheel', 'touchstart', 'pointerdown', 'keydown']) {
      expect(remove).toHaveBeenCalledWith(event, expect.any(Function))
    }
    expect(window.cancelAnimationFrame).toHaveBeenCalled()
  })
})
