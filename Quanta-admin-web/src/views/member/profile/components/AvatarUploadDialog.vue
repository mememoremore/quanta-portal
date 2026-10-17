<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { avatarPreview, cropAvatar, readAvatarFile } from '../avatar-image'

const props = defineProps({ modelValue: Boolean, uploading: Boolean, error: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'confirm'])
const picker = ref(null), selected = ref(null), image = ref(null), processing = ref(false), message = ref('')
const transform = ref({ zoom: 1, x: 0, y: 0 })
let generation = 0, drag = null
const busy = computed(() => props.uploading || processing.value)
const preview = computed(() => image.value ? avatarPreview(image.value, transform.value) : null)
const imageStyle = computed(() => preview.value ? { width: `${preview.value.width / 256 * 100}%`, height: `${preview.value.height / 256 * 100}%`, left: `calc(50% + ${preview.value.x / 256 * 100}%)`, top: `calc(50% + ${preview.value.y / 256 * 100}%)` } : {})

function close() { if (!busy.value) emit('update:modelValue', false) }
watch(() => props.modelValue, (open) => {
  if (!open) { generation++; selected.value = image.value = null; message.value = ''; transform.value = { zoom: 1, x: 0, y: 0 } }
})
onBeforeUnmount(() => { generation++ })
async function choose(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const current = ++generation
  processing.value = true
  message.value = ''
  try {
    const result = await readAvatarFile(file)
    if (current !== generation) return
    selected.value = result.src; image.value = result.image
    transform.value = { zoom: 1, x: 0, y: 0 }
  } catch (error) { if (current === generation) message.value = error.message }
  finally { if (current === generation) processing.value = false }
}
function startDrag(event) {
  if (!preview.value || busy.value) return
  const ratio = 256 / event.currentTarget.getBoundingClientRect().width
  drag = { id: event.pointerId, x: event.clientX, y: event.clientY, startX: preview.value.x, startY: preview.value.y, ratio }
  event.currentTarget.setPointerCapture(event.pointerId)
}
function moveDrag(event) {
  if (!drag || event.pointerId !== drag.id) return
  const next = avatarPreview(image.value, { zoom: transform.value.zoom, x: drag.startX + (event.clientX - drag.x) * drag.ratio, y: drag.startY + (event.clientY - drag.y) * drag.ratio })
  transform.value = { zoom: transform.value.zoom, x: next.x, y: next.y }
}
async function confirm() {
  if (!image.value || busy.value) return
  processing.value = true; message.value = ''
  try { emit('confirm', await cropAvatar(image.value, transform.value)) }
  catch (error) { message.value = error.message }
  finally { processing.value = false }
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="更换头像" width="min(440px, calc(100vw - 32px))" :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="close">
    <div class="avatar-crop__stage">
      <div class="avatar-crop__frame" @pointerdown="startDrag" @pointermove="moveDrag" @pointerup="drag = null" @pointercancel="drag = null">
        <div v-if="selected" class="avatar-crop__coordinate"><img :src="selected" :style="imageStyle" alt="待上传头像裁剪预览" draggable="false" /></div>
        <span v-else>选择一张图片</span>
      </div>
    </div>
    <label class="avatar-crop__zoom">缩放<input v-model.number="transform.zoom" aria-label="头像缩放" type="range" min="1" max="3" step="0.05" :disabled="!image || busy" /></label>
    <p class="avatar-crop__hint">拖动图片调整位置。支持 JPG、PNG、WebP，最大 5 MB；确认后更新塔员门户内的头像。</p>
    <p v-if="message || error" class="avatar-crop__error" role="alert">{{ message || error }}</p>
    <input ref="picker" type="file" accept="image/jpeg,image/png,image/webp" aria-label="选择头像图片" hidden :disabled="busy" @change="choose" />
    <template #footer><div class="avatar-crop__actions"><button class="member-secondary-button" type="button" :disabled="busy" @click="picker.click()">{{ selected ? '重新选择' : '选择图片' }}</button><div><button class="member-secondary-button" type="button" :disabled="busy" @click="close">取消</button><button class="member-primary-button" type="button" :disabled="!image || busy" @click="confirm">{{ busy ? '处理中…' : '确认上传' }}</button></div></div></template>
  </el-dialog>
</template>

<style scoped>
.avatar-crop__stage { display: grid; padding: 18px; border-radius: 16px; background: #f6f7f9; place-items: center; }
.avatar-crop__frame { position: relative; width: min(256px, 100%); aspect-ratio: 1; overflow: hidden; border: 2px solid #ff6700; border-radius: 24%; background: #fff1e7; color: #99532a; display: grid; place-items: center; touch-action: none; user-select: none; cursor: grab; }
.avatar-crop__coordinate { position: absolute; width: 100%; height: 100%; left: 50%; top: 50%; transform: translate(-50%,-50%); }
.avatar-crop__coordinate img { position: absolute; max-width: none; transform: translate(-50%,-50%); pointer-events: none; }
.avatar-crop__zoom { display: flex; align-items: center; gap: 16px; margin: 20px 0 12px; }.avatar-crop__zoom input { flex: 1; min-width: 0; accent-color: #ff6700; }
.avatar-crop__hint { color: #77808d; font-size: 13px; line-height: 1.8; }.avatar-crop__error { color: #b42318; line-height: 1.7; }
.avatar-crop__actions, .avatar-crop__actions > div { display: flex; align-items: center; gap: 8px; }.avatar-crop__actions { justify-content: space-between; flex-wrap: wrap; }
</style>
