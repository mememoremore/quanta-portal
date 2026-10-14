<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMemberDirectory } from '@/api/portal/members'

const members = ref([])
const keyword = ref('')
const department = ref('')
const cohort = ref('')
const loading = ref(true)
const errorMessage = ref('')

const departments = computed(() => [...new Set(members.value.map((item) => item.department).filter(Boolean))])
const cohorts = computed(() => [...new Set(members.value.map((item) => item.cohort).filter(Boolean))])
const visibleMembers = computed(() => members.value.filter((item) => {
  const text = `${item.name} ${item.memberNo} ${item.department} ${item.title}`.toLowerCase()
  return (!keyword.value || text.includes(keyword.value.trim().toLowerCase()))
    && (!department.value || item.department === department.value)
    && (!cohort.value || item.cohort === cohort.value)
}))

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    members.value = (await getMemberDirectory()).rows
  } catch (error) {
    errorMessage.value = error.message || '通讯录加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="member-page member-directory">
    <header class="member-page__heading"><div><p>MEMBER DIRECTORY</p><h1>成员通讯录</h1><span>查找 Quanta 的伙伴、部门和届次。</span></div></header>
    <div class="member-directory__filters portal-card">
      <input v-model="keyword" type="search" placeholder="搜索姓名、编号、部门或职位" aria-label="搜索成员" />
      <select v-model="department" aria-label="筛选部门"><option value="">全部部门</option><option v-for="item in departments" :key="item">{{ item }}</option></select>
      <select v-model="cohort" aria-label="筛选届次"><option value="">全部届次</option><option v-for="item in cohorts" :key="item">{{ item }}</option></select>
    </div>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="member-loading portal-card">正在加载通讯录…</div>
    <div v-else-if="visibleMembers.length" class="member-directory__grid">
      <article v-for="member in visibleMembers" :key="member.id" class="member-directory__card portal-card">
        <div class="member-directory__avatar"><img v-if="member.avatar" :src="member.avatar" alt="" /><span v-else>{{ member.name.slice(0, 1) }}</span></div>
        <div><h2>{{ member.name }}</h2><p>{{ member.memberNo || '暂无编号' }}</p></div>
        <span class="member-directory__department">{{ member.department }}</span>
        <dl><div><dt>职位</dt><dd>{{ member.title }}</dd></div><div><dt>届次</dt><dd>{{ member.cohort }}</dd></div></dl>
      </article>
    </div>
    <div v-else class="member-empty portal-card">没有找到符合条件的成员</div>
  </section>
</template>

<style src="../member.css"></style>
<style scoped>
.member-directory__filters { display: grid; padding: 16px; grid-template-columns: minmax(260px, 1fr) 180px 160px; gap: 12px; }
.member-directory__filters input, .member-directory__filters select { box-sizing: border-box; width: 100%; padding: 11px 13px; border: 1px solid var(--portal-line); border-radius: 11px; background: #fff; color: var(--portal-ink); font: inherit; }
.member-directory__grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.member-directory__card { position: relative; display: grid; padding: 22px; grid-template-columns: auto 1fr; gap: 14px; align-items: center; }
.member-directory__avatar { display: grid; width: 52px; height: 52px; overflow: hidden; border-radius: 16px; background: var(--portal-orange-soft); color: var(--portal-orange-strong); font-size: 20px; font-weight: 850; place-items: center; }
.member-directory__avatar img { width: 100%; height: 100%; object-fit: cover; }
.member-directory__card h2, .member-directory__card p { margin: 0; }
.member-directory__card h2 { font-size: 17px; }
.member-directory__card p { margin-top: 4px; color: var(--portal-muted); font-size: 12px; }
.member-directory__department { position: absolute; top: 18px; right: 18px; padding: 5px 8px; border-radius: 999px; background: #f4f5f6; color: var(--portal-muted); font-size: 10px; font-weight: 750; }
.member-directory__card dl { display: grid; margin: 6px 0 0; padding-top: 15px; border-top: 1px solid var(--portal-line); grid-column: 1 / -1; grid-template-columns: 1fr 1fr; }
.member-directory__card dl div { display: grid; gap: 3px; }
.member-directory__card dt { color: var(--portal-muted); font-size: 10px; }
.member-directory__card dd { margin: 0; font-size: 12px; font-weight: 700; }
@media (max-width: 940px) { .member-directory__grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px) { .member-directory__filters, .member-directory__grid { grid-template-columns: 1fr; } .member-directory__filters { padding: 14px; } .member-directory__card { padding: 20px 18px; } .member-directory__department { position: static; width: fit-content; grid-column: 1 / -1; grid-row: 1; } .member-directory__card dl { grid-template-columns: 1fr; gap: 12px; } .member-directory__card dd { overflow-wrap: anywhere; } }
</style>
