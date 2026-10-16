import { afterEach, describe, expect, it } from 'vitest'
import {
  isRumAvailable,
  loadRumSdk,
  sanitizeRumEvent,
  stripUrlDetails,
} from '../rum'

const endpoint = 'https://example.aliyuncs.com/rum/web/v2?service_id=d08n852dkz@a575525305e4ddb5ce019'

afterEach(() => {
  delete window.__rum
})

describe('ARMS RUM automatic production loader', () => {
  it('only enables production on the real site with an endpoint', () => {
    expect(isRumAvailable({ production: true, hostname: 'www.quantacenter.com', endpoint })).toBe(true)
    expect(isRumAvailable({ production: false, hostname: 'www.quantacenter.com', endpoint })).toBe(false)
    expect(isRumAvailable({ production: true, hostname: 'localhost', endpoint })).toBe(false)
    expect(isRumAvailable({ production: true, hostname: 'www.quantacenter.com', endpoint: '' })).toBe(false)
  })

  it('does not download the SDK outside production or without a configured endpoint', () => {
    expect(loadRumSdk({ production: false, hostname: 'www.quantacenter.com', endpoint })).toBe(false)
    expect(loadRumSdk({ production: true, hostname: 'www.quantacenter.com', endpoint: '' })).toBe(false)
    expect(document.querySelector('script[data-quanta-rum]')).toBe(null)
  })

  it('starts automatically even if the old consent setting was declined', () => {
    localStorage.setItem('quanta:rum-consent:v1', 'declined')
    expect(loadRumSdk({ production: true, hostname: 'www.quantacenter.com', endpoint })).toBe(true)
  })

  it('sets exact ARMS config before adding a single async CDN script', () => {
    const options = { production: true, hostname: 'www.quantacenter.com', endpoint }
    expect(loadRumSdk(options)).toBe(true)
    const script = document.querySelector('script[data-quanta-rum]')
    expect(script?.src).toBe('https://sdk.rum.aliyuncs.com/v2/browser-sdk.js')
    expect(script?.async).toBe(true)
    expect(window.__rum).toMatchObject({
      endpoint,
      env: 'prod',
      spaMode: 'history',
      collectors: {
        perf: true,
        webVitals: true,
        api: true,
        staticResource: true,
        jsError: true,
        consoleError: true,
        click: { enable: true, trackUserInteractions: true },
      },
      tracing: false,
      replay: false,
    })
    expect(loadRumSdk(options)).toBe(false)
    expect(document.querySelectorAll('script[data-quanta-rum]')).toHaveLength(1)
  })

  it('allows a retry if the CDN script fails without affecting the page', () => {
    const options = { production: true, hostname: 'www.quantacenter.com', endpoint }
    expect(loadRumSdk(options)).toBe(true)
    document.querySelector('script[data-quanta-rum]').dispatchEvent(new Event('error'))
    expect(document.querySelector('script[data-quanta-rum]')).toBe(null)
    expect(loadRumSdk(options)).toBe(true)
  })

  it('removes URL query/fragment and diagnostic bodies from outgoing events', () => {
    expect(stripUrlDetails('https://example.com/api/list?token=secret#part')).toBe('https://example.com/api/list')
    expect(stripUrlDetails('/freshman/home?student=123')).toBe('/freshman/home')
    const event = {
      view: { url: 'https://example.com/a?student=123' },
      resource: { name: '/api/list?token=secret' },
      api: { snapshots: 'private', reqHeaders: { Authorization: 'Bearer secret' } },
    }
    expect(sanitizeRumEvent(event)).toEqual({
      view: { url: 'https://example.com/a' },
      resource: { name: '/api/list' },
      api: {},
    })
  })
})
