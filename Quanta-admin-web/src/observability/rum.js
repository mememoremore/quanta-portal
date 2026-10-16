const RUM_CONSENT_KEY = 'quanta:rum-consent:v1'
const RUM_SCRIPT_URL = 'https://sdk.rum.aliyuncs.com/v2/browser-sdk.js'
const PRODUCTION_HOST = 'www.quantacenter.com'
const SENSITIVE_FIELDS = new Set([
  'body', 'cookies', 'data', 'headers', 'params', 'reqheaders',
  'requestbody', 'resheaders', 'response', 'responsebody', 'snapshots',
])

export function isRumAvailable({
  production = import.meta.env.PROD,
  hostname = globalThis.location?.hostname,
  endpoint = import.meta.env.VITE_ARMS_RUM_ENDPOINT,
} = {}) {
  return production === true && hostname === PRODUCTION_HOST && /^https:\/\//.test(endpoint || '')
}

export function readRumConsent(storage) {
  try {
    const choice = (storage ?? globalThis.localStorage)?.getItem(RUM_CONSENT_KEY)
    return choice === 'accepted' || choice === 'declined' ? choice : null
  } catch {
    return null
  }
}

export function saveRumConsent(choice, storage) {
  if (choice !== 'accepted' && choice !== 'declined') return false
  try {
    const resolvedStorage = storage ?? globalThis.localStorage
    resolvedStorage?.setItem(RUM_CONSENT_KEY, choice)
    return true
  } catch {
    return false
  }
}

export function stripUrlDetails(value) {
  if (typeof value !== 'string') return value
  if (!/^(https?:\/\/|\/|\.\/)/i.test(value)) return value
  return value.split(/[?#]/, 1)[0]
}

export function sanitizeRumEvent(event) {
  const visited = new WeakSet()
  function visit(value) {
    if (!value || typeof value !== 'object' || visited.has(value)) return value
    visited.add(value)
    for (const key of Object.keys(value)) {
      if (SENSITIVE_FIELDS.has(key.toLowerCase())) {
        delete value[key]
      } else if (typeof value[key] === 'string') {
        value[key] = stripUrlDetails(value[key])
      } else {
        visit(value[key])
      }
    }
    return value
  }
  return visit(event)
}

export function loadRumSdk({
  production = import.meta.env.PROD,
  hostname = globalThis.location?.hostname,
  endpoint = import.meta.env.VITE_ARMS_RUM_ENDPOINT,
  windowRef = globalThis.window,
  documentRef = globalThis.document,
  storage,
} = {}) {
  if (!isRumAvailable({ production, hostname, endpoint }) || readRumConsent(storage) !== 'accepted') {
    return false
  }
  if (!windowRef || !documentRef?.body || documentRef.querySelector('script[data-quanta-rum]')) {
    return false
  }

  windowRef.__rum = {
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
    parseViewName: stripUrlDetails,
    parseResourceName: stripUrlDetails,
    evaluateApi: async (options) => ({
      name: stripUrlDetails(options?.url || ''),
      snapshots: '',
    }),
    beforeReport: sanitizeRumEvent,
  }

  const script = documentRef.createElement('script')
  script.dataset.quantaRum = 'true'
  script.async = true
  script.crossOrigin = 'anonymous'
  script.src = RUM_SCRIPT_URL
  script.onerror = () => {
    script.remove()
    delete windowRef.__rum
  }
  documentRef.body.insertBefore(script, documentRef.body.firstChild)
  return true
}

export function startRumIfConsented() {
  return loadRumSdk()
}
