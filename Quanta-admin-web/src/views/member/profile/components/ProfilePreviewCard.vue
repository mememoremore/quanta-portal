<script setup>
import { computed } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'
import MemberAvatar from '@/components/MemberAvatar.vue'
import { normalizeProfile } from '@/utils/member-profile'
const props = defineProps({ profile: { type: Object, required: true } })
const fields = computed(() => normalizeProfile(props.profile))
</script>
<template>
  <article class="member-preview-card" aria-label="个人档案名片预览"><header><div><BrandLogo :size="28" decorative /><strong>QUANTA</strong></div><MemberAvatar :member-id="profile.id" :src="profile.avatar" :name="profile.name" :size="60" /></header><h2>{{ profile.name }}</h2><p v-if="fields.communityNickname" class="member-preview-card__nickname">社区昵称 · {{ fields.communityNickname }}</p><p class="member-preview-card__role">{{ profile.department }} · {{ profile.title }} · {{ profile.cohort }}</p><p class="member-preview-card__headline">{{ fields.headline || '写一句话，介绍独一无二的你。' }}</p><div class="member-preview-card__tags"><span v-for="skill in fields.skills.slice(0,5)" :key="skill">{{ skill }}</span></div><p v-if="fields.showContactEmail && fields.contactEmail" class="member-preview-card__email">{{ fields.contactEmail }}</p></article>
</template>
<style scoped>
.member-preview-card { position:relative; overflow:hidden; min-height:305px; padding:25px; border-radius:22px; background:linear-gradient(135deg,#202124,#34363c); color:#fff; }.member-preview-card::after { position:absolute; content:''; right:-66px; bottom:-78px; width:205px; height:205px; border:35px solid #ff6700; border-radius:52px; opacity:.18; transform:rotate(20deg); pointer-events:none; }.member-preview-card header { display:flex; align-items:center; justify-content:space-between; gap:10px; }.member-preview-card header > div { display:flex; align-items:center; gap:8px; }.member-preview-card strong { font-size:12px; letter-spacing:.12em; }.member-preview-card h2 { position:relative; z-index:1; margin:27px 0 6px; font-size:28px; overflow-wrap:anywhere; }.member-preview-card__nickname { position:relative; z-index:1; margin:0 0 10px; color:#d5d8df; font-size:12px; overflow-wrap:anywhere; }.member-preview-card__role { margin:0; color:#c8cbd1; font-size:12px; }.member-preview-card__headline { position:relative; z-index:1; margin:19px 0; font-size:13px; line-height:1.8; overflow-wrap:anywhere; }.member-preview-card__tags { display:flex; flex-wrap:wrap; gap:6px; }.member-preview-card__tags span { padding:5px 8px; border:1px solid #55575e; border-radius:7px; font-size:11px; }.member-preview-card__email { position:relative; z-index:1; padding-top:15px; margin-bottom:0; border-top:1px solid #51535a; color:#d9dbe1; font-size:12px; overflow-wrap:anywhere; }
</style>
