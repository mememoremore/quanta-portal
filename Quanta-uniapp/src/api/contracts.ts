export interface AjaxResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface TableResponse<T> {
  code: number
  msg: string
  rows: T[]
  total: number
}

export interface SysUserDto {
  userId?: number | string
  userName?: string
  nickName?: string
  avatar?: string
  memberNo?: string
  memberDepartment?: string
  memberTitle?: string
  memberCohort?: string
  studentNo?: string
  className?: string
  isQuantaMember?: string
}

export interface GetInfoResponse extends Omit<AjaxResponse<never>, 'data'> {
  user: SysUserDto
  roles?: string[]
  permissions?: string[]
}
