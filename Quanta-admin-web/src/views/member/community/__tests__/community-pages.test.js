import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import CommunityFeed from '../index.vue'
import CommunityEditor from '../editor.vue'
import { createForumPost, listForumPosts, uploadForumImage } from '@/api/portal/forum'

vi.mock('@/api/portal/forum', async (importOriginal) => ({
  ...await importOriginal(),
  listForumPosts: vi.fn(),
  createForumPost: vi.fn(),
  uploadForumImage: vi.fn(),
}))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ user: { userId: 2 }, permissions: [] }) }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}))
vi.mock('element-plus', async (importOriginal) => ({ ...await importOriginal(), ElMessage: { success: vi.fn(), error: vi.fn() } }))

const SelectStub = defineComponent({
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
})
const stubs = {
  RouterLink: { template: '<a><slot /></a>' },
  ElSelect: SelectStub,
  ElOption: { props: ['label', 'value'], template: '<option :value="value">{{ label }}</option>' },
}

enableAutoUnmount(afterEach)

describe('member community pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    listForumPosts.mockResolvedValue({ rows: [{ postId: 7, title: '项目讨论', authorName: '小王', category: 'PROJECT', commentCount: 2 }], total: 1 })
  })

  it('shows the feed and queries my posts when the tab changes', async () => {
    const wrapper = mount(CommunityFeed, { global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('项目讨论')
    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    await flushPromises()
    expect(listForumPosts).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, scope: 'mine' })
    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    await flushPromises()
    expect(listForumPosts).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10 })
  })

  it('filters the feed by category without changing the current scope', async () => {
    const wrapper = mount(CommunityFeed, { global: { stubs } })
    await flushPromises()
    await wrapper.get('[data-category="TECH"]').trigger('click')
    await flushPromises()
    expect(listForumPosts).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, category: 'TECH' })
    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    await flushPromises()
    expect(listForumPosts).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, scope: 'mine', category: 'TECH' })
  })

  it('shows a safe uploaded image preview when a post contains Markdown images', async () => {
    listForumPosts.mockResolvedValue({
      rows: [{ postId: 7, title: '活动记录', authorName: '小王', category: 'TECH', commentCount: 0, content: '![现场](/files/photo.webp)\n![bad](javascript:alert(1))' }],
      total: 1,
    })
    const wrapper = mount(CommunityFeed, { global: { stubs } })
    await flushPromises()
    expect(wrapper.get('.community-post-card__media img').attributes('src')).toBe('/files/photo.webp')
    expect(wrapper.get('.community-post-card__media').text()).not.toContain('photo.webp')
    expect(wrapper.find('.community-post-card__image-count').exists()).toBe(false)
  })

  it('does not claim success when loading the feed fails', async () => {
    listForumPosts.mockRejectedValue(new Error('服务不可用'))
    const wrapper = mount(CommunityFeed, { global: { stubs } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('服务不可用')
    expect(wrapper.text()).not.toContain('项目讨论')
  })

  it('validates the editor before publishing a post', async () => {
    const wrapper = mount(CommunityEditor, { global: { stubs } })
    const publish = wrapper.findAll('button').find((button) => button.text().includes('发布帖子'))
    await publish.trigger('click')
    expect(wrapper.get('[role="alert"]').text()).toContain('请填写标题和正文')
    expect(createForumPost).not.toHaveBeenCalled()
    await wrapper.get('input[maxlength="80"]').setValue('分享')
    await wrapper.get('textarea[aria-label="帖子正文"]').setValue('一些内容')
    createForumPost.mockResolvedValue({ postId: 8 })
    await publish.trigger('click')
    await flushPromises()
    expect(createForumPost).toHaveBeenCalledWith({ title: '分享', category: 'PROJECT', content: '一些内容' })
  })

  it('switches edit and preview panes and does not insert a failed image', async () => {
    const wrapper = mount(CommunityEditor, { global: { stubs } })
    await wrapper.get('textarea[aria-label="帖子正文"]').setValue('## 预览标题')
    expect(wrapper.find('.community-editor__preview').html()).toContain('<h2>预览标题</h2>')
    await wrapper.findAll('.community-editor__mobile-tabs button')[1].trigger('click')
    expect(wrapper.findAll('.community-editor__pane')[0].classes()).toContain('is-mobile-hidden')
    uploadForumImage.mockRejectedValue(new Error('上传失败'))
    const file = new File(['image'], 'test.png', { type: 'image/png' })
    Object.defineProperty(wrapper.get('input[type="file"]').element, 'files', { value: [file], configurable: true })
    await wrapper.get('input[type="file"]').trigger('change')
    await flushPromises()
    expect(wrapper.get('textarea[aria-label="帖子正文"]').element.value).toBe('## 预览标题')
  })

  it('keeps the styled category selector and list toolbar functional', async () => {
    const wrapper = mount(CommunityEditor, { global: { stubs } })
    expect(wrapper.get('.community-category-select').exists()).toBe(true)
    await wrapper.get('select[aria-label="帖子分类"]').setValue('TECH')
    await wrapper.get('[data-tool="list"]').trigger('click')
    expect(wrapper.get('textarea[aria-label="帖子正文"]').element.value).toContain('- 列表项')
    await wrapper.get('input[maxlength="80"]').setValue('技术分享')
    createForumPost.mockResolvedValue({ postId: 8 })
    await wrapper.findAll('button').find((button) => button.text().includes('发布帖子')).trigger('click')
    await flushPromises()
    expect(createForumPost).toHaveBeenCalledWith({ title: '技术分享', category: 'TECH', content: '- 列表项' })
  })
})
