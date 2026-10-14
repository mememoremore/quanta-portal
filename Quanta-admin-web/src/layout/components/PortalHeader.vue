<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PortalUserMenu from './PortalUserMenu.vue'
import quantaLogoUrl from '@/assets/quanta-logo.jpg'

const props = defineProps({
  audience: { type: String, required: true },
})

const route = useRoute()
const mobileOpen = ref(false)
const portalName = computed(() => (props.audience === 'freshman' ? '新生门户' : '塔员门户'))
const navigation = computed(() => props.audience === 'freshman'
  ? [
      { label: '新生首页', to: '/freshman/home' },
      { label: '加入我们', to: '/freshman/recruitment' },
      { label: 'Quanta 活动', to: '/freshman/events' },
      { label: '账号安全', to: '/freshman/security' },
    ]
  : [
      { label: '塔员首页', to: '/member/home' },
      { label: '通讯录', to: '/member/directory' },
      { label: '社区论坛', to: '/member/community' },
      { label: '个人中心', to: '/member/profile' },
      { label: '我的服务', to: '/member/services' },
      { label: '图书借阅', to: '/member/library' },
      { label: '塔服订购', to: '/member/clothing' },
      { label: '学习资料', to: '/member/materials' },
      { label: '招新报名', to: '/member/recruitment' },
    ])

watch(() => route.fullPath, () => {
  mobileOpen.value = false
})
</script>

<template>
  <header class="portal-header">
    <div class="portal-header__inner">
      <RouterLink class="portal-header__brand" to="/" aria-label="返回 Quanta 门户入口">
        <img :src="quantaLogoUrl" alt="Quanta 社团 Logo" />
        <span><strong>Quanta</strong><small>{{ portalName }}</small></span>
      </RouterLink>

      <nav class="portal-header__nav" data-testid="portal-desktop-nav" :aria-label="`${portalName}主导航`">
        <RouterLink v-for="item in navigation" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="portal-header__actions">
        <PortalUserMenu :audience="audience" />
        <button
          class="portal-header__mobile-trigger"
          type="button"
          data-testid="portal-mobile-menu"
          aria-controls="portal-mobile-navigation"
          :aria-expanded="mobileOpen"
          :aria-label="mobileOpen ? '关闭导航菜单' : '打开导航菜单'"
          @click="mobileOpen = !mobileOpen"
        >
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
    <nav
      v-if="mobileOpen"
      id="portal-mobile-navigation"
      class="portal-header__mobile-panel"
      data-testid="portal-mobile-panel"
      :aria-label="`${portalName}移动导航`"
    >
      <RouterLink v-for="item in navigation" :key="item.to" :to="item.to">
        {{ item.label }}
      </RouterLink>
    </nav>
  </header>
</template>
