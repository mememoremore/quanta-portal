export type UserRole = 'freshman' | 'tower'

export interface UserProfile {
  id: string
  name: string
  account: string
  role: UserRole
  department?: string
  batch?: string
  avatar?: string
}

export interface SessionSnapshot {
  token: string
  role: UserRole | null
  profile?: UserProfile | null
}

export const isUserRole = (value: unknown): value is UserRole =>
  value === 'freshman' || value === 'tower'

export const normalizeSession = (snapshot: {
  token?: unknown
  role?: unknown
  profile?: UserProfile | null
}): SessionSnapshot => {
  if (typeof snapshot.token !== 'string' || !snapshot.token || !isUserRole(snapshot.role)) {
    return { token: '', role: null, profile: null }
  }

  return {
    token: snapshot.token,
    role: snapshot.role,
    profile: snapshot.profile || null,
  }
}
