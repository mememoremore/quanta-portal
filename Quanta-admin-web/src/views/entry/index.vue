<script setup>
import quantaLogoUrl from '@/assets/quanta-logo.jpg'

const entries = [
  {
    code: '01',
    title: '新生入口',
    description: '查看招新信息、提交报名并跟进面试进度',
    action: '进入新生门户',
    to: '/login/freshman',
  },
  {
    code: '02',
    title: '塔员入口',
    description: '进入 Quanta 成员门户和日常服务',
    action: '进入塔员门户',
    to: '/login/member',
  },
  {
    code: '03',
    title: '管理入口',
    description: '进入后台管理系统',
    action: '进入管理后台',
    to: '/admin/login',
  },
]
</script>

<template>
  <main class="entry-page">
    <div class="entry-page__glow" aria-hidden="true"></div>
    <header class="entry-page__header portal-container">
      <div class="entry-page__brand">
        <img :src="quantaLogoUrl" alt="Quanta 社团 Logo" />
        <span><strong>Quanta</strong><small>DIGITAL CLUB</small></span>
      </div>
      <span class="entry-page__system">统一门户 · UNIFIED PORTAL</span>
    </header>
    <section class="entry-page__content portal-container">
      <div class="entry-page__hero">
        <p class="entry-page__eyebrow"><i></i> QUANTA DIGITAL CLUB</p>
        <h1><span>让每一种身份，</span><br /><em>都找到自己的<br />入口。</em></h1>
        <p class="entry-page__intro">连接招新、成员服务与社团管理，在同一个 Quanta 门户自然协作。</p>
      </div>
      <div class="entry-page__grid" aria-label="选择登录身份">
        <RouterLink v-for="entry in entries" :key="entry.to" class="entry-card" :to="entry.to">
          <span class="entry-card__code">{{ entry.code }}</span>
          <div><h2>{{ entry.title }}</h2><p>{{ entry.description }}</p></div>
          <strong>{{ entry.action }} <span aria-hidden="true">↗</span></strong>
        </RouterLink>
      </div>
    </section>
    <footer class="entry-page__footer portal-container">
      <span>© 2026 Quanta Digital Club</span><span>聚合热爱 · 共创未来</span>
    </footer>
  </main>
</template>

<style scoped>
.entry-page {
  position: relative;
  display: flex;
  height: 100vh;
  height: 100dvh;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  flex-direction: column;
  background: var(--portal-ink);
  color: #fff;
}

.entry-page::before {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgb(255 255 255 / 3%) 1px, transparent 1px), linear-gradient(90deg, rgb(255 255 255 / 3%) 1px, transparent 1px);
  background-size: 56px 56px;
  content: '';
  mask-image: linear-gradient(to bottom, black, transparent 86%);
  pointer-events: none;
}

.entry-page__glow {
  position: absolute;
  top: -260px;
  right: -180px;
  width: 720px;
  height: 720px;
  border-radius: 50%;
  background: rgb(255 103 0 / 17%);
  filter: blur(20px);
}

.entry-page__header,
.entry-page__footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.entry-page__header { padding-block: 28px; }
.entry-page__brand { display: flex; align-items: center; gap: 12px; user-select: none; }
.entry-page__brand img { width: 46px; height: 46px; border-radius: 13px; object-fit: cover; -webkit-user-drag: none; }
.entry-page__brand span { display: grid; }
.entry-page__brand strong { font-size: 22px; letter-spacing: -.02em; }
.entry-page__brand small, .entry-page__system { color: #aeb2b8; font-size: 10px; letter-spacing: .14em; }
.entry-page__system { padding: 9px 13px; border: 1px solid rgb(255 255 255 / 12%); border-radius: 999px; }

.entry-page__content {
  position: relative;
  z-index: 1;
  display: grid;
  flex: 1;
  grid-template-columns: minmax(0, .9fr) minmax(560px, 1.1fr);
  align-items: center;
  gap: clamp(48px, 8vw, 112px);
  padding-block: 48px 72px;
}

.entry-page__eyebrow {
  margin: 0 0 8px;
  color: #ff9b5d;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.15em;
}
.entry-page__eyebrow i { display: inline-block; width: 24px; height: 2px; margin-right: 8px; background: var(--portal-orange); vertical-align: middle; }

.entry-page h1 {
  margin: 0;
  max-width: 650px;
  font-size: clamp(40px, 4.6vw, 64px);
  line-height: 1.08;
  letter-spacing: -.055em;
}
.entry-page h1 em { color: var(--portal-orange); font-style: normal; }
.entry-page h1 > span { display: inline-block; white-space: nowrap; }

.entry-page__intro {
  max-width: 560px;
  margin: 24px 0 0;
  color: #b8bbc1;
  font-size: 17px;
  line-height: 1.8;
}

.entry-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.entry-card {
  display: flex;
  min-height: 220px;
  padding: 26px;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: var(--portal-radius-md);
  background: rgb(255 255 255 / 6%);
  color: #fff;
  flex-direction: column;
  justify-content: space-between;
  text-decoration: none;
  transition: transform .2s ease, border-color .2s ease, background .2s ease;
}
.entry-card:first-child { grid-row: span 2; min-height: 456px; background: var(--portal-orange); }
.entry-card:hover { border-color: rgb(255 103 0 / 65%); background: rgb(255 255 255 / 10%); transform: translateY(-3px); }
.entry-card:first-child:hover { background: var(--portal-orange-strong); }
.entry-card__code { color: rgb(255 255 255 / 58%); font-size: 12px; font-weight: 800; letter-spacing: .16em; }
.entry-card h2 { margin: 0 0 8px; font-size: 25px; }
.entry-card div p { margin: 0; color: #b8bbc1; line-height: 1.65; }
.entry-card:first-child div p { color: rgb(255 255 255 / 82%); }
.entry-card strong { display: flex; align-items: center; justify-content: space-between; font-size: 13px; }
.entry-card strong span { font-size: 20px; }

.entry-page__footer { padding-block: 22px 28px; border-top: 1px solid rgb(255 255 255 / 8%); color: #858990; font-size: 12px; }

@media (max-width: 980px) {
  .entry-page__content { grid-template-columns: 1fr; gap: 44px; }
  .entry-card:first-child { grid-row: auto; min-height: 220px; }
}

@media (max-width: 620px) {
  .entry-page__header { padding-block: 20px; }
  .entry-page__system { display: none; }
  .entry-page__content { padding-block: 36px 54px; }
  .entry-page__grid {
    grid-template-columns: 1fr;
  }
  .entry-card, .entry-card:first-child { min-height: 188px; }
  .entry-page__footer { align-items: flex-start; gap: 8px; flex-direction: column; }
}
</style>
