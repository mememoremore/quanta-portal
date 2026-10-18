<script setup>
import { computed, inject, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, routeLocationKey } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getManagedMember, getMemberManagementOptions, managedMemberPayload, saveManagedMember } from '@/api/member-admin'

const props = defineProps({ modelValue: Boolean, mode: { type: String, default: 'create' }, member: { type: Object, default: null }, cohort: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'saved'])
const store = useUserStore()
const loading = ref(false), saving = ref(false), error = ref(''), options = ref(null), record = ref(null), form = ref({}), snapshot = ref('')
let generation = 0
const isCeo = computed(() => store.roles.includes('ceo'))
const title = computed(() => ({ create:'添加成员', identity:'编辑成员资料', access:'分配权限' }[props.mode]))
const selectedRole = computed(() => options.value?.roleProfiles.find(item => item.value === form.value.roleCategory))
const dirty = computed(() => snapshot.value && snapshot.value !== JSON.stringify(form.value))
const protectedAccount = computed(() => record.value?.protected === true || ['CEO','ADMIN'].includes(String(record.value?.memberTitle || '').toUpperCase()) || (record.value && Number(record.value.userId) === Number(store.user?.userId)) || record.value?.roles?.some(role => ['ceo','admin'].includes(role)))
const allowed = computed(() => isCeo.value && options.value && !protectedAccount.value && (props.mode === 'create' ? options.value.canCreate === true : props.mode === 'access' ? record.value?.canAssignRoles === true : record.value?.canEdit === true))

async function load() {
  const current = ++generation
  error.value = ''; options.value = null; record.value = null; loading.value = true
  try {
    if (!isCeo.value) throw new Error('仅 CEO 可维护成员身份及权限')
    const [settings, detail] = await Promise.all([getMemberManagementOptions(), props.mode === 'create' ? null : getManagedMember(props.member?.id, props.cohort)])
    if (current !== generation) return
    options.value = settings; record.value = detail
    const base = settings.roleProfiles.find(item => item.value === 'INTERN') || settings.roleProfiles.find(item => item.value === 'MEMBER')
    form.value = detail ? { nickName:detail.nickName, userName:detail.userName, memberDepartment:detail.memberDepartment, memberTitle:detail.memberTitle, memberCohort:String(detail.memberCohort), roleCategory:detail.roleCategory, version:detail.version } : { nickName:'',userName:'',email:'',memberDepartment:'',roleCategory:base?.value || '',memberTitle:base?.titles[0] || '',memberCohort:props.cohort || String(settings.cohorts[0].value) }
    snapshot.value = JSON.stringify(form.value)
  } catch (failure) {
    if (current === generation) error.value = Number(failure.code) === 404 ? '成员维护接口尚未接通，请后端按对接说明实现后再使用。' : failure.message || '成员资料加载失败'
  } finally { if (current === generation) loading.value = false }
}
watch(() => [props.modelValue, props.mode, props.member?.id, props.cohort], () => { if (props.modelValue) load(); else { generation += 1; snapshot.value = '' } }, { immediate:true })
function changeRole(value) { form.value.roleCategory = value; form.value.memberTitle = selectedRole.value?.titles[0] || '' }
async function confirmLeave() {
  if (saving.value) return false
  if (!dirty.value) return true
  try { await ElMessageBox.confirm('有尚未保存的成员修改，是否放弃？', '放弃修改', { confirmButtonText:'放弃',cancelButtonText:'继续编辑',type:'warning' }); return true } catch { return false }
}
async function close() { if (await confirmLeave()) emit('update:modelValue', false) }
async function save() {
  if (!allowed.value || saving.value || loading.value) return
  error.value = ''
  saving.value = true
  try {
    const payload = managedMemberPayload(form.value, options.value, props.mode)
    const department = options.value.departments.find(item => item.value === payload.memberDepartment)?.label
    await ElMessageBox.confirm(`${form.value.nickName || props.member?.name}：${department} · ${form.value.memberTitle}。${props.mode === 'access' && ['INTERN','MEMBER'].includes(payload.roleCategory) ? '将不能登录后台。' : ''}确认保存本次变更？`, '确认成员变更', { confirmButtonText:'确认保存',cancelButtonText:'取消',type:'warning' })
    await saveManagedMember(props.member?.id, form.value, options.value, props.mode)
    snapshot.value = JSON.stringify(form.value)
    ElMessage.success('成员信息已保存')
    emit('saved'); emit('update:modelValue', false)
  } catch (failure) { if (failure !== 'cancel' && failure !== 'close') error.value = failure.message || '保存失败，修改内容已保留' }
  finally { saving.value = false }
}
if (inject(routeLocationKey, null)) onBeforeRouteLeave(confirmLeave)
function beforeUnload(event) { if (dirty.value || saving.value) { event.preventDefault(); event.returnValue = '' } }
watch([dirty, saving], ([changed, busy]) => { if (changed || busy) window.addEventListener('beforeunload', beforeUnload); else window.removeEventListener('beforeunload', beforeUnload) })
onBeforeUnmount(() => { generation += 1; window.removeEventListener('beforeunload', beforeUnload) })
</script>

