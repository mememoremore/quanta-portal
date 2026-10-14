<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:page', 'update:pageSize'])
const shouldRender = computed(() => props.total > props.pageSize)
</script>

<template>
  <div v-if="shouldRender" class="recruitment-pagination" data-testid="recruitment-pagination">
    <span>共 {{ total }} 位候选人</span>
    <ElPagination
      background
      :current-page="page"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      :total="total"
      :disabled="disabled"
      layout="sizes, prev, pager, next, jumper"
      @current-change="emit('update:page', $event)"
      @size-change="emit('update:pageSize', $event)"
    />
  </div>
</template>
