import { service, normalizeRuoYiResponse, refreshSession, isDualTokenEnabled } from '@/utils/request'
import { getToken } from '@/utils/token'
import { interactionId } from '@/utils/oauth'

// OAuth must never use demo handlers or trigger the ordinary admin-login redirect.
async function send(config, retry = true) {
  try {
    return normalizeRuoYiResponse((await service.request({ ...config, withCredentials: true })).data)
  } catch (error) {
    const status = Number(error.response?.status || error.response?.data?.code || error.code)
    if (status === 401 && retry && config.headers?.isToken !== false && isDualTokenEnabled()) {
      await refreshSession(getToken())
      return send(config, false)
    }
    const result = new Error([404, 501, 503].includes(status) ? '授权服务尚未就绪，请稍后重试或联系管理员。' : error.response?.data?.msg || error.message || '授权服务连接失败，请重试。')
    result.code = status
    throw result
  }
}

export function getInteraction(id) {
  return send({ url: `/oauth2/interaction/${interactionId(id)}`, headers: { isToken: false } }).then(value => value.data)
}

export function decideInteraction(id, action, csrfToken) {
  return send({ url: `/oauth2/interaction/${interactionId(id)}/decision`, method: 'post', data: { action }, headers: { 'X-CSRF-Token': csrfToken } }, false).then(value => value.data)
}

export function getOAuthIdentity() {
  return send({ url: '/getInfo' })
}
