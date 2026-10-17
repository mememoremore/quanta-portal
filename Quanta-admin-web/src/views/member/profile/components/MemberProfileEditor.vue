<script setup>
const props = defineProps({ modelValue: { type: Object, required: true }, disabled: Boolean, saving: Boolean, dirty: Boolean })
const emit = defineEmits(['update:modelValue', 'save', 'cancel'])
const fields = [{ key:'headline', label:'一句话介绍', max:80 }, { key:'bio', label:'个人简介', max:300, area:true }, { key:'skills', label:'技能与兴趣', max:140, hint:'最多5个标签，用逗号分隔，每个不超过20字。' }, { key:'githubUrl', label:'GitHub', max:300, type:'url', placeholder:'https://github.com/…' }, { key:'websiteUrl', label:'作品主页', max:300, type:'url', placeholder:'https://…' }, { key:'contactEmail', label:'联系邮箱（选填）', max:160, type:'email', hint:'与登录邮箱分开设置，不会修改账号绑定邮箱。' }]
function value(key) { const v = props.modelValue[key]; return Array.isArray(v) ? v.join(', ') : v }
function update(key, v) { emit('update:modelValue', { ...props.modelValue, [key]:v }) }
</script>
<template>
  <form class="member-profile-form" @submit.prevent="emit('save')">
    <fieldset :disabled="disabled || saving">
      <label v-for="field in fields" :key="field.key" :class="{ 'is-half':field.type === 'url' }">{{ field.label }}<small v-if="field.max <= 300 && !field.type && field.key !== 'skills'">{{ field.max }}字以内</small><textarea v-if="field.area" :value="value(field.key)" :maxlength="field.max" :aria-label="field.label" rows="4" @input="update(field.key,$event.target.value)" /><input v-else :value="value(field.key)" :maxlength="field.max" :aria-label="field.label" :type="field.type || 'text'" :placeholder="field.placeholder" @input="update(field.key,$event.target.value)" /><span v-if="field.hint">{{ field.hint }}</span></label>
      <label class="member-profile-form__check"><input type="checkbox" :checked="modelValue.showContactEmail" @change="update('showContactEmail',$event.target.checked)" />在成员主页展示联系邮箱</label>
    </fieldset>
    <footer><span aria-live="polite">{{ dirty ? '有未保存的修改' : '资料保存后可跨设备查看' }}</span><div><button class="member-secondary-button" type="button" :disabled="saving || !dirty" @click="emit('cancel')">取消修改</button><button class="member-primary-button" type="submit" :disabled="disabled || saving || !dirty">{{ saving ? '保存中…' : '保存修改' }}</button></div></footer>
  </form>
</template>
<style scoped>
.member-profile-form fieldset { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); min-width:0; padding:0; margin:0; border:0; gap:18px 14px; }.member-profile-form label { display:grid; min-width:0; grid-column:1/-1; gap:8px; position:relative; color:var(--portal-ink); font-size:13px; font-weight:700; }.member-profile-form label.is-half { grid-column:auto; }.member-profile-form small { position:absolute; right:0; color:var(--portal-muted); font-size:11px; font-weight:400; }
.member-profile-form input:focus-visible,.member-profile-form textarea:focus-visible { outline:3px solid rgb(255 103 0 / 12%); border-color:var(--portal-orange); }
.member-profile-form input:not([type=checkbox]),.member-profile-form textarea { box-sizing:border-box; width:100%; min-width:0; padding:12px 13px; border:1px solid var(--portal-line); border-radius:11px; background:#fff; color:var(--portal-ink); font:inherit; font-weight:400; }.member-profile-form textarea { resize:vertical; line-height:1.8; }.member-profile-form label > span { color:var(--portal-muted); font-size:11px; line-height:1.7; font-weight:400; }
.member-profile-form label.member-profile-form__check { display:flex; align-items:center; gap:8px; color:var(--portal-muted); font-size:12px; font-weight:400; }.member-profile-form__check input { accent-color:var(--portal-orange); width:16px; height:16px; flex-shrink:0; }.member-profile-form footer { display:flex; align-items:center; justify-content:space-between; gap:12px; border-top:1px solid var(--portal-line); margin-top:22px; padding-top:18px; }.member-profile-form footer > span { color:var(--portal-muted); font-size:11px; }.member-profile-form footer > div { display:flex; gap:8px; }
@media(max-width:600px) { .member-profile-form fieldset { grid-template-columns:minmax(0,1fr); }.member-profile-form input:not([type=checkbox]),.member-profile-form textarea { font-size:16px; }.member-profile-form footer { align-items:stretch; flex-direction:column; }.member-profile-form footer button { flex:1; min-height:44px; white-space:nowrap; } }
</style>
