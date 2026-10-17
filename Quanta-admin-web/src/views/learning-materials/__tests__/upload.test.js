import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import LearningMaterials from '../index.vue'
import { getMaterials, uploadMaterial } from '@/api/materials'

vi.mock('@/api/materials', () => ({ getMaterials: vi.fn(), uploadMaterial: vi.fn(), downloadMaterial: vi.fn(), removeMaterial: vi.fn() }))
vi.mock('@/api/material-folders', () => ({ ensureMaterialTypeFolder: vi.fn().mockResolvedValue(12), getMaterialFolders: vi.fn() }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ roles: ['qt_mgmt'], permissions: [], departmentCode: 'FRONTEND', canAccessAdmin: true }) }))
vi.mock('@/composables/usePermission', () => ({ usePermission: () => ({ hasAny: () => false, hasRole: () => false }) }))
vi.mock('element-plus', async importOriginal => ({ ...await importOriginal(), ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))

const stubs = {
  ElUpload: { name: 'ElUpload', props: ['onChange'], template: '<div><slot /></div>', methods: { clearFiles() {}, handleStart() {} } },
  ElIcon: { template: '<span><slot /></span>' },
  ElSelect: { template: '<select><slot /></select>' },
  ElOption: { template: '<option />' },
  ElInput: { template: '<input />' },
  ElButton: { template: '<button><slot /></button>' },
  ElTable: { template: '<div><slot /></div>' },
  ElTableColumn: { template: '<div />' },
  ElPagination: { template: '<div />' },
  ElDrawer: { name: 'ElDrawer', props: ['modelValue'], template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>' },
  PermissionButton: { template: '<button><slot /></button>' },
}

describe('learning materials staged upload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMaterials.mockResolvedValue({ rows: [], total: 0 })
    uploadMaterial.mockResolvedValue({})
  })

  it('shows a selected file and uploads only after confirmation', async () => {
    const wrapper = mount(LearningMaterials, { global: { stubs, directives: { loading: () => {} } } })
    await flushPromises()
    await wrapper.get('[data-testid="open-material-upload"]').trigger('click')
    await flushPromises()
    const file = new File(['notes'], '学习资料.pdf', { type: 'application/pdf' })
    wrapper.getComponent({ name: 'ElUpload' }).props('onChange')({ raw: file })
    await flushPromises()
    expect(uploadMaterial).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="material-upload-item"]').text()).toContain('学习资料.pdf')
    expect(wrapper.get('.material-upload-drawer__status').text()).toBe('待上传')
    await wrapper.get('[data-testid="confirm-material-upload"]').trigger('click')
    await flushPromises()
    expect(uploadMaterial).toHaveBeenCalledWith(file, expect.objectContaining({ category: 'FRONTEND', folderId: 12 }))
    expect(wrapper.get('.material-upload-drawer__status').text()).toBe('已上传')
    expect(wrapper.get('[data-testid="confirm-material-upload"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('keeps the selected file after an upload error for retry', async () => {
    uploadMaterial.mockRejectedValue(new Error('网络异常'))
    const wrapper = mount(LearningMaterials, { global: { stubs, directives: { loading: () => {} } } })
    await flushPromises()
    await wrapper.get('[data-testid="open-material-upload"]').trigger('click')
    await flushPromises()
    const file = new File(['notes'], '学习资料.pdf', { type: 'application/pdf' })
    wrapper.getComponent({ name: 'ElUpload' }).props('onChange')({ raw: file })
    await flushPromises()
    await wrapper.get('[data-testid="confirm-material-upload"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="material-upload-item"]').text()).toContain('学习资料.pdf')
    expect(wrapper.get('[data-testid="material-upload-item"]').text()).toContain('网络异常')
    expect(wrapper.get('.material-upload-drawer__status').text()).toBe('上传失败')
    wrapper.unmount()
  })
})
