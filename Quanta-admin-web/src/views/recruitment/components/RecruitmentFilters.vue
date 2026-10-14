<script setup>
defineProps({
  department: { type: String, default: '' },
  keyword: { type: String, default: '' },
  departments: { type: Array, default: () => [] },
  visibleCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
})

const emit = defineEmits(['update:department', 'update:keyword', 'search', 'reset'])
</script>

<template>
  <div class="recruitment-filters" data-testid="recruitment-filters">
    <div class="recruitment-filters__fields">
      <ElSelect
        :model-value="department"
        class="recruitment-filters__department"
        clearable
        placeholder="全部部门"
        aria-label="筛选部门"
        @update:model-value="emit('update:department', $event || '')"
      >
        <ElOption
          v-for="item in departments"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
      <ElInput
        :model-value="keyword"
        data-testid="recruitment-keyword"
        clearable
        placeholder="输入学号或姓名"
        aria-label="学号或姓名"
        @update:model-value="emit('update:keyword', $event)"
        @keyup.enter="emit('search')"
      />
      <ElButton
        type="primary"
        class="recruitment-filters__search"
        data-testid="recruitment-search"
        @click="emit('search')"
      >
        搜索
      </ElButton>
      <ElButton data-testid="recruitment-reset" @click="emit('reset')">重置</ElButton>
    </div>
    <span class="recruitment-filters__count">当前显示 {{ visibleCount }} / {{ totalCount }} 人</span>
  </div>
</template>
