import { describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/request'
import { changePassword, sendFreshmanEmailCode, updateFreshmanEmail } from '../account'

vi.mock('@/utils/request', () => ({ request: vi.fn() }))

describe('portal account API', () => {
  it('changes the password through the real profile endpoint', async () => {
    request.mockResolvedValue({ code: 200 })

    await changePassword({ oldPassword: 'old-secret', newPassword: 'new-secret' })

    expect(request).toHaveBeenCalledWith({
      url: '/system/user/profile/updatePwd',
      method: 'put',
      data: { oldPassword: 'old-secret', newPassword: 'new-secret' },
    })
  })

  it('sends a verification code to the new freshman email', async () => {
    request.mockResolvedValue({ code: 200 })

    await sendFreshmanEmailCode('freshman@example.com')

    expect(request).toHaveBeenCalledWith({
      url: '/system/user/profile/emailCode',
      method: 'post',
      data: { email: 'freshman@example.com' },
    })
  })

  it('updates the current freshman email with its verification code', async () => {
    request.mockResolvedValue({ code: 200 })

    await updateFreshmanEmail({ email: 'freshman@example.com', emailCode: '123456' })

    expect(request).toHaveBeenCalledWith({
      url: '/system/user/profile/updateEmail',
      method: 'put',
      data: { email: 'freshman@example.com', emailCode: '123456' },
    })
  })
})
