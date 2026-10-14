<script setup>
defineProps({
  name: { type: String, default: 'Quanta 成员' },
})
</script>

<template>
  <section class="welcome-banner">
    <div class="welcome-banner__copy">
      <span class="welcome-banner__eyebrow">QUANTA ADMIN CONSOLE</span>
      <h2>你好，{{ name }}！</h2>
      <p>连接成员、招新与日常事务，让每一次协作都有迹可循。</p>
    </div>
    <img class="welcome-banner__mark" :src="'/quanta-logo.jpg'" alt="" />
  </section>
</template>

<style scoped>
.welcome-banner {
  position: relative;
  display: flex;
  min-height: 168px;
  padding: 36px 40px;
  color: white;
  overflow: hidden;
  background:
    radial-gradient(circle at 86% 16%, rgb(255 103 0 / 22%), transparent 26%),
    linear-gradient(115deg, #202124 0%, #27282c 100%);
  border: 1px solid rgb(255 255 255 / 6%);
  border-radius: 26px;
  box-shadow: 0 24px 60px rgb(32 33 36 / 14%);
  align-items: center;
}

.welcome-banner__copy {
  position: relative;
  z-index: 1;
}

.welcome-banner h2 {
  margin: 8px 0 10px;
  font-size: 30px;
  letter-spacing: -.04em;
}

.welcome-banner p {
  margin: 0;
  color: #b7b8bb;
  font-size: 14px;
}

.welcome-banner__eyebrow {
  color: #ff7a1a;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: .18em;
}

.welcome-banner__mark {
  position: absolute;
  right: 28px;
  width: 210px;
  height: 210px;
  opacity: .16;
  object-fit: cover;
  border-radius: 44px;
  transform: rotate(-7deg);
}
</style>
