<script setup>
import { computed } from 'vue'
import { ChatDotSquare, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useForumReactions } from './reactions'
import ThumbIcon from './ThumbIcon.vue'

const props = defineProps({ post: { type: Object, required: true }, detail: Boolean })
const emit = defineEmits(['changed', 'comments'])
const reactions = useForumReactions()
const state = computed(() => reactions.states.get(Number(props.post.postId)) || { ...props.post, liked: false, favorited: false, reactionsReady: false })
const busy = computed(() => reactions.busy(props.post.postId))

async function toggle(kind) {
  try {
    if (await reactions.toggle(props.post.postId, kind)) emit('changed', kind)
  } catch (error) { ElMessage.error(error.message || '操作失败，请稍后重试') }
}
</script>

<template>
  <div class="community-reactions" :class="{ 'is-detail': detail }" aria-label="帖子互动">
    <button type="button" data-reaction="liked" :aria-pressed="state.liked === true" :aria-label="`${state.liked ? '取消点赞' : '点赞'}，${state.likeCount || 0} 人点赞`" :disabled="busy || !state.reactionsReady" @click="toggle('liked')">
      <ThumbIcon :class="{ 'is-filled': state.liked }" />
      <span>{{ detail ? (state.liked ? '已赞 ' : '点赞 ') : '' }}{{ state.likeCount || 0 }}</span>
    </button>
    <button type="button" data-reaction="comments" :aria-label="`查看 ${post.commentCount || 0} 条讨论`" @click="emit('comments')"><ChatDotSquare /><span>{{ detail ? '讨论 ' : '' }}{{ post.commentCount || 0 }}</span></button>
    <button type="button" data-reaction="favorites" :aria-pressed="state.favorited === true" :aria-label="state.favorited ? '取消收藏' : '收藏帖子'" :disabled="busy || !state.reactionsReady" @click="toggle('favorites')"><component :is="state.favorited ? StarFilled : Star" :class="{ 'is-filled': state.favorited }" /><span>{{ state.favorited ? '已收藏' : '收藏' }}</span></button>
    <span v-if="!state.reactionsReady" class="community-reactions__unavailable">互动暂未开放</span>
  </div>
</template>
