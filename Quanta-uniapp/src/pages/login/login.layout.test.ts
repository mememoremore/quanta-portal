import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const loginPages = ['freshman.vue', 'tower.vue'].map((file) => ({
  file,
  source: readFileSync(new URL(`./${file}`, import.meta.url), 'utf8'),
}))

describe('login button loading label', () => {
  it.each(loginPages)('keeps 登录中 on one line in $file', ({ source }) => {
    const rule = source.match(/\.login-button-text\s*\{([^}]*)\}/)?.[1] ?? ''
    expect(rule).toContain('width: auto')
    expect(rule).toContain('min-width: 64rpx')
    expect(rule).toContain('white-space: nowrap')
  })
})
