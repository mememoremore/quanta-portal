import { findAccountByToken } from '../data/accounts'
import { lectureQuota, mockLectureRegistrations } from '../data/lecture-signups'

export const lectureSignupHandlers = [{
  method: 'get',
  path: '/qt/activity/lecture/registrations',
  handle(config) {
    const account = findAccountByToken(config.headers?.Authorization || '')
    if (!account) return { code: 401, msg: '登录状态已失效' }
    if (!account.permissions.includes('*:*:*') && !account.permissions.includes('qt:activity:registrations')) {
      return { code: 403, msg: '没有查看宣讲会名单的权限' }
    }
    return { code: 200, rows: mockLectureRegistrations, total: mockLectureRegistrations.length, quota: lectureQuota }
  },
}]
