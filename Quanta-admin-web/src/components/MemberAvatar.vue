<script setup>
import { computed, ref, watch } from 'vue'
import { memberAvatars } from '@/utils/member-avatars'
import { assetUrl } from '@/utils/member-profile'

const props = defineProps({ memberId: { type: [String, Number], default: '' }, src: { type: String, default: '' }, name: { type: String, default: '塔员' }, size: { type: Number, default: 44 }, eager: Boolean })
const failed = ref(false)
const url = computed(() => memberAvatars.get(Number(props.memberId)) ?? assetUrl(props.src))
const letter = computed(() => Array.from(props.name.trim() || '塔员')[0])
const style = computed(() => ({ '--member-avatar-size': `${Math.max(20, Math.min(128, props.size || 44))}px` }))
watch(url, () => { failed.value = false })
</script>

<template>
  <span class="member-avatar" :style="style" role="img" :aria-label="`${name}的头像`">
    <img v-if="url && !failed" :src="url" alt="" :loading="eager ? 'eager' : 'lazy'" decoding="async" referrerpolicy="no-referrer" @error="failed = true" />
    <span v-else aria-hidden="true">{{ letter }}</span>
  </span>
</template>

<style scoped>
.member-avatar { display: inline-grid; width: var(--member-avatar-size); height: var(--member-avatar-size); min-width: var(--member-avatar-size); padding: 0; overflow: hidden; flex: 0 0 var(--member-avatar-size); border-radius: 24%; background: var(--portal-orange-soft, #fff1e7); color: var(--portal-orange-strong, #d95400); font-size: calc(var(--member-avatar-size) * .38); font-weight: 800; line-height: 1; place-items: center; vertical-align: middle; }
.member-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; }
</style>
