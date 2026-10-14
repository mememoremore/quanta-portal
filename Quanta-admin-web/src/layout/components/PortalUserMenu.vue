<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

defineProps({
  audience: { type: String, required: true },
})

const router = useRouter()
const userStore = useUserStore()
const { displayName } = storeToRefs(userStore)
const open = ref(false)
const menuRoot = ref()

function close() {
  open.value = false
}

function handleDocumentClick(event) {
  if (!menuRoot.value?.contains(event.target)) close()
}

async function logout() {
  close()
  await userStore.logout().catch(() => undefined)
  await router.replace('/')
}

onMounted(() => document.addEventListener('click', handleDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', handleDocumentClick))
</script>

<template>
  <div ref="menuRoot" class="portal-user-menu" @keydown.esc="close">
    <button
      class="portal-user-menu__trigger"
      type="button"
      aria-haspopup="menu"
      :aria-expanded="open"
      @click.stop="open = !open"
    >
      <span class="portal-user-menu__avatar" aria-hidden="true">{{ displayName.slice(0, 1) }}</span>
      <span class="portal-user-menu__name">{{ displayName }}</span>
      <span class="portal-user-menu__chevron" aria-hidden="true">⌄</span>
    </button>
    <div v-if="open" class="portal-user-menu__panel" role="menu">
      <RouterLink :to="`/${audience}/security`" role="menuitem" @click="close">
        账号与安全
      </RouterLink>
      <button type="button" role="menuitem" @click="logout">退出登录</button>
    </div>
  </div>
</template>
