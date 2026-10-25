import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus'
import LearningMaterials from '../index.vue'
import { batchRemoveMaterials, getMaterials, removeMaterial } from '@/api/materials'
import { useUserStore } from '@/stores/user'

vi.mock('@/api/materials', () => ({ getMaterials: vi.fn(), batchRemoveMaterials: vi.fn(), removeMaterial: vi.fn(), downloadMaterial: vi.fn(), uploadMaterial: vi.fn() }))
vi.mock('@/api/material-folders', () => ({ getMaterialFolders: vi.fn().mockResolvedValue([]), ensureMaterialTypeFolder: vi.fn() }))
vi.mock('element-plus', async original => ({ ...await original(), ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))

let wrapper
const rows = [1, 2].map(materialId => ({ materialId, category: 'FRONTEND', fileName: `学习资料${materialId}.pdf`, fileSize: 100 }))
function setup(roles = ['ceo'], permissions = ['qt:material:remove']) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useUserStore()
  store.roles = roles
  store.permissions = permissions
  store.user = { userId: 1, isQuantaMember: 1, memberDepartment: 'FRONTEND' }
  wrapper = mount(LearningMaterials, { global: { plugins: [pinia, ElementPlus] } })
  return store
}
async function selectFirst() { await wrapper.get('.learning-materials-mobile input[type=checkbox]').setValue(true) }

describe('CEO material batch deletion', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMaterials.mockResolvedValue({ rows, total: 2 })
    batchRemoveMaterials.mockResolvedValue({ deletedIds: [1], alreadyDeletedIds: [] })
    ElMessageBox.confirm.mockResolvedValue('confirm')
  })
  afterEach(() => wrapper?.unmount())

  it.each([
    { roles: ['admin'], permissions: ['*:*:*'], visible: false },
    { roles: ['qt_member'], permissions: ['qt:material:remove'], visible: false },
    { roles: ['ceo'], permissions: [], visible: false },
    { roles: ['qt_mgmt'], permissions: ['qt:material:remove'], visible: true },
    { roles: ['qt_manager'], permissions: ['qt:material:remove'], visible: true },
    { roles: ['qt_manager'], permissions: [], visible: false },
  ])('shows batch controls only for ceo, management, or manager with delete permission: $roles', async ({ roles, permissions, visible }) => {
    setup(roles, permissions)
    await flushPromises()
    expect(wrapper.find('[data-testid="material-batch-delete"]').exists()).toBe(visible)
    expect(wrapper.find('.learning-materials-mobile input[type=checkbox]').exists()).toBe(visible)
  })

  it('starts disabled, confirms selected files, and uses only the batch endpoint', async () => {
    setup()
    await flushPromises()
    expect(wrapper.get('[data-testid="material-batch-delete"]').attributes('disabled')).toBeDefined()
    await selectFirst()
    getMaterials.mockResolvedValue({ rows: [rows[1]], total: 1 })
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(expect.objectContaining({ type: 'div' }), '批量删除资料', expect.objectContaining({ confirmButtonText: '确认删除 1 份' }))
    expect(batchRemoveMaterials).toHaveBeenCalledExactlyOnceWith([1])
    expect(removeMaterial).not.toHaveBeenCalled()
    expect(ElMessage.success).toHaveBeenCalledWith('已删除 1 份资料')
    expect(wrapper.get('[data-testid="material-batch-delete"]').attributes('disabled')).toBeDefined()
  })

  it('does not send a mutation on cancellation', async () => {
    ElMessageBox.confirm.mockRejectedValue('cancel')
    setup()
    await flushPromises()
    await selectFirst()
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    await flushPromises()
    expect(batchRemoveMaterials).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('已选 1 项')
  })

  it('keeps the selection after a definite permission or missing-endpoint error', async () => {
    batchRemoveMaterials.mockRejectedValue(Object.assign(new Error('仅 CEO 可执行该操作'), { code: 403 }))
    setup()
    await flushPromises()
    await selectFirst()
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('已选 1 项')
    expect(ElMessage.error).toHaveBeenCalledWith('仅 CEO 可执行该操作')
    expect(ElMessage.success).not.toHaveBeenCalled()
  })

  it('locks filters and repeat submissions while the confirmation is open', async () => {
    let finish
    ElMessageBox.confirm.mockImplementation(() => new Promise(resolve => { finish = resolve }))
    setup()
    await flushPromises()
    await selectFirst()
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    expect(wrapper.get('[data-testid="material-batch-delete"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('.material-filters__departments button').attributes('disabled')).toBeDefined()
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(1)
    finish('confirm')
    await flushPromises()
    expect(batchRemoveMaterials).toHaveBeenCalledTimes(1)
  })

  it('rechecks privileges after confirmation and clears selection when revoked', async () => {
    let finish
    ElMessageBox.confirm.mockImplementation(() => new Promise(resolve => { finish = resolve }))
    const store = setup()
    await flushPromises()
    await selectFirst()
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    store.roles = ['admin']
    finish('confirm')
    await flushPromises()
    expect(batchRemoveMaterials).not.toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('当前账号无批量删除权限，请刷新后重试')
  })

  it('clears hidden selections on search and toggles the selected type back to all', async () => {
    setup()
    await flushPromises()
    await selectFirst()
    await wrapper.get('input[aria-label="搜索资料名称"]').setValue('学习')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('已选 0 项')
    const training = wrapper.findAll('.material-filters__types button').find(button => button.text() === '培训资料')
    await training.trigger('click')
    await flushPromises()
    expect(training.attributes('aria-pressed')).toBe('true')
    await training.trigger('click')
    await flushPromises()
    expect(wrapper.get('.material-filters__reset').attributes('aria-pressed')).toBe('true')
  })

  it('refreshes uncertain results and does not report deletion success', async () => {
    batchRemoveMaterials.mockRejectedValue(Object.assign(new Error('删除结果未确认'), { uncertain: true }))
    setup()
    await flushPromises()
    await selectFirst()
    await wrapper.get('[data-testid="material-batch-delete"]').trigger('click')
    await flushPromises()
    expect(getMaterials).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('已选 0 项')
    expect(ElMessage.success).not.toHaveBeenCalled()
  })
})
