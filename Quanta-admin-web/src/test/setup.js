import { afterEach, vi } from 'vitest'

afterEach(() => {
  localStorage.clear()
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})

