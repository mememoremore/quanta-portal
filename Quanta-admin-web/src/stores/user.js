import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/token'
import {
  clearAudience,
  getAudience,
  loginTypeFor,
  setAudience,
} from '@/utils/session-audience'

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
  const displayName = computed(() => user.value?.nickName || user.value?.userName || '未登录')
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
    token.value = response.token
    audience.value = requestedAudience
    setToken(response.token)
    setAudience(requestedAudience)
    return response
  }

  async function fetchUserInfo() {
    const response = await authApi.getInfo()
    user.value = response.user || null
    roles.value = response.roles || []
    permissions.value = response.permissions || []
    return response
  }

  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } finally {
      reset()
    }
  }

  function reset() {
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
    logout,
    reset,
  }
})
