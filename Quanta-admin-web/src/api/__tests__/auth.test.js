import { describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { registerFreshman, sendRegisterEmailCode } from '../auth'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('authentication API', () => {
  it('registers a freshman with the student number as username', async () => {
    request.mockResolvedValue({ code: 200 })

    await registerFreshman({
      studentNo: '20241003193',
      email: 'freshman@example.com',
      emailCode: '123456',
      password: 'secret123',
    })

    expect(request).toHaveBeenCalledWith({
      url: '/register',
      method: 'post',
      data: {
        studentNo: '20241003193',
        username: '20241003193',
        email: 'freshman@example.com',
        emailCode: '123456',
        password: 'secret123',
        loginType: '0',
      },
    })
  })

  it('sends a registration email code bound to the student number', async () => {
    request.mockResolvedValue({ code: 200 })

    await sendRegisterEmailCode({
      studentNo: '20241003193',
      email: 'freshman@example.com',
    })

    expect(request).toHaveBeenCalledWith({
      url: '/register/emailCode',
      method: 'post',
      data: {
        studentNo: '20241003193',
        email: 'freshman@example.com',
      },
    })
  })
})
