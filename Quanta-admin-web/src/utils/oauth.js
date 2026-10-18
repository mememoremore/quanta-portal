export const challengeOrigin = String(import.meta.env.VITE_CHALLENGE_ORIGIN || 'https://challenge.quantacenter.com').replace(/\/$/, '')
const callbackPath = '/api/auth/quanta/callback'

export function interactionId(value) {
  if (typeof value !== 'string' || !/^[A-Za-z0-9_-]{32,128}$/.test(value)) throw new Error('授权请求无效，请从 Challenge 重新发起登录。')
  return value
}

export function isOAuthMember(user) {
  return [true, 1, '1'].includes(user?.isQuantaMember)
}

export function assertUnexpired(value, now = Date.now()) {
  if (typeof value !== 'string' || !Number.isFinite(Date.parse(value)) || Date.parse(value) <= now) throw new Error('本次授权已过期，请从 Challenge 重新登录。')
}

export function validateInteraction(data, id, now = Date.now()) {
  interactionId(id)
  const client = data?.client
  if (data?.interactionId !== id || !['pending', 'approved'].includes(data.status) || client?.clientId !== 'quanta-challenge' || client.origin !== challengeOrigin || client.redirectUri !== `${challengeOrigin}${callbackPath}` || !Array.isArray(data.scopes) || data.scopes.length !== 2 || !data.scopes.includes('openid') || !data.scopes.includes('profile') || typeof data.csrfToken !== 'string' || !/^[A-Za-z0-9_-]{32,128}$/.test(data.csrfToken) || typeof data.consentRequired !== 'boolean') throw new Error('授权服务返回了不受信任的请求，请重新发起登录。')
  assertUnexpired(data.expiresAt, now)
  return data
}

export function validateApproval(data, id, now = Date.now()) {
  if (data?.status !== 'approved' || data.continuePath !== `/oauth2/interaction/${interactionId(id)}/continue`) throw new Error('授权结果无效，无法安全返回 Challenge。')
  assertUnexpired(data.expiresAt, now)
  return data
}

export function validateDeniedRedirect(value, registeredUri) {
  const url = new URL(value)
  if (`${url.origin}${url.pathname}` !== registeredUri || url.hash || url.username || url.password || url.searchParams.get('error') !== 'access_denied' || !url.searchParams.get('state') || [...url.searchParams.keys()].some(key => !['error', 'error_description', 'state', 'iss'].includes(key)) || [...new Set(url.searchParams.keys())].some(key => url.searchParams.getAll(key).length !== 1)) throw new Error('授权取消响应无效。')
  return url.href
}

export function continuationLocation(id) {
  const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  return `${base}/oauth2/interaction/${interactionId(id)}/continue`
}
