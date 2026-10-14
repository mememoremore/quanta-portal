import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const source = readFileSync(new URL('./elite-share.vue', import.meta.url), 'utf8')
const pageRule = source.match(/\.elite-page\s*\{([^}]*)\}/)?.[1] ?? ''
const topBarRule = source.match(/\.top-bar\s*\{([^}]*)\}/)?.[1] ?? ''
const pageScrollRule = source.match(/\.page-scroll\s*\{([^}]*)\}/)?.[1] ?? ''
const contentCardRule = source.match(/\.content-card\s*\{([^}]*)\}/)?.[1] ?? ''
const scrollSpacerRule = source.match(/\.scroll-spacer\s*\{([^}]*)\}/)?.[1] ?? ''

describe('elite share page frame', () => {
  it('uses the same compact top navigation height as other freshman pages', () => {
    expect(pageRule).toContain('display: flex')
    expect(pageRule).toContain('flex-direction: column')
    expect(topBarRule).toContain('height: 176rpx')
    expect(topBarRule).toContain('padding: 88rpx 32rpx 40rpx')
  })

  it('allows the content card bottom to scroll above the fixed signup bar', () => {
    expect(pageScrollRule).toContain('flex: 1')
    expect(pageScrollRule).toContain('height: 0')
    expect(contentCardRule).toContain('margin: 40rpx auto 0')
    expect(source).toContain('<view class="scroll-spacer" />')
    expect(scrollSpacerRule).toContain('height: calc(180rpx + env(safe-area-inset-bottom))')
  })
})
