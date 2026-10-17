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
    <div class="welcome-banner__brand" aria-hidden="true">
      <span class="welcome-banner__plate"></span>
      <img class="welcome-banner__mark" :src="'/quanta-logo-small.jpg'" alt="" width="120" height="120" />
      <span class="welcome-banner__orbit"></span>
      <span class="welcome-banner__orbit welcome-banner__orbit--secondary"></span>
    </div>
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

.welcome-banner__brand {
  position: relative;
  display: grid;
  width: 190px;
  height: 104px;
  flex: 0 0 190px;
  isolation: isolate;
  place-items: center;
  pointer-events: none;
}

.welcome-banner__brand::before {
  position: absolute;
  width: 230px;
  height: 170px;
  background: radial-gradient(ellipse, rgb(255 135 43 / 31%), rgb(255 174 104 / 10%) 53%, transparent 72%);
  content: '';
}

.welcome-banner__plate {
  position: absolute;
  width: 126px;
  height: 126px;
  border: 1px solid rgb(255 255 255 / 90%);
  border-radius: 28px;
  background: rgb(255 255 255 / 48%);
  box-shadow: inset 0 0 18px rgb(255 255 255 / 55%), 0 8px 22px rgb(226 124 45 / 9%);
  transform: translate(-9px, -2px) rotate(8deg);
}

.welcome-banner__mark {
  position: relative;
  width: 120px;
  height: 120px;
  opacity: 1;
  object-fit: cover;
  border-radius: 27px;
  box-shadow: 0 12px 22px rgb(104 65 31 / 17%), 0 2px 5px rgb(32 33 36 / 6%);
  transform: rotate(-7deg);
  transition: transform .25s ease;
}

.welcome-banner__orbit {
  position: absolute;
  width: 202px;
  height: 73px;
  border: 1px solid rgb(255 149 65 / 52%);
  border-radius: 50%;
  transform: rotate(-24deg);
}

.welcome-banner__orbit::before,
.welcome-banner__orbit::after {
  position: absolute;
  top: 50%;
  width: 9px;
  height: 9px;
  border: 2px solid #fff4e9;
  border-radius: 50%;
  background: #ff861b;
  box-shadow: 0 2px 6px rgb(255 134 27 / 20%);
  content: '';
  transform: translateY(-50%);
}

.welcome-banner__orbit::before { left: -5px; }
.welcome-banner__orbit::after { right: -5px; }
.welcome-banner__orbit--secondary { width: 191px; height: 78px; border-color: rgb(255 174 104 / 28%); transform: rotate(-37deg); }
.welcome-banner__orbit--secondary::before,
.welcome-banner__orbit--secondary::after { content: none; }

@media (hover: hover) and (pointer: fine) {
  .welcome-banner:hover .welcome-banner__mark { transform: translateY(-3px) rotate(-4deg); }
}

@media (prefers-reduced-motion: reduce) {
  .welcome-banner__mark { transition: none; }
  .welcome-banner:hover .welcome-banner__mark { transform: rotate(-7deg); }
}

@media (max-width: 600px) {
  .welcome-banner { min-height: 0; padding: 21px 18px; border-radius: 15px; gap: 12px; }
  .welcome-banner__copy { min-width: 0; }
  .welcome-banner h2 { font-size: 22px; overflow-wrap: anywhere; }
  .welcome-banner p { line-height: 1.7; overflow-wrap: anywhere; }
  .welcome-banner__brand { width: 72px; height: 72px; flex-basis: 72px; }
  .welcome-banner__brand::before { width: 110px; height: 105px; }
  .welcome-banner__plate { width: 60px; height: 60px; border-radius: 17px; transform: translate(-4px, -1px) rotate(8deg); }
  .welcome-banner__mark { width: 55px; height: 55px; border-radius: 15px; }
  .welcome-banner__orbit { width: 84px; height: 32px; }
  .welcome-banner__orbit::before,
  .welcome-banner__orbit::after { width: 6px; height: 6px; border-width: 1px; }
  .welcome-banner__orbit--secondary { display: none; }
}
</style>
