import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import CommunityPost from '../post.vue'
import { getForumPost, listForumComments } from '@/api/portal/forum'

vi.mock('@/api/portal/forum', async (importOriginal) => ({
  ...await importOriginal(),
  getForumPost: vi.fn(),
  listForumComments: vi.fn(),
}))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ user: { userId: 2 }, permissions: [] }) }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { postId: '7' } }),
  useRouter: () => ({ replace: vi.fn() }),
}))

const post = { postId: 7, title: '技术分享', category: 'TECH', authorName: '小王', authorId: 2, content: '正文', createdAt: '' }
const stubs = { RouterLink: { template: '<a><slot /></a>' } }

describe('community post loading', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listForumComments.mockResolvedValue({ rows: [], total: 0 })
  })

  it('starts loading comments without waiting for the post response', async () => {
    let resolvePost
    getForumPost.mockReturnValue(new Promise((resolve) => { resolvePost = resolve }))
    const wrapper = mount(CommunityPost, { global: { stubs } })
    expect(getForumPost).toHaveBeenCalledWith('7')
    expect(listForumComments).toHaveBeenCalledWith('7', { pageNum: 1, pageSize: 100 })
    resolvePost(post)
    await flushPromises()
    expect(wrapper.text()).toContain('技术分享')
  })

  it('still shows the post if loading comments fails', async () => {
    getForumPost.mockResolvedValue(post)
    listForumComments.mockRejectedValue(new Error('评论暂不可用'))
    const wrapper = mount(CommunityPost, { global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('技术分享')
    expect(wrapper.get('[role="alert"]').text()).toContain('评论暂不可用')
  })
})
