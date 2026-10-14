import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = readFileSync(resolve(process.cwd(), 'src/views/entry/index.vue'), 'utf8')

describe('identity entry scroll', () => {
  it('owns vertical overflow because the global body is locked', () => {
    expect(source).toMatch(/\.entry-page\s*\{[^}]*overflow-y:\s*auto/s)
    expect(source).toMatch(/\.entry-page\s*\{[^}]*height:\s*100dvh/s)
  })
})
