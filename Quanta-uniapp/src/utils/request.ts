import { TOKEN_KEY } from './storage'
import { clearSession } from '../stores/user'
import { API_BASE_URL } from '../config/runtime'

/** 与后端约定的业务成功码 */
const SUCCESS_CODE = 200

let handlingUnauthorized = false

export const handleUnauthorized = () => {
  clearSession()
  if (handlingUnauthorized) return
  handlingUnauthorized = true
  uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
  uni.reLaunch({
    url: '/pages/login/select-role',
    complete: () => {
      setTimeout(() => { handlingUnauthorized = false }, 300)
    },
  })
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

interface RequestOptions {
  url: string
  method?: Method
  data?: any
  header?: Record<string, string>
  timeout?: number
}

/**
 * 统一请求封装（基于 uni.request，兼容微信小程序）。
 * 自动携带 token，统一校验业务 code === 200。
 */
const request = <T = any>(options: RequestOptions): Promise<T> => {
  const { url, method = 'GET', data, header, timeout = 10000 } = options

  return new Promise<T>((resolve, reject) => {
    const token = uni.getStorageSync(TOKEN_KEY)

    const finalHeader: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(header || {}),
    }
    if (token) {
      finalHeader.Authorization = `Bearer ${token}`
    }

    uni.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: finalHeader,
      timeout,
      success: (res) => {
        const body = res.data as any
        if (res.statusCode === 401 || body?.code === 401) {
          handleUnauthorized()
          reject(new Error('登录已失效'))
          return
        }
        if (res.statusCode === 403 || body?.code === 403) {
          reject(new Error('暂无权限执行此操作'))
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(body?.msg || `请求失败（${res.statusCode}）`))
          return
        }
        // 业务失败：HTTP 200，但 body 里 code !== 200
        if (!body || body.code !== SUCCESS_CODE) {
          reject(new Error(body?.msg || '请求失败'))
          return
        }
        // 业务成功：把 body 返回给调用方（含 token、msg 等）
        resolve(body as T)
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '网络异常，请稍后重试'))
      },
    })
  })
}

// 导出请求函数
export default request
