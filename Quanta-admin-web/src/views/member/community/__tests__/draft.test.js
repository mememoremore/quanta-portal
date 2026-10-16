import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import { forumDraftKey, useForumDraft } from '../draft'

function editor(userId = 2, postId = '') {
  return mount(defineComponent({
    setup() {
      const title = ref('')
      const category = ref('PROJECT')
      const content = ref('')
      const draft = useForumDraft({ userId: () => userId, postId: () => postId, title, category, content })
      draft.initialize()
      return { title, category, content, ...draft }
    },
    template: '<div />',
  }))
}

afterEach(() => vi.restoreAllMocks())

describe('forum local drafts', () => {
  it('flushes pending changes on pagehide/unmount and restores only the matching account and post', () => {
    const wrapper = editor()
    wrapper.vm.title = '未完成的文章'
    wrapper.vm.content = '```yaml\non: push\n```'
    wrapper.vm.category = 'TECH'
    window.dispatchEvent(new Event('pagehide'))
    expect(JSON.parse(localStorage.getItem(forumDraftKey(2))).category).toBe('TECH')
    wrapper.unmount()
    const restored = editor()
    expect(restored.vm.title).toBe('未完成的文章')
    expect(restored.vm.content).toContain('yaml')
    const other = editor(3)
    const editExisting = editor(2, '9')
    expect(other.vm.title).toBe('')
    expect(editExisting.vm.title).toBe('')
    restored.unmount(); other.unmount(); editExisting.unmount()
  })

  it('does not recreate a cleared draft when the published editor unmounts', () => {
    const wrapper = editor()
    wrapper.vm.content = '已发布的正文'
    wrapper.vm.flush()
    wrapper.vm.clear()
    wrapper.unmount()
    expect(localStorage.getItem(forumDraftKey(2))).toBeNull()
  })

  it('handles invalid saved content and storage failures without breaking the editor', () => {
    localStorage.setItem(forumDraftKey(2), '{broken')
    const wrapper = editor()
    expect(wrapper.vm.content).toBe('')
    wrapper.vm.content = '保留当前输入'
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('QuotaExceeded') })
    expect(() => wrapper.vm.flush()).not.toThrow()
    expect(wrapper.vm.status).toContain('本地保存失败')
    expect(wrapper.vm.content).toBe('保留当前输入')
    wrapper.unmount()
  })
})
