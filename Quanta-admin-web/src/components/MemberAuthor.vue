<script setup>
import { computed, resolveComponent } from 'vue'
import { useRoute } from 'vue-router'
import MemberAvatar from './MemberAvatar.vue'
import { memberDestination } from '@/utils/member-profile'
import { sessionMemberId } from '@/utils/member-avatars'
const props = defineProps({ memberId: { type: [Number,String], default: '' }, name: { type: String, default:'塔员' }, avatar: { type: String, default:'' }, department: { type: String, default:'' }, size: { type: Number, default:44 }, avatarOnly: Boolean, nameOnly: Boolean })
const route = useRoute()
const linkComponent = resolveComponent('RouterLink')
const destination = computed(() => memberDestination(props.memberId, sessionMemberId.value))
const target = computed(() => ({ path:destination.value, state:{ memberReturn:route?.fullPath || '/member/community' } }))
</script>
<template>
  <component :is="destination ? linkComponent : 'span'" class="member-author" :to="destination ? target : undefined" :aria-label="destination ? `查看${name}的成员主页` : undefined">
    <MemberAvatar v-if="!nameOnly" :member-id="memberId" :src="avatar" :name="name" :size="size" /><span v-if="!avatarOnly" class="member-author__text"><strong>{{ name }}</strong><small v-if="department">{{ department }}</small></span>
  </component>
</template>
<style scoped>
.member-author { display:inline-flex; min-width:0; max-width:100%; align-items:center; gap:10px; color:inherit; text-decoration:none; border-radius:12px; }.member-author__text { display:flex; min-width:0; align-items:center; flex-wrap:wrap; gap:6px; }.member-author__text strong { font-size:14px; overflow-wrap:anywhere; }.member-author__text small { color:var(--portal-muted); font-size:12px; }.member-author:is(a):hover strong { color:var(--portal-orange-strong); }
@media(max-width:600px) { .member-author__text { align-items:flex-start; flex-direction:column; gap:4px; } }
</style>
