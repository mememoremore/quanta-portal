import { LoginData, LoginResponse } from '../types/auth'
import request from '../utils/request'
import { USE_LOGIN_MOCK } from '../config/runtime'

export const loginApi = (data: LoginData): Promise<LoginResponse> => {
  // 仅当 VITE_USE_MOCK=true 时启用演示登录。
  if (USE_LOGIN_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => resolve({ code: 200, msg: '演示登录成功', token: `mock-${data.loginType}-${data.username}` }), 260)
    })
  }
  return request<LoginResponse>({
    url: '/login',
    method: 'POST',
    data,
  })
}


export const towerMemberLoginApi = loginApi