<template>
  <ElDrawer class="member-editor-drawer" :model-value="modelValue" :title="title" size="min(450px, 100vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @update:model-value="close">
    <p v-if="loading" class="member-editor__note">正在读取成员资料和可用角色…</p>
    <div v-if="error" class="member-editor__error" role="alert">{{ error }} <ElButton v-if="!options" link @click="load">重新加载</ElButton></div>
    <template v-if="options && !loading">
      <p class="member-editor__note">真实姓名与社区昵称分开维护。本面板不分配 CEO 或系统管理员。</p>
      <p v-if="!allowed" class="member-editor__error" role="alert">此账号受保护，或服务端未授予本次维护权限。</p>
      <form class="member-editor__form" @submit.prevent="save">
        <label>真实姓名<ElInput v-model="form.nickName" aria-label="真实姓名" maxlength="30" :disabled="!allowed || saving || mode === 'access'" /></label>
        <label>登录账号<ElInput v-model="form.userName" aria-label="成员登录账号" maxlength="20" :disabled="!allowed || saving || mode !== 'create'" /></label>
        <label v-if="mode === 'create'">账号邮箱（选填）<ElInput v-model="form.email" aria-label="成员账号邮箱" maxlength="64" :disabled="!allowed || saving" /></label>
        <label>所属部门<ElSelect v-model="form.memberDepartment" aria-label="维护所属部门" :disabled="!allowed || saving"><ElOption v-for="item in options.departments" :key="item.value" :label="item.label" :value="item.value" /></ElSelect></label>
        <label>身份层级<ElSelect :model-value="form.roleCategory" aria-label="成员身份层级" :disabled="!allowed || saving || mode !== 'access'" @update:model-value="changeRole"><ElOption v-for="item in options.roleProfiles" :key="item.value" :label="item.label" :value="item.value" /></ElSelect></label>
        <label>具体职位<ElSelect v-model="form.memberTitle" aria-label="成员具体职位" :disabled="!allowed || saving || mode !== 'access'"><ElOption v-for="item in selectedRole?.titles || []" :key="item" :label="item" :value="item" /></ElSelect></label>
        <label>届数<ElSelect v-model="form.memberCohort" aria-label="维护成员届数" placeholder="请选择届数" :disabled="!allowed || saving || mode === 'access'"><ElOption v-for="item in options.cohorts" :key="item.value" :label="item.label" :value="String(item.value)" /></ElSelect></label>
        <div class="member-editor__preview"><strong>权限预览</strong><p>{{ ['INTERN','MEMBER'].includes(form.roleCategory) ? '可使用塔员门户，不可登录后台。' : '可登录后台；部门不等于全社团数据范围。' }}</p><ul><li v-for="permission in selectedRole?.permissions || []" :key="permission">{{ permission }}</li></ul><small>具体菜单及数据范围仍由后端按现有角色校验。</small></div>
        <p v-if="mode === 'create'" class="member-editor__note">新增默认普通塔员。初始凭据由后端按现有账号策略处理，前端不设置固定密码。</p>
      </form>
    </template>
    <template #footer><ElButton :disabled="saving" @click="close">取消</ElButton><ElButton type="primary" :disabled="!allowed || loading" :loading="saving" @click="save">{{ mode === 'create' ? '创建成员' : '保存修改' }}</ElButton></template>
  </ElDrawer>
</template>

<style>
.member-editor-drawer.el-drawer { --el-color-primary:#ff6700; --el-drawer-padding-primary:24px; max-width:100vw; }
.member-editor-drawer .el-drawer__header { padding-bottom:20px; margin-bottom:0; border-bottom:1px solid #e9ecf1; color:#282e37; font-weight:650; }
.member-editor-drawer .el-drawer__body { overflow-y:auto; }
.member-editor__form { display:grid; gap:18px; }
.member-editor__form > label { display:grid; gap:9px; color:#343c49; font-size:13px; font-weight:600; }
.member-editor__form .el-select { width:100%; }
.member-editor__note { color:#778292; font-size:12px; line-height:1.8; margin:0 0 18px; }
.member-editor__error { padding:12px; margin-bottom:17px; border-radius:8px; background:#fff0ee; color:#b34130; font-size:12px; line-height:1.8; overflow-wrap:anywhere; }
.member-editor__preview { padding:16px; background:#f6f7f9; border-radius:10px; color:#778292; font-size:12px; line-height:1.8; }
.member-editor__preview strong { color:#343c49; }.member-editor__preview p { margin:9px 0; }.member-editor__preview ul { padding-left:18px; }.member-editor__preview small { font-size:11px; }
.member-editor-drawer .el-drawer__footer { border-top:1px solid #e9ecf1; }.member-editor-drawer .el-button--primary { --el-button-bg-color:#ff6700; --el-button-border-color:#ff6700; --el-button-text-color:#fff; color:#fff; }
@media(max-width:600px) { .member-editor-drawer input { font-size:16px; }.member-editor-drawer .el-button { min-height:44px; } }
</style>
