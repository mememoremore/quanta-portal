import { hasAnyPermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'

export const permissionDirective = {
  mounted(element, binding) {
    const userStore = useUserStore()
    if (!hasAnyPermission(binding.value, userStore.permissions)) {
      element.parentNode?.removeChild(element)
    }
  },
}

