<script setup>
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  type: { type: String, default: 'success' },
  title: { type: String, default: '' },
  message: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

function close() {
  emit('update:modelValue', false)
}

function handleKeydown(event) {
  if (event.key === 'Escape') close()
}

watch(
  () => props.modelValue,
  (visible) => {
    document[visible ? 'addEventListener' : 'removeEventListener']('keydown', handleKeydown)
  },
  { immediate: true },
)

onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <div v-if="modelValue" class="portal-notice-dialog" :class="`is-${type}`">
    <button class="portal-notice-dialog__backdrop" type="button" aria-label="关闭提示" @click="close"></button>
    <section
      class="portal-notice-dialog__panel"
      role="alertdialog"
      aria-modal="true"
      :aria-labelledby="title ? 'portal-notice-title' : undefined"
      aria-describedby="portal-notice-message"
    >
      <div class="portal-notice-dialog__icon" aria-hidden="true">
        <svg v-if="type === 'success'" viewBox="0 0 24 24"><path d="m6.5 12.5 3.3 3.3 7.7-8" /></svg>
        <svg v-else viewBox="0 0 24 24"><path d="M12 7v6M12 17h.01" /></svg>
      </div>
      <div class="portal-notice-dialog__copy">
        <p class="portal-notice-dialog__eyebrow">{{ type === 'success' ? '操作完成' : '需要处理' }}</p>
        <h2 v-if="title" id="portal-notice-title">{{ title }}</h2>
        <p id="portal-notice-message">{{ message }}</p>
      </div>
      <button class="portal-notice-dialog__confirm" type="button" data-testid="notice-confirm" @click="close">
        知道了
      </button>
    </section>
  </div>
</template>

<style scoped>
.portal-notice-dialog { position: fixed; z-index: 3200; inset: 0; display: grid; padding: 24px; place-items: center; }
.portal-notice-dialog__backdrop { position: absolute; inset: 0; width: 100%; height: 100%; padding: 0; background: rgb(21 23 28 / 58%); border: 0; backdrop-filter: blur(4px); cursor: default; }
.portal-notice-dialog__panel { position: relative; display: grid; width: min(440px, 100%); padding: 34px; color: #202124; background: #fff; border: 1px solid #eceef2; border-radius: 22px; box-shadow: 0 28px 80px rgb(20 22 27 / 24%); grid-template-columns: 54px 1fr; gap: 18px; animation: notice-enter .22s ease-out both; }
.portal-notice-dialog__icon { display: grid; width: 54px; height: 54px; color: #fff; background: #ff6700; border-radius: 17px; box-shadow: 0 12px 24px rgb(255 103 0 / 24%); place-items: center; }
.portal-notice-dialog.is-error .portal-notice-dialog__icon { background: #e44949; box-shadow: 0 12px 24px rgb(228 73 73 / 22%); }
.portal-notice-dialog__icon svg { width: 29px; height: 29px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2.4; }
.portal-notice-dialog__copy { min-width: 0; }
.portal-notice-dialog__eyebrow { margin: 1px 0 6px; color: #e45d00; font-size: 11px; font-weight: 800; letter-spacing: .14em; }
.portal-notice-dialog.is-error .portal-notice-dialog__eyebrow { color: #d13f3f; }
.portal-notice-dialog h2 { margin: 0; font-size: 24px; line-height: 1.3; }
.portal-notice-dialog__copy > p:last-child { margin: 10px 0 0; color: #6d7280; font-size: 14px; line-height: 1.75; }
.portal-notice-dialog__confirm { grid-column: 1 / -1; min-height: 44px; margin-top: 8px; color: #fff; font: inherit; font-weight: 700; background: #202124; border: 0; border-radius: 12px; cursor: pointer; transition: background .2s ease, transform .2s ease; }
.portal-notice-dialog__confirm:hover { background: #ff6700; transform: translateY(-1px); }
.portal-notice-dialog__confirm:focus-visible { outline: 3px solid rgb(255 103 0 / 22%); outline-offset: 3px; }
@keyframes notice-enter { from { opacity: 0; transform: translateY(12px) scale(.98); } }
@media (max-width: 520px) { .portal-notice-dialog { padding: 14px; } .portal-notice-dialog__panel { width: calc(100vw - 28px); max-height: calc(100dvh - 28px); padding: 24px 20px; overflow-y: auto; grid-template-columns: 44px minmax(0, 1fr); gap: 14px; } .portal-notice-dialog__icon { width: 44px; height: 44px; border-radius: 14px; } .portal-notice-dialog h2 { font-size: 20px; } .portal-notice-dialog__copy > p:last-child { font-size: 13px; } .portal-notice-dialog__confirm { min-height: 48px; } }
</style>
