<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { sessionMemberId } from '@/utils/member-avatars'
import { createProtectedImages, isProtectedForumImage } from './protected-images'

const props = defineProps({ src: { type: String, required: true }, alt: { type: String, default: '帖子配图' } })
const root = ref(null)
let loader = createProtectedImages()
watch([root, () => props.src], () => { if (root.value) loader.sync(root.value) }, { flush: 'post' })
watch(sessionMemberId, () => { loader.destroy(); loader = createProtectedImages() })
onBeforeUnmount(() => loader.destroy())
</script>

<template>
  <span ref="root" class="community-image"><img v-if="isProtectedForumImage(src)" :data-forum-image="src" :alt="alt" loading="lazy" /><img v-else :src="src" :alt="alt" loading="lazy" /></span>
</template>
