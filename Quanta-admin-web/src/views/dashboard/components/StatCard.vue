<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: '--' },
  icon: { type: Object, required: true },
  tone: { type: String, default: 'blue' },
})
</script>

<template>
  <article class="stat-card page-card">
    <div class="stat-card__heading"><p>{{ label }}</p><div class="stat-card__icon" :class="`stat-card__icon--${tone}`"><ElIcon :size="19"><component :is="icon" /></ElIcon></div></div>
    <strong>{{ value ?? '--' }}</strong>
  </article>
</template>

<style scoped>
.stat-card {
  position: relative;
  display: grid;
  min-height: 140px;
  padding: 22px;
  gap: 16px;
}

.stat-card__heading { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: 12px; }

.stat-card__icon {
  display: grid;
  width: 34px;
  height: 34px;
  color: #ff6700;
  background: #fff5ee;
  border-radius: 10px;
  flex: 0 0 34px;
  place-items: center;
}

.stat-card__icon--green {
  color: #32a96b;
  background: #e9faef;
}

.stat-card__icon--orange {
  color: #e95d00;
  background: #fff1e7;
}

.stat-card__icon--red {
  color: #df6a70;
  background: #ffedef;
}

.stat-card strong {
  display: block;
  color: #252a32;
  font-size: 33px;
  line-height: 1.15;
}

.stat-card p {
  margin: 0;
  color: #84909f;
  font-size: 12px;
}

@media (max-width: 600px) {
  .stat-card { display: flex; min-width: 0; min-height: 86px; padding: 18px; gap: 14px; align-items: center; justify-content: space-between; }
  .stat-card__heading { flex-direction: row-reverse; justify-content: flex-end; }
  .stat-card__heading p { overflow-wrap: anywhere; }
  .stat-card strong { font-size: 28px; }
}
</style>
