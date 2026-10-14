<template>
  <RouterLink class="identity-back" to="/">← 返回身份选择</RouterLink>
</template>

<style scoped>
.identity-back {
  color: var(--portal-muted);
  font-size: 13px;
  font-weight: 400;
  text-decoration: none;
}

.identity-back:focus-visible {
  outline: 2px solid var(--portal-orange);
  outline-offset: 4px;
}
</style>
