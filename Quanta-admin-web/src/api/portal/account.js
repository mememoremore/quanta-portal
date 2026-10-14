import { request } from '@/utils/request'

export function changePassword({ oldPassword, newPassword }) {
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: { oldPassword, newPassword },
  })
}

export function sendFreshmanEmailCode(email) {
  return request({
    url: '/system/user/profile/emailCode',
    method: 'post',
    data: { email },
  })
}

export function updateFreshmanEmail({ email, emailCode }) {
  return request({
    url: '/system/user/profile/updateEmail',
    method: 'put',
    data: { email, emailCode },
  })
}
