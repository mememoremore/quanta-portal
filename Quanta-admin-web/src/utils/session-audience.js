const AUDIENCE_KEY = 'quanta_session_audience'
const allowedAudiences = new Set(['freshman', 'member', 'admin'])

export function getAudience() {
  const value = localStorage.getItem(AUDIENCE_KEY) || ''
  return allowedAudiences.has(value) ? value : ''
}

export function setAudience(audience) {
  if (!allowedAudiences.has(audience)) throw new Error('未知登录身份')
  localStorage.setItem(AUDIENCE_KEY, audience)
}

export function clearAudience() {
  localStorage.removeItem(AUDIENCE_KEY)
}

export function loginTypeFor(audience) {
  if (!allowedAudiences.has(audience)) throw new Error('未知登录身份')
  return audience === 'freshman' ? '0' : '1'
}

export function loginPathFor(audience) {
  if (audience === 'freshman') return '/login/freshman'
  if (audience === 'member') return '/login/member'
  return '/admin/login'
}

export function homePathFor(audience) {
  if (audience === 'freshman') return '/freshman/home'
  if (audience === 'member') return '/member/home'
  return '/admin/dashboard'
}

export function audienceForPath(path = '') {
  if (path === '/login/freshman' || path === '/freshman' || path.startsWith('/freshman/')) return 'freshman'
  if (path === '/login/member' || path === '/member' || path.startsWith('/member/')) return 'member'
  if (path === '/admin/login' || path === '/admin' || path.startsWith('/admin/')) return 'admin'
  return ''
}

export { AUDIENCE_KEY }
