<script setup>
defineProps({ card: { type: Object, required: true } })
</script>

<template>
  <article class="business-card-preview">
    <div class="business-card-preview__brand"><span>Q</span><strong>QUANTA</strong></div>
    <div class="business-card-preview__identity">
      <p>{{ card.department || 'Quanta Laboratory' }}</p>
      <h2>{{ card.name || 'Quanta 成员' }}</h2>
      <span>{{ card.title || '成员' }}</span>
    </div>
    <p class="business-card-preview__bio">{{ card.bio || '让想法被看见，让创造真正发生。' }}</p>
    <dl>
      <div v-if="card.phone"><dt>PHONE</dt><dd>{{ card.phone }}</dd></div>
      <div v-if="card.email"><dt>EMAIL</dt><dd>{{ card.email }}</dd></div>
    </dl>
  </article>
</template>

<style scoped>
.business-card-preview { position: relative; min-height: 300px; padding: 30px; overflow: hidden; border-radius: 24px; background: linear-gradient(145deg, #1f2023, #33353b); color: #fff; box-shadow: 0 24px 60px rgb(32 33 36 / 20%); }
.business-card-preview::after { position: absolute; right: -70px; bottom: -100px; width: 240px; height: 240px; border: 50px solid var(--portal-orange); border-radius: 72px; content: ''; opacity: .9; transform: rotate(18deg); }
.business-card-preview__brand { display: flex; align-items: center; gap: 10px; }
.business-card-preview__brand span { display: grid; width: 34px; height: 34px; border-radius: 10px; background: var(--portal-orange); font-weight: 900; place-items: center; }
.business-card-preview__brand strong { font-size: 13px; letter-spacing: .18em; }
.business-card-preview__identity { margin-top: 48px; }
.business-card-preview__identity p { margin: 0 0 7px; color: #ffb783; font-size: 12px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.business-card-preview__identity h2 { margin: 0; font-size: 34px; }
.business-card-preview__identity span { display: block; margin-top: 7px; color: #d5d7dc; }
.business-card-preview__bio { position: relative; z-index: 1; max-width: 72%; margin: 25px 0; color: #d5d7dc; font-size: 13px; line-height: 1.7; }
.business-card-preview dl { position: relative; z-index: 1; display: flex; margin: 0; gap: 22px; }
.business-card-preview dl div { display: grid; gap: 3px; }
.business-card-preview dt { color: #92969f; font-size: 9px; letter-spacing: .13em; }
.business-card-preview dd { margin: 0; font-size: 12px; }
@media (max-width: 520px) { .business-card-preview { min-height: 280px; padding: 24px 20px; border-radius: 20px; } .business-card-preview__identity { margin-top: 34px; } .business-card-preview__identity h2 { font-size: 28px; overflow-wrap: anywhere; } .business-card-preview__bio { max-width: 82%; } .business-card-preview dl { align-items: flex-start; flex-direction: column; gap: 10px; } .business-card-preview dd { overflow-wrap: anywhere; } .business-card-preview::after { right: -105px; bottom: -125px; } }
</style>
