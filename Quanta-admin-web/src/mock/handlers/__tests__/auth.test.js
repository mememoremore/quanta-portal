import { describe, expect, it } from 'vitest'
import { authHandlers } from '../auth'
import { mockRequest } from '@/mock'

function handler(method, path) {
  return authHandlers.find((item) => item.method === method && item.path === path)
}

function config(token) {
  return { headers: { Authorization: `Bearer ${token}` } }
}

describe('auth mock handlers', () => {
  it('returns the confirmed manager menu', () => {
    const response = handler('get', '/getRouters').handle(
      config('mock-token-product-interviewer'),
    )

    expect(response.code).toBe(200)
    expect(response.data.map((route) => route.name)).toEqual([
      'Dashboard',
      'Members',
      'Recruitment',
      'ActivitySignups',
      'LearningMaterials',
      'BookBorrows',
    ])
  })

  it('supports the new-email verification flow in explicit full mock mode', () => {
    const sendResponse = handler('post', '/system/user/profile/emailCode').handle({
      ...config('mock-token-admin'),
      data: { email: 'new@example.com' },
    })
    const updateResponse = handler('put', '/system/user/profile/updateEmail').handle({
      ...config('mock-token-admin'),
      data: { email: 'new@example.com', emailCode: '123456' },
    })

    expect(sendResponse.code).toBe(200)
    expect(updateResponse.code).toBe(200)
  })
})

describe('partial Web mock', () => {
  it('accepts a real session and returns backend-compatible cohort values', async () => {
    const response = await mockRequest({
      url: '/qt/member/cohorts',
      method: 'get',
      __partialMock: true,
      headers: { Authorization: 'Bearer real-backend-token' },
    })

    expect(response.code).toBe(200)
    expect(response.data.cohorts.map((item) => item.id)).toContain('21st')
  })
})
