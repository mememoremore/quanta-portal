import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/token'
import { isDualTokenEnabled } from '@/utils/request'
import { clearMemberAvatars, setMemberAvatar, sessionMemberId } from '@/utils/member-avatars'
import {
  clearAudience,
  getAudience,
  loginTypeFor,
  setAudience,
} from '@/utils/session-audience'
import { displayPersonName } from '@/utils/person-name'

const departmentAliases = {
  产品: 'PRODUCT',
  产品部: 'PRODUCT',
  设计: 'DESIGN',
  设计部: 'DESIGN',
  前端: 'FRONTEND',
  前端部: 'FRONTEND',
  '全栈（前端）': 'FRONTEND',
  后端: 'BACKEND',
  后端部: 'BACKEND',
  研发部: 'BACKEND',
  '全栈（后端）': 'BACKEND',
}

export function normalizeDepartmentCode(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  const code = text.toUpperCase()
  if (['PRODUCT', 'DESIGN', 'FRONTEND', 'BACKEND'].includes(code)) return code
  return departmentAliases[text] || ''
}

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const user = ref(null)
  const roles = ref([])
  const permissions = ref([])
  const audience = ref(getAudience())

  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => displayPersonName(user.value || {}, '未登录'))
  const departmentCode = computed(() =>
    normalizeDepartmentCode(
      user.value?.deptCode ||
        user.value?.dept?.deptCode ||
        user.value?.memberDepartmentCode ||
        user.value?.memberDepartment,
    ),
  )
  const departmentName = computed(
    () => user.value?.dept?.deptName || user.value?.memberDepartment || '',
  )
  const serverAudience = computed(() => {
    if (!user.value) return ''
    const memberFlag = user.value.isQuantaMember
    if (memberFlag !== undefined && memberFlag !== null && memberFlag !== '') {
      return memberFlag === true || memberFlag === 1 || String(memberFlag) === '1'
        ? 'member'
        : 'freshman'
    }
    const managementRoles = new Set(['admin', 'ceo', 'qt_mgmt', 'qt_manager'])
    return roles.value.some((role) => managementRoles.has(role)) || permissions.value.length > 0
      ? 'member'
      : 'freshman'
  })
  const canAccessAdmin = computed(() => {
    if (serverAudience.value !== 'member') return false
    const managementRoles = new Set(['admin', 'ceo', 'qt_mgmt', 'qt_manager'])
    return (
      roles.value.some((role) => managementRoles.has(role)) ||
      permissions.value.includes('*:*:*')
    )
  })

  async function login(credentials, requestedAudience) {
    const response = await authApi.login({
      ...credentials,
      loginType: loginTypeFor(requestedAudience),
    })
    clearMemberAvatars()
    token.value = response.token
    audience.value = requestedAudience
    setToken(response.token)
    setAudience(requestedAudience)
    return response
  }

  async function restoreSession() {
    if (!isDualTokenEnabled()) return false
    try {
      const response = await authApi.refreshSession()
      token.value = response.token
      return Boolean(response.token)
    } catch {
      reset()
      return false
    }
  }

  async function fetchUserInfo() {
    const response = await authApi.getInfo()
    const identity = response?.user
    const validId = identity
      && ['string', 'number'].includes(typeof identity.userId)
      && Number.isSafeInteger(Number(identity.userId))
      && Number(identity.userId) > 0
    const validName = typeof identity?.userName === 'string' && identity.userName.trim().length > 0
    if (!identity || typeof identity !== 'object' || Array.isArray(identity) || (!validId && !validName)) {
      throw new Error('无法确认账号身份，请重新登录')
    }
    user.value = identity
    sessionMemberId.value = user.value?.userId || null
    if (user.value) setMemberAvatar(user.value.userId, user.value.avatar || '')
    roles.value = response.roles || []
    permissions.value = response.permissions || []
    return response
  }

  async function logout() {
    try {
      if (token.value || isDualTokenEnabled()) await authApi.logout()
    } finally {
      reset()
    }
  }

  function reset() {
    clearMemberAvatars()
    token.value = ''
    user.value = null
    roles.value = []
    permissions.value = []
    audience.value = ''
    removeToken()
    clearAudience()
  }

  return {
    token,
    user,
    roles,
    permissions,
    audience,
    isAuthenticated,
    displayName,
    departmentCode,
    departmentName,
    serverAudience,
    canAccessAdmin,
    login,
    fetchUserInfo,
    restoreSession,
    logout,
    reset,
  }
})
