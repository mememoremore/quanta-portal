import { describe, expect, it } from 'vitest'
import { metricsHandlers } from '../metrics'

function config(token) {
  return { headers: { Authorization: `Bearer ${token}` } }
}

describe('access metrics permissions', () => {
  it('allows ceo and admin to read metrics', () => {
    expect(metricsHandlers[0].handle(config('mock-token-ceo')).code).toBe(200)
    expect(metricsHandlers[1].handle(config('mock-token-admin')).code).toBe(200)
  })

  it('rejects manager and intern from the metrics page', () => {
    expect(metricsHandlers[0].handle(config('mock-token-product-manager')).code).toBe(403)
    expect(metricsHandlers[0].handle(config('mock-token-product-interviewer')).code).toBe(403)
    expect(metricsHandlers[0].handle(config('mock-token-viewer')).code).toBe(403)
  })
})
