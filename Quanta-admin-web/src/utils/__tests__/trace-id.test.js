import { describe, expect, it } from 'vitest'
import { TRACE_ID_HEADER, createTraceId, getTraceId, withTraceId } from '@/utils/trace-id'

describe('trace id utilities', () => {
  it('creates a unique 32-character lowercase hexadecimal id', () => {
    const first = createTraceId()
    const second = createTraceId()

    expect(first).toMatch(/^[a-f0-9]{32}$/)
    expect(second).toMatch(/^[a-f0-9]{32}$/)
    expect(first).not.toBe(second)
  })

  it('adds a trace id header when one is absent', () => {
    const config = withTraceId({ headers: {} })

    expect(config.headers[TRACE_ID_HEADER]).toMatch(/^[a-f0-9]{32}$/)
  })

  it('preserves an explicitly supplied trace id regardless of header casing', () => {
    const config = withTraceId({ headers: { 'x-trace-id': 'backend-test-id' } })

    expect(config.headers['x-trace-id']).toBe('backend-test-id')
    expect(config.headers[TRACE_ID_HEADER]).toBeUndefined()
  })

  it('reads trace id headers case-insensitively from a request config', () => {
    expect(getTraceId({ headers: { 'X-TRACE-ID': 'case-insensitive' } })).toBe('case-insensitive')
  })
})
