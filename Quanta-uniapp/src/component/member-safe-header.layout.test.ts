import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const source = readFileSync(new URL('./MemberSafeHeader.vue', import.meta.url), 'utf8')

describe('MemberSafeHeader layout', () => {
  it('uses runtime safe-area and capsule metrics', () => {
    expect(source).toContain('getMemberNavigationMetrics')
    expect(source).toContain("height: `${metrics.totalHeight}px`")
    expect(source).toContain("paddingTop: `${metrics.statusBarHeight}px`")
    expect(source).toContain("right: `${metrics.capsuleInsetRight}px`")
  })

  it('supports brand actions and detail navigation', () => {
    expect(source).toContain("mode === 'brand'")
    expect(source).toContain('<slot name="actions" />')
    expect(source).toContain('member-safe-header__back')
    expect(source).toContain("emit('back')")
  })

  it('keeps detail titles centered inside symmetric safe bounds', () => {
    expect(source).toContain('titleSafeStyle')
    expect(source).toContain("left: `${metrics.capsuleInsetRight}px`")
    expect(source).toContain("right: `${metrics.capsuleInsetRight}px`")
    expect(source.match(/transform: translateY\(-50%\)/g)).toHaveLength(4)
  })
})
