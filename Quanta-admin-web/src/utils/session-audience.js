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

/**
 * 已登录用户打开某个入口（登录页）时应当落到的位置。
 *
 * - 身份与入口一致：回该端首页，不必重复登录
 * - 管理员打开管理入口：直接进后台
 * - 其余情况：停在目标登录页，由页面说明当前会话
 *
 * 打开别的入口只代表"我想看看这个门"，不代表要求退出当前账号，
 * 因此这里只决定去哪个页面，不涉及任何登出动作。
 */
export function entryPathFor({ audience = '', serverAudience = '', canAccessAdmin = false } = {}) {
  if (!audience) return '/'
  if (serverAudience === audience) return homePathFor(audience)
  if (audience === 'admin' && canAccessAdmin === true) return homePathFor('admin')
  return loginPathFor(audience)
}

export function audienceForPath(path = '') {
  if (path === '/login/freshman' || path === '/freshman' || path.startsWith('/freshman/')) return 'freshman'
  if (path === '/login/member' || path === '/member' || path.startsWith('/member/')) return 'member'
  if (path === '/admin/login' || path === '/admin' || path.startsWith('/admin/')) return 'admin'
  return ''
}

export { AUDIENCE_KEY }
