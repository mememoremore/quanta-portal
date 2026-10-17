<script setup>
defineProps({
  name: { type: String, default: 'Quanta 成员' },
})
</script>

<template>
  <section class="welcome-banner">
    <div class="welcome-banner__copy">
      <h2>你好，{{ name }}！</h2>
      <p>连接成员、招新与日常事务，让每一次协作都有迹可循。</p>
    </div>
    <img class="welcome-banner__mark" :src="'/quanta-logo-small.jpg'" alt="" />
  </section>
</template>

<style scoped>
.welcome-banner {
  position: relative;
  display: flex;
  min-height: 156px;
  padding: 27px 30px;
  color: var(--quanta-text);
  overflow: hidden;
  background:
    linear-gradient(110deg, #fff 55%, #fff3e9);
  border: 1px solid #e9ecf1;
  border-radius: 15px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.welcome-banner__copy {
  position: relative;
  z-index: 1;
  min-width: 0;
  flex: 1;
}

.welcome-banner h2 {
  margin: 0 0 12px;
  font-size: 27px;
  letter-spacing: -.04em;
}

.welcome-banner p {
  margin: 0;
  color: #7f8998;
  font-size: 13px;
  line-height: 1.8;
}

.welcome-banner__eyebrow {
  color: #ff7a1a;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: .18em;
}

.welcome-banner__mark {
  width: 104px;
  height: 104px;
  opacity: 1;
  object-fit: cover;
  border-radius: 24px;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgb(32 33 36 / 9%);
}

@media (max-width: 600px) {
  .welcome-banner { min-height: 0; padding: 21px 18px; border-radius: 15px; gap: 12px; }
  .welcome-banner__copy { min-width: 0; }
  .welcome-banner h2 { font-size: 22px; overflow-wrap: anywhere; }
  .welcome-banner p { line-height: 1.7; overflow-wrap: anywhere; }
  .welcome-banner__mark { width: 55px; height: 55px; border-radius: 15px; }
}
</style>
