import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import LearningMaterials from '../index.vue'
import { getMaterials, uploadMaterial } from '@/api/materials'

vi.mock('@/api/materials', () => ({ getMaterials: vi.fn(), uploadMaterial: vi.fn(), downloadMaterial: vi.fn(), removeMaterial: vi.fn() }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ roles: ['qt_mgmt'], permissions: ['qt:material:add'], departmentCode: 'FRONTEND' }) }))
vi.mock('@/composables/usePermission', () => ({ usePermission: () => ({ hasAny: () => false, hasRole: () => false }) }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))

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
    const file = new File(['notes'], '学习资料.pdf', { type: 'application/pdf' })
    wrapper.getComponent({ name: 'ElUpload' }).props('onChange')({ raw: file })
    await flushPromises()
    expect(uploadMaterial).not.toHaveBeenCalled()
    expect(wrapper.get('.learning-upload-card__selected').text()).toContain('学习资料.pdf')
    await wrapper.get('[data-testid="confirm-material-upload"]').trigger('click')
    await flushPromises()
    expect(uploadMaterial).toHaveBeenCalledWith(file, { category: 'FRONTEND' })
    expect(wrapper.find('.learning-upload-card__selected').exists()).toBe(false)
  })

  it('keeps the selected file after an upload error for retry', async () => {
    uploadMaterial.mockRejectedValue(new Error('网络异常'))
    const wrapper = mount(LearningMaterials, { global: { stubs, directives: { loading: () => {} } } })
    await flushPromises()
    const file = new File(['notes'], '学习资料.pdf', { type: 'application/pdf' })
    wrapper.getComponent({ name: 'ElUpload' }).props('onChange')({ raw: file })
    await wrapper.get('[data-testid="confirm-material-upload"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('.learning-upload-card__selected').text()).toContain('学习资料.pdf')
  })
})
