import { describe, expect, it } from 'vitest'
import { resolveApiBaseUrl, resolveUseMock } from './runtime'

describe('runtime configuration', () => {
  it('uses the local backend when the URL is missing or blank', () => {
    expect(resolveApiBaseUrl()).toBe('http://127.0.0.1:8080')
    expect(resolveApiBaseUrl('   ')).toBe('http://127.0.0.1:8080')
  })

  it('removes trailing slashes from a configured backend URL', () => {
    expect(resolveApiBaseUrl('http://192.168.1.20:8080/')).toBe('http://192.168.1.20:8080')
  })

  it('enables mock login only for the explicit true value', () => {
    expect(resolveUseMock('true')).toBe(true)
    expect(resolveUseMock('false')).toBe(false)
    expect(resolveUseMock()).toBe(false)
  })
})
