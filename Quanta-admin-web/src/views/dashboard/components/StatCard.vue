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
    <div class="stat-card__icon" :class="`stat-card__icon--${tone}`">
      <ElIcon :size="24"><component :is="icon" /></ElIcon>
    </div>
    <div>
      <strong>{{ value ?? '--' }}</strong>
      <p>{{ label }}</p>
    </div>
  </article>
</template>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  min-height: 118px;
  padding: 24px;
  align-items: center;
  gap: 18px;
  transition: transform .2s ease, box-shadow .2s ease;
}

.stat-card::before {
  position: absolute;
  top: 0;
  left: 24px;
  width: 42px;
  height: 3px;
  content: "";
  background: var(--quanta-primary);
  border-radius: 0 0 4px 4px;
}

.stat-card:hover {
  box-shadow: 0 22px 58px rgb(32 33 36 / 10%);
  transform: translateY(-3px);
}

.stat-card__icon {
  display: grid;
  width: 56px;
  height: 56px;
  color: #5d61a8;
  background: #f0f1ff;
  border-radius: 17px;
  flex: 0 0 56px;
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
  color: #1f2438;
  font-size: 28px;
  line-height: 1.15;
}

.stat-card p {
  margin: 6px 0 0;
  color: #7e8597;
  font-size: 13px;
}
</style>
