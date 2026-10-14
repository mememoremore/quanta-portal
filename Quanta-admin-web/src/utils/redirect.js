import { audienceForPath, homePathFor } from './session-audience'

export function safeInternalRedirect(value, fallback = '/admin/dashboard') {
  if (typeof value !== 'string') return fallback
  if (!value.startsWith('/') || value.startsWith('//')) return fallback
  if (value.includes('\\')) return fallback
  if ([...value].some((character) => {
    const code = character.charCodeAt(0)
    return code < 32 || code === 127
  })) return fallback
  return value
}

export function safeAudienceRedirect(value, audience) {
  const fallback = homePathFor(audience)
  const redirect = safeInternalRedirect(value, fallback)
  return audienceForPath(redirect) === audience ? redirect : fallback
}
