import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Guide from '../guide.vue'
import GuideEntry from '../components/GuideEntry.vue'
import { portalRoutes } from '@/router/portal-routes'

async function mountGuide(query = {}) {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/member/pages', component: { template: '<div />' } },
    { path: '/member/pages/guide', name: 'MemberPagesGuide', component: Guide },
  ] })
  await router.push({ name: 'MemberPagesGuide', query })
  return { router, wrapper: mount(Guide, { global: { plugins: [router] } }) }
}

afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals() })

describe('personal website guide', () => {
  it('opens the guide in a new tab before and after site creation', async () => {
    const { router, wrapper } = await mountGuide()
    wrapper.unmount()
    const entry = mount(GuideEntry, { global: { plugins: [router] } })
    const link = entry.get('a')
    expect(link.attributes('href')).toBe('/member/pages/guide')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener')
    await entry.setProps({ slug: 'octo-lab' })
    expect(link.attributes('href')).toBe('/member/pages/guide?slug=octo-lab')
    entry.unmount()
  })

  it('has a direct route protected by the existing member layout', () => {
    const parent = portalRoutes.find(route => route.path === '/member')
    const route = parent.children.find(route => route.name === 'MemberPagesGuide')
    expect(route.path).toBe('pages/guide')
    expect(route.meta.portalAudience).toBe('member')
    expect(route.meta.public).not.toBe(true)
  })

  it('personalizes the full example, exposes chapter links and renders code as text', async () => {
    const { wrapper } = await mountGuide({ slug: 'octo-lab' })
    const content = wrapper.get('article')
    expect(content.text()).toContain("const SLUG = 'octo-lab'")
    expect(content.text()).toContain('/open/pages/octo-lab')
    expect(content.text()).toContain('authorAvatar')
    expect(content.text()).toContain('response.ok')
    expect(content.text()).toContain('不会替你执行')
    expect(content.text()).toContain('开放接口无需登录')
    expect(content.findAll('script')).toHaveLength(0)
    expect(content.text()).not.toContain('YOUR_SITE_SLUG')
    expect(wrapper.findAll('h1')).toHaveLength(1)
    for (const link of wrapper.findAll('nav a')) {
      expect(wrapper.find(link.attributes('href')).exists()).toBe(true)
    }
    expect(wrapper.findAll('nav a')).toHaveLength(7)
    wrapper.unmount()
  })

  it.each([undefined, '<script>alert(1)</script>', ['first', 'second'], 'invalid/name'])('uses a safe placeholder for invalid slug %s', async slug => {
    const { wrapper } = await mountGuide({ slug })
    expect(wrapper.get('article').text()).toContain("const SLUG = 'your-site'")
    expect(wrapper.findAll('script')).toHaveLength(0)
    wrapper.unmount()
  })

  it('downloads a readable, personalized Markdown file with the user-facing filename', async () => {
    vi.useFakeTimers()
    const { wrapper } = await mountGuide({ slug: 'octo-lab' })
    const create = vi.fn(() => 'blob:guide')
    vi.stubGlobal('URL', class extends URL { static createObjectURL = create; static revokeObjectURL = vi.fn() })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    await wrapper.get('button').trigger('click')
    expect(create).toHaveBeenCalledWith(expect.any(Blob))
    expect(click.mock.instances[0].download).toBe('Quanta个人网站使用指南.md')
    expect(click.mock.instances[0].href).toBe('blob:guide')
    vi.runAllTimers()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:guide')
    wrapper.unmount()
  })
})
