import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const pages = [
  './home/home.vue', './contact/contact.vue', './function/function.vue',
  './shirt-order/shirt-order.vue', './business-card/view.vue', './business-card/edit.vue',
  './security/security.vue', './services/services.vue', './library/library.vue',
  './workstation/workstation.vue',
]

describe('member page navigation migration', () => {
  for (const page of pages) {
    it(`${page} uses the shared safe header`, () => {
      const source = readFileSync(new URL(page, import.meta.url), 'utf8')
      expect(source).toContain('MemberSafeHeader')
      expect(source).toContain('<member-safe-header')
      expect(source).not.toContain('<view class="top-bar">')
      expect(source).not.toContain('<view class="header">')
    })
  }

  it('leaves freshman pages outside this migration', () => {
    const source = readFileSync(new URL('../freshman/home.vue', import.meta.url), 'utf8')
    expect(source).not.toContain('MemberSafeHeader')
  })

  it('keeps visible spacing between home search and notification icons', () => {
    const source = readFileSync(new URL('./home/home.vue', import.meta.url), 'utf8')
    expect(source).toContain('class="header-actions"')
    expect(source).toContain('gap: 28rpx')
  })
})
