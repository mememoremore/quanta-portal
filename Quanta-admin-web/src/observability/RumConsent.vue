<script setup>
import { ref } from 'vue'
import { isRumAvailable, loadRumSdk, readRumConsent, saveRumConsent } from './rum'
import './rum.css'

const props = defineProps({
  enabled: { type: Boolean, default: () => isRumAvailable() },
  reloadPage: { type: Function, default: () => window.location.reload() },
})

const choice = ref(readRumConsent())
const expanded = ref(props.enabled && choice.value === null)

function accept() {
  saveRumConsent('accepted')
  choice.value = 'accepted'
  expanded.value = false
  loadRumSdk()
}

function decline() {
  const wasAccepted = choice.value === 'accepted'
  saveRumConsent('declined')
  choice.value = 'declined'
  expanded.value = false
  if (wasAccepted) props.reloadPage()
}
</script>

<template>
  <aside v-if="enabled" class="rum-consent" aria-label="性能监控设置">
    <section v-if="expanded" class="rum-consent__panel" aria-label="性能监控选择">
      <div class="rum-consent__eyebrow">QUANTA · PRIVACY</div>
      <h2>是否开启性能监控？</h2>
      <p>
        经你同意后，阿里云用户体验监控 SDK 会收集页面性能、错误、接口和点击行为等信息。
        暂不同意也能正常使用网站。
        <a
          href="https://terms.alicdn.com/legal-agreement/terms/privacy_policy_full/20240308104330389/20240308104330389.html"
          target="_blank"
          rel="noopener noreferrer"
        >
          查看 SDK 隐私说明
        </a>
      </p>
      <div class="rum-consent__actions">
        <button type="button" class="rum-consent__accept" data-rum-choice="accepted" @click="accept">
          同意监控
        </button>
        <button type="button" class="rum-consent__decline" data-rum-choice="declined" @click="decline">
          {{ choice === 'accepted' ? '停止监控' : '暂不同意' }}
        </button>
        <button
          v-if="choice !== null"
          type="button"
          class="rum-consent__close"
          aria-label="关闭监控设置"
          @click="expanded = false"
        >
          ×
        </button>
      </div>
    </section>
    <button
      v-else
      type="button"
      class="rum-consent__settings"
      data-rum-settings
      @click="expanded = true"
    >
      监控设置
    </button>
  </aside>
</template>
