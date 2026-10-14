<script setup>
import { computed, useAttrs } from 'vue'
import { usePermission } from '@/composables/usePermission'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  permissions: {
    type: [String, Array],
    default: () => [],
  },
  roles: {
    type: [String, Array],
    default: () => [],
  },
})

const attrs = useAttrs()
const { hasAny, hasRole } = usePermission()
const visible = computed(() => {
  const requiredRoles = Array.isArray(props.roles) ? props.roles : [props.roles]
  const hasRequiredRole = !requiredRoles.length || requiredRoles.some(hasRole)
  return hasAny(props.permissions) && hasRequiredRole
})
</script>

<template>
  <ElButton v-if="visible" v-bind="attrs">
    <slot />
  </ElButton>
</template>
