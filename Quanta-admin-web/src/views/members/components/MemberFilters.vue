<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  name: { type: String, default: '' },
})

const emit = defineEmits(['search'])
const keyword = ref(props.name)
let timer = null

watch(
  () => props.name,
  (value) => {
    keyword.value = value
  },
)

function scheduleSearch(value) {
  window.clearTimeout(timer)
  timer = window.setTimeout(() => emit('search', value.trim()), 300)
}

function clearSearch() {
  window.clearTimeout(timer)
  emit('search', '')
}

onBeforeUnmount(() => window.clearTimeout(timer))
</script>

<template>
  <ElInput
    v-model="keyword"
    class="member-search"
    size="small"
    clearable
    :prefix-icon="Search"
    placeholder="搜索成员姓名…"
    aria-label="搜索成员姓名"
    @input="scheduleSearch"
    @clear="clearSearch"
    @keyup.enter="emit('search', keyword.trim())"
  />
</template>

<style scoped>
.member-search {
  width: 220px;
}

:deep(.el-input__wrapper) {
  min-height: 32px;
  border-radius: 7px;
  box-shadow: 0 0 0 1px var(--quanta-border) inset;
}

:deep(.el-input__inner) {
  font-size: 12px;
}
</style>
