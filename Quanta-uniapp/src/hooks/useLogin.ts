import type { LoginResponse } from '../types/auth'
import type { UserProfile, UserRole } from '../types/session'
import { clearSession, setSession, updateProfile } from '../stores/user'
import { homeForRole } from '../utils/authGuard'
import { getCurrentProfileApi } from '../api/user'
import { departmentLabel } from '../api/mappers'

type LoginResponseWithUser = LoginResponse & { user?: Partial<UserProfile> }

export const completeLogin = async (response: LoginResponseWithUser, role: UserRole, account: string) => {
  const fallbackProfile: UserProfile = {
    id: response.memberNo || response.studentNo || account,
    account,
    name: account,
    role,
    department: departmentLabel(response.memberDepartment),
    batch: response.memberCohort,
  }
  const profile: UserProfile = { ...fallbackProfile, ...(response.user || {}), role }
  setSession(response.token, role, profile)
  try {
    const serverProfile = await getCurrentProfileApi(role)
    updateProfile(serverProfile)
    return serverProfile
  } catch {
    return profile
  }
}

export const logout = () => {
  clearSession()
  uni.reLaunch({ url: '/pages/login/select-role' })
}

export const goToRoleHome = (role: UserRole) => {
  uni.reLaunch({ url: homeForRole(role) })
}
