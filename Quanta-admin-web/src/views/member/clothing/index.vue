<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createClothingOrder, getClothingItems } from '@/api/portal/clothing'

const items = ref([])
const selectedItem = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const submitting = ref(false)
const selection = reactive({ color: '', size: '', quantity: 1 })

function selectItem(item) {
  selectedItem.value = item
  selection.color = item.colors[0] || ''
  selection.size = item.sizes[0] || ''
  selection.quantity = 1
}

async function loadItems() {
  loading.value = true
  errorMessage.value = ''
  try {
    items.value = await getClothingItems()
    if (items.value.length) selectItem(items.value[0])
  } catch (error) {
    errorMessage.value = error.message || '塔服款式加载失败'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  if (submitting.value || !selectedItem.value || !selection.color || !selection.size) return
  submitting.value = true
  try {
    const result = await createClothingOrder(selectedItem.value, { ...selection })
    if (result.status !== 'DRAFT') throw new Error('订单状态异常，请刷新后重试')
    ElMessage.success('订购草稿已保存')
  } catch (error) {
    ElMessage.error(error.message || '订购草稿保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadItems)
</script>

<template>
  <section class="member-service-page">
    <header class="member-service-heading"><div><p>QUANTA UNIFORM</p><h1>塔服订购</h1><span>选择款式信息并保存订购草稿，价格与付款安排以实物通知为准。</span></div></header>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="loadItems">重试</button></p>
    <div v-if="loading" class="member-service-state portal-card">正在加载塔服款式…</div>
    <div v-else-if="items.length" class="member-clothing-layout">
      <div class="member-clothing-grid">
        <button v-for="item in items" :key="item.itemId" type="button" class="member-clothing-card portal-card" :class="{ 'is-selected': selectedItem?.itemId === item.itemId }" @click="selectItem(item)">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="`${item.itemName}效果图`" />
          <div v-else class="member-clothing-card__placeholder" aria-hidden="true">Q</div>
          <span>{{ item.itemName }}</span><strong>{{ item.priceLabel }}</strong>
        </button>
      </div>
      <section v-if="selectedItem" class="member-order-panel portal-card">
        <p>ORDER DRAFT</p><h2>{{ selectedItem.itemName }}</h2>
        <div class="member-order-panel__price"><span>参考价格</span><strong>{{ selectedItem.priceLabel }}</strong></div>
        <label>颜色<select v-model="selection.color"><option v-for="color in selectedItem.colors" :key="color" :value="color">{{ color }}</option></select></label>
        <label>尺码<select v-model="selection.size"><option v-for="size in selectedItem.sizes" :key="size" :value="size">{{ size }}</option></select></label>
        <label>数量<input v-model.number="selection.quantity" type="number" min="1" max="10" /></label>
        <p class="member-order-panel__notice">本操作仅保存 DRAFT 草稿，不代表已付款或完成订购。</p>
        <button data-testid="save-order-draft" class="member-primary-button" type="button" :disabled="submitting || !selection.color || !selection.size" @click="saveDraft">{{ submitting ? '保存中…' : '保存订购草稿' }}</button>
        <RouterLink to="/member/services">前往“我的服务”查看草稿状态 →</RouterLink>
      </section>
    </div>
    <div v-else class="member-service-state portal-card"><strong>暂无上架塔服</strong><p>新款式发布后会在这里显示。</p></div>
  </section>
</template>

<style src="../services/services.css"></style>
