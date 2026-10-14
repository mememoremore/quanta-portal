import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

export function hasAnyPermission(required = [], granted = []) {
  const requested = Array.isArray(required) ? required : [required]
  if (!requested.length) return true
  if (granted.includes('*:*:*')) return true
  return requested.some((permission) => granted.includes(permission))
}

export function hasAllPermissions(required = [], granted = []) {
  const requested = Array.isArray(required) ? required : [required]
  if (!requested.length) return true
  if (granted.includes('*:*:*')) return true
  return requested.every((permission) => granted.includes(permission))
}

export function isCeoRole(role) {
  return role === 'ceo' || role === 'admin'
}

export function hasBusinessRole(required, granted = []) {
  if (required === 'ceo') return granted.some(isCeoRole)
  return granted.includes(required) || granted.includes('admin')
}

export function usePermission() {
  const userStore = useUserStore()
  const { permissions, roles } = storeToRefs(userStore)

  return {
    hasAny: (required) => hasAnyPermission(required, permissions.value),
    hasAll: (required) => hasAllPermissions(required, permissions.value),
    hasRole: (role) => hasBusinessRole(role, roles.value),
    hasExactRole: (role) => roles.value.includes(role),
  }
}
