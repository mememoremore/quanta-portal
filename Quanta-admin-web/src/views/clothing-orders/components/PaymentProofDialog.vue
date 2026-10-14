<script setup>
defineProps({ modelValue: Boolean, order: { type: Object, default: null } })
defineEmits(['update:modelValue'])
</script>

<template>
  <ElDialog :model-value="modelValue" width="560px" class="payment-proof-dialog" @update:model-value="$emit('update:modelValue', $event)">
    <template #header><strong>付款凭证 - {{ order?.nickName }}</strong></template>
    <img v-if="order?.paymentProofUrl" :src="order.paymentProofUrl" alt="付款凭证" />
    <ElEmpty v-else description="该订单未上传付款凭证" />
  </ElDialog>
</template>
