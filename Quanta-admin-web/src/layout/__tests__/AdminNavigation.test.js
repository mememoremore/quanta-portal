import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SidebarMenuItem from '../components/SidebarMenuItem.vue'

describe('admin navigation', () => {
  it('does not render static route badges as unread messages', () => {
    const wrapper = mount(SidebarMenuItem, {
      props: { route: { path: '/recruitment', meta: { title: '招新管理', badge: 2 } } },
      global: {
        stubs: {
          ElMenuItem: { template: '<div><slot name="title" /></div>' },
          ElIcon: { template: '<i><slot /></i>' },
        },
      },
    })

    expect(wrapper.text()).toContain('招新管理')
    expect(wrapper.find('.menu-item__badge').exists()).toBe(false)
  })

  it('uses the Quanta logo for the browser and admin sidebar', () => {
    const html = readFileSync(join(process.cwd(), 'index.html'), 'utf8')
    const sidebar = readFileSync(join(process.cwd(), 'src/layout/components/AppSidebar.vue'), 'utf8')

    expect(html).toContain('type="image/png"')
    expect(html).toContain('href="/quanta-favicon-small.png"')
    expect(sidebar).toContain("'/quanta-logo-small.jpg'")
    expect(sidebar).toContain('alt="Quanta 社团 Logo"')
  })

  it('keeps brand marks non-selectable without locking ordinary page text', () => {
    const sources = [
      ['src/views/entry/index.vue', 'entry-page__brand'],
      ['src/styles/portal.css', 'portal-header__brand'],
      ['src/components/identity-login/LoginBrand.vue', 'login-brand'],
      ['src/styles/layout.css', 'app-sidebar__brand'],
    ]

    sources.forEach(([file, className]) => {
      const source = readFileSync(join(process.cwd(), file), 'utf8')
      expect(source).toMatch(new RegExp(`\\.${className}[^{]*\\{[^}]*user-select:\\s*none`, 's'))
    })

    const globalCss = readFileSync(join(process.cwd(), 'src/styles/global.css'), 'utf8')
    expect(globalCss).not.toMatch(/(?:html|body|\*)[^{]*\{[^}]*user-select:\s*none/s)
  })

  it('does not add a trailing arrow to ordinary sidebar links', () => {
    const wrapper = mount(SidebarMenuItem, {
      props: { route: { path: '/members', meta: { title: '成员管理', icon: 'UserFilled' } } },
      global: { stubs: { ElMenuItem: { template: '<div><slot /><slot name="title" /></div>' }, ElIcon: { template: '<i><slot /></i>' } } },
    })
    expect(wrapper.findAll('svg')).toHaveLength(1)
    expect(wrapper.text()).toBe('成员管理')
  })

})
