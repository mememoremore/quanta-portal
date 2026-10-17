import { reactive, ref } from 'vue'
import { assetUrl, validMemberId } from './member-profile'

// 只缓存本会话刚更新的头像，既不持久化，也不为评论逐条发请求。
export const memberAvatars = reactive(new Map())
export const sessionMemberId = ref(null)
export function setMemberAvatar(id, url) { if (validMemberId(id)) memberAvatars.set(Number(id), assetUrl(url)) }
export function clearMemberAvatars() { memberAvatars.clear(); sessionMemberId.value = null }
