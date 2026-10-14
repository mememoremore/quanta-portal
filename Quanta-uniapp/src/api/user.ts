import request from '../utils/request'
import type { UserRole } from '../types/session'
import type { GetInfoResponse } from './contracts'
import { mapSysUserProfile } from './mappers'

export type ChangePasswordData = {
  oldPassword: string
  newPassword: string
}

export const submitChangePasswordApi = (data: ChangePasswordData) => {
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'PUT',
    data,
  })
}

export const getCurrentProfileApi = async (role: UserRole) => {
  const response = await request<GetInfoResponse>({ url: '/getInfo' })
  return mapSysUserProfile(response.user, role)
}
