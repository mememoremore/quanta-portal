import { request } from '@/utils/request'

export function login(data) {
  return request({ url: '/login', method: 'post', data })
}

export function registerFreshman(data) {
  return request({
    url: '/register',
    method: 'post',
    data: {
      ...data,
      username: data.studentNo,
      loginType: '0',
    },
  })
}

export function sendRegisterEmailCode(data) {
  return request({
    url: '/register/emailCode',
    method: 'post',
    data: {
      email: data.email,
      studentNo: data.studentNo,
    },
  })
}

export function sendPasswordResetEmailCode(data) {
  return request({
    url: '/system/password/emailCode',
    method: 'post',
    data,
  })
}

export function resetPasswordByEmail(data) {
  return request({
    url: '/system/password/reset',
    method: 'post',
    data,
  })
}

export function getInfo() {
  return request({ url: '/getInfo', method: 'get' })
}

export function getRouters() {
  return request({ url: '/getRouters', method: 'get' })
}

export function logout() {
  return request({ url: '/logout', method: 'post' })
}

export function getCaptcha() {
  return request({ url: '/captchaImage', method: 'get' })
}
