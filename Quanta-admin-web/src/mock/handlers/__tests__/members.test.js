import { describe, expect, it } from 'vitest'
import { memberHandlers } from '../members'

function handler(method, path) {
  return memberHandlers.find(
    (item) => item.method === method && (item.path === path || item.match?.(path)),
  )
}

function config(token, extra = {}) {
  return { headers: { Authorization: `Bearer ${token}` }, ...extra }
}

describe('member high-risk permissions', () => {
  it('treats the RuoYi administrator as CEO', () => {
    const response = handler('put', '/system/user/resetPwd').handle(
      config('mock-token-admin', { data: { userId: 2101, password: '123456' } }),
    )
    expect(response.code).toBe(200)
  })

  it('allows the CEO to reset a member password', () => {
    const response = handler('put', '/system/user/resetPwd').handle(
      config('mock-token-ceo', { data: { userId: 2101, password: '123456' } }),
    )
    expect(response.code).toBe(200)
  })

  it('rejects a password reset without a new password', () => {
    const response = handler('put', '/system/user/resetPwd').handle(
      config('mock-token-ceo', { data: { userId: 2101 } }),
    )
    expect(response.code).toBe(400)
  })
})
