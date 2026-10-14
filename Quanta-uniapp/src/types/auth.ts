/** 登录请求（与 Apifox /login 的 LoginBody 一致，已关闭验证码） */
export interface LoginData {
  username: string
  password: string
  /** 登录身份：0 新生 / 1 塔员 */
  loginType: string
}

/**
 * 登录成功响应。
 * TODO: 后端真实响应体尚未确定（后端未部署）。当前只确认含 token；
 *       若后端返回 loginType / isQuantaMember / 用户信息，需回来补充。
 */
export interface LoginResponse {
  code: number
  msg: string
  token: string
  isQuantaMember?: string
  memberNo?: string
  memberDepartment?: string
  memberTitle?: string
  memberCohort?: string
  studentNo?: string
  className?: string
}
