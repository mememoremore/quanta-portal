const text = (value) => String(value ?? '').trim()
const unsafeCharacters = (value) => Array.from(value).some((char) => char === '\\' || char.charCodeAt(0) <= 32)

export function assetUrl(value = '') {
  const url = text(value)
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return profileLink(url)
  if (/^data:image\/(jpeg|png|webp);base64,[a-z0-9+/=]+$/i.test(url)) return url
  if (/^[a-z][\w+.-]*:/i.test(url) || url.startsWith('//') || unsafeCharacters(url)) return ''
  const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  if (base.startsWith('/') && (url === base || url.startsWith(`${base}/`))) return url
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

export function normalizeProfile(value = {}) {
  const skills = Array.isArray(value.skills) ? value.skills : text(value.skills).split(/[,，]/)
  return {
    headline: text(value.headline), bio: text(value.bio),
    skills: [...new Set(skills.map(text).filter(Boolean))],
    githubUrl: text(value.githubUrl), websiteUrl: text(value.websiteUrl),
    contactEmail: text(value.contactEmail), showContactEmail: value.showContactEmail === true,
  }
}

export function profileLink(value) {
  try {
    const url = new URL(text(value))
    return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password ? url.href : ''
  } catch { return '' }
}

export function validateProfile(value) {
  const data = normalizeProfile(value)
  if (data.headline.length > 80) return '一句话介绍最多 80 字'
  if (data.bio.length > 300) return '个人简介最多 300 字'
  if (data.skills.length > 5 || data.skills.some((s) => s.length > 20)) return '最多 5 个技能标签，每个不超过 20 字'
  if ([data.githubUrl, data.websiteUrl].some((url) => url && (url.length > 300 || !profileLink(url)))) return '个人链接需为有效的 http 或 https 地址，最多 300 字符'
  if (data.contactEmail && (data.contactEmail.length > 160 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.contactEmail))) return '请输入正确的联系邮箱'
  if (data.showContactEmail && !data.contactEmail) return '公开联系邮箱前请先填写邮箱'
  return ''
}

export function validMemberId(id) { return /^\d+$/.test(String(id)) && Number.isSafeInteger(Number(id)) && Number(id) > 0 }
export function memberDestination(id, currentId) {
  return validMemberId(id) ? Number(id) === Number(currentId) ? '/member/profile' : `/member/people/${Number(id)}` : ''
}

export function safeProfileReturn(value) {
  return typeof value === 'string' && !unsafeCharacters(value) && /^\/member\/(?:community(?:\/\d+)?|directory|people\/\d+|profile)(?:[?#].*)?$/.test(value)
    ? value : '/member/community'
}
