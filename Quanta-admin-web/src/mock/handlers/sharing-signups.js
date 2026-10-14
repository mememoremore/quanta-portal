import { findAccountByToken } from '../data/accounts'
import { mockSharingRegistrations, sharingQuota } from '../data/sharing-signups'

export const sharingSignupHandlers = [{
  method: 'get',
  path: '/qt/activity/sharing/registrations',
  handle(config) {
    const authorization = config.headers?.Authorization || config.headers?.authorization || ''
    const account = findAccountByToken(authorization)
    if (!account) return { code: 401, msg: '登录状态已失效' }
    if (!account.permissions.includes('*:*:*') && !account.permissions.includes('qt:activity:registrations')) {
      return { code: 403, msg: '没有查看精英分享会名单的权限' }
    }
    return {
      code: 200,
      msg: '操作成功',
      rows: mockSharingRegistrations,
      total: mockSharingRegistrations.length,
      quota: sharingQuota,
    }
  },
}]
