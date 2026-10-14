import { reactive } from 'vue'
import type { SessionSnapshot, UserProfile, UserRole } from '../types/session'
import { normalizeSession } from '../types/session'
import {
  IS_QUANTA_MEMBER_KEY,
  ROLE_KEY,
  SELECTED_ROLE_KEY,
  TOKEN_KEY,
  USER_PROFILE_KEY,
} from '../utils/storage'

export const sessionState = reactive<SessionSnapshot & { initialized: boolean }>({
  token: '',
  role: null,
  profile: null,
  initialized: false,
})

const readProfile = (): UserProfile | null => {
  const stored = uni.getStorageSync(USER_PROFILE_KEY)
  if (!stored) return null
  if (typeof stored === 'object') return stored as UserProfile
  try {
    return JSON.parse(stored) as UserProfile
  } catch {
    return null
  }
}

export const restoreSession = () => {
  const normalized = normalizeSession({
    token: uni.getStorageSync(TOKEN_KEY),
    role: uni.getStorageSync(ROLE_KEY),
    profile: readProfile(),
  })
  Object.assign(sessionState, normalized, { initialized: true })
  if (!normalized.token) clearStoredSession()
  return normalized
}

const clearStoredSession = () => {
  ;[TOKEN_KEY, ROLE_KEY, IS_QUANTA_MEMBER_KEY, SELECTED_ROLE_KEY, USER_PROFILE_KEY].forEach((key) => {
    uni.removeStorageSync(key)
  })
}

export const setSession = (token: string, role: UserRole, profile: UserProfile) => {
  Object.assign(sessionState, { token, role, profile, initialized: true })
  uni.setStorageSync(TOKEN_KEY, token)
  uni.setStorageSync(ROLE_KEY, role)
  uni.setStorageSync(IS_QUANTA_MEMBER_KEY, role === 'tower' ? '1' : '0')
  uni.setStorageSync(SELECTED_ROLE_KEY, role)
  uni.setStorageSync(USER_PROFILE_KEY, JSON.stringify(profile))
}

export const updateProfile = (profile: UserProfile) => {
  sessionState.profile = profile
  uni.setStorageSync(USER_PROFILE_KEY, JSON.stringify(profile))
}

export const clearSession = () => {
  clearStoredSession()
  Object.assign(sessionState, { token: '', role: null, profile: null, initialized: true })
}
