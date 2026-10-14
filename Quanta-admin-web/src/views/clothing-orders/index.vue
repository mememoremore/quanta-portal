<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PermissionButton from '@/components/PermissionButton.vue'
import { approveClothingOrder, getClothingOrders } from '@/api/clothing-orders'
import PaymentProofDialog from './components/PaymentProofDialog.vue'
import './clothing-orders.css'

const rows = ref([])
const loading = ref(false)
const failed = ref(false)
const status = ref('')
const proof = reactive({ visible: false, order: null })
const statusMap = {
  APPROVED: { label: '已确认收款', className: 'is-approved' },
  DRAFT: { label: 'pending', className: 'is-pending' },
  SUBMITTED: { label: '待确认(已传图)', className: 'is-submitted' },
  REJECTED: { label: '已驳回', className: 'is-rejected' },
  CANCELED: { label: '已取消', className: 'is-canceled' },
}

const statusMeta = (value) => statusMap[value] || { label: value || '-', className: '' }

async function loadList() {
  loading.value = true
  failed.value = false
  try { rows.value = (await getClothingOrders(status.value ? { status: status.value } : {})).rows }
  catch (error) { failed.value = true; ElMessage.error(error.message || '塔服订单加载失败') }
  finally { loading.value = false }
}

function showProof(row) { proof.order = row; proof.visible = true }

async function confirmReceipt(row) {
  try {
    await ElMessageBox.confirm(`确认已收到 ${row.nickName} 的塔服款项吗？`, '确认收款', { type: 'warning', confirmButtonText: '确认收款', cancelButtonText: '取消' })
    await approveClothingOrder(row.orderId)
    ElMessage.success('收款确认成功')
    await loadList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '确认收款失败')
  }
}

watch(status, loadList)
onMounted(loadList)
</script>

<template>
  <section class="page-card clothing-orders-card">
    <header class="clothing-orders-card__header">
      <h2>塔服订购名单</h2>
      <ElSelect v-model="status" size="small" aria-label="订单状态" placeholder="所有状态" clearable>
        <ElOption label="所有状态" value="" />
        <ElOption v-for="(meta, value) in statusMap" :key="value" :label="meta.label" :value="value" />
      </ElSelect>
    </header>
    <div v-if="failed && !loading" class="clothing-orders-card__error"><span>订单暂时无法加载。</span><ElButton type="primary" link @click="loadList">重新加载</ElButton></div>
    <ElTable v-else v-loading="loading" class="clothing-orders-table" :data="rows" size="small" row-key="orderId" empty-text="暂无塔服订单">
      <ElTableColumn prop="nickName" label="姓名" min-width="130" />
      <ElTableColumn prop="selectedColor" label="颜色" min-width="130" />
      <ElTableColumn prop="selectedSize" label="尺码" min-width="100" />
      <ElTableColumn label="单价" min-width="110"><template #default="{ row }">¥{{ row.unitPrice }}</template></ElTableColumn>
      <ElTableColumn prop="orderTime" label="下单时间" min-width="230" />
      <ElTableColumn prop="remark" label="备注" min-width="230" />
      <ElTableColumn label="状态" min-width="210"><template #default="{ row }"><span class="clothing-orders-table__status" :class="statusMeta(row.status).className">{{ statusMeta(row.status).label }}</span></template></ElTableColumn>
      <ElTableColumn label="操作" min-width="230">
        <template #default="{ row }">
          <template v-if="row.status === 'SUBMITTED'">
            <PermissionButton size="small" type="success" permissions="qt:order:approve" :roles="['qt_mgmt', 'ceo']" @click="confirmReceipt(row)">确认收款</PermissionButton>
            <ElButton size="small" @click="showProof(row)">查看凭证</ElButton>
          </template>
          <span v-else>-</span>
        </template>
      </ElTableColumn>
    </ElTable>
    <PaymentProofDialog v-model="proof.visible" :order="proof.order" />
  </section>
</template>
