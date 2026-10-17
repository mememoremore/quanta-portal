import { afterEach, vi } from 'vitest'
import { refreshMock } from '@/mock/handlers/auth'
import { clearMemberAvatars } from '@/utils/member-avatars'

afterEach(() => {
  clearMemberAvatars()
  localStorage.clear()
  refreshMock.mode = 'fail'
  refreshMock.calls = 0
  refreshMock.token = ''
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})

