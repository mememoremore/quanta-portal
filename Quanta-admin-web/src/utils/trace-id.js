export const TRACE_ID_HEADER = 'X-Trace-Id'

function fallbackTraceId() {
  const time = Date.now().toString(16).padStart(12, '0')
  let random = ''
  while (random.length < 20) {
    random += Math.floor(Math.random() * 0xffffffff)
      .toString(16)
      .padStart(8, '0')
  }
  return `${time}${random}`.slice(0, 32)
}

export function createTraceId() {
  const cryptoApi = globalThis.crypto
  if (typeof cryptoApi?.randomUUID === 'function') {
    return cryptoApi.randomUUID().replaceAll('-', '').toLowerCase()
  }
  if (typeof cryptoApi?.getRandomValues === 'function') {
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  }
  return fallbackTraceId()
}

function headerEntries(headers) {
  if (!headers) return []
  if (typeof headers.entries === 'function') return Array.from(headers.entries())
  return Object.entries(headers)
}

export function getTraceId(configOrHeaders) {
  const headers = configOrHeaders?.headers || configOrHeaders
  if (!headers) return ''
  if (typeof headers.get === 'function') {
    const value = headers.get(TRACE_ID_HEADER)
    if (value) return String(value)
  }
  const match = headerEntries(headers).find(
    ([name]) => String(name).toLowerCase() === TRACE_ID_HEADER.toLowerCase(),
  )
  return match?.[1] ? String(match[1]) : ''
}

export function withTraceId(config = {}) {
  const headers = config.headers || {}
  if (getTraceId(headers)) return config

  const traceId = createTraceId()
  if (typeof headers.set === 'function') {
    headers.set(TRACE_ID_HEADER, traceId)
    return { ...config, headers }
  }
  return {
    ...config,
    headers: { ...headers, [TRACE_ID_HEADER]: traceId },
  }
}
