<script setup>
defineProps({ slug: { type: String, default: '' } })
</script>

<template>
  <aside class="pages-help portal-card" aria-labelledby="pages-help-title" data-testid="pages-guide-entry">
    <p class="pages-help__eyebrow">从这里开始</p>
    <h2 id="pages-help-title">个人网站使用指南</h2>
    <p>从创建网站到展示文章，跟着步骤完成。</p>
    <ul><li>选模板，创建你的网站</li><li>展示个人资料、文章和留言</li><li>修改版式，接入自己的代码</li></ul>
    <RouterLink
      :to="{ name: 'MemberPagesGuide', query: slug ? { slug } : {} }"
      class="member-secondary-button"
      target="_blank"
      rel="noopener"
    >
      查看指南 <span class="pages-help__new-tab">↗ 新标签页</span>
    </RouterLink>
  </aside>
</template>
