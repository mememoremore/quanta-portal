import { describe, expect, it } from 'vitest'
import { createCsv } from '../csv'

describe('csv utility', () => {
  it('creates a UTF-8 CSV and escapes commas', () => {
    const result = createCsv([{ key: 'name', label: '姓名' }], [{ name: '张,三' }])
    expect(result).toBe('\uFEFF姓名\r\n"张,三"')
  })
})
