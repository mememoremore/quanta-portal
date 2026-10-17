import PortalLayout from '@/layout/PortalLayout.vue'

export const portalRoutes = [
  {
    path: '/freshman',
    component: PortalLayout,
    meta: { portalAudience: 'freshman' },
    children: [
      { path: '', redirect: '/freshman/home' },
      {
        path: 'home',
        name: 'FreshmanHome',
        component: () => import('@/views/freshman/home/index.vue'),
        meta: { title: '新生首页', portalAudience: 'freshman' },
      },
      {
        path: 'recruitment',
        name: 'FreshmanRecruitment',
        component: () => import('@/views/freshman/recruitment/index.vue'),
        meta: { title: '加入我们', portalAudience: 'freshman' },
      },
      {
        path: 'events',
        name: 'FreshmanEvents',
        component: () => import('@/views/freshman/events/index.vue'),
        meta: { title: 'Quanta 活动', portalAudience: 'freshman' },
      },
      {
        path: 'security',
        name: 'FreshmanSecurity',
        component: () => import('@/views/portal-security/index.vue'),
        meta: { title: '账号安全', portalAudience: 'freshman' },
      },
    ],
  },
  {
    path: '/member',
    component: PortalLayout,
    meta: { portalAudience: 'member' },
    children: [
      { path: '', redirect: '/member/home' },
      {
        path: 'home',
        name: 'MemberHome',
        component: () => import('@/views/member/home/index.vue'),
        meta: { title: '塔员首页', portalAudience: 'member' },
      },
      {
        path: 'directory',
        name: 'MemberDirectory',
        component: () => import('@/views/member/directory/index.vue'),
        meta: { title: '通讯录', portalAudience: 'member' },
      },
      {
        path: 'community',
        name: 'MemberCommunity',
        component: () => import('@/views/member/community/index.vue'),
        meta: { title: '社区论坛', portalAudience: 'member' },
      },
      {
        path: 'community/new',
        name: 'MemberCommunityNew',
        component: () => import('@/views/member/community/editor.vue'),
        meta: { title: '发布帖子', portalAudience: 'member' },
      },
      {
        path: 'community/:postId/edit',
        name: 'MemberCommunityEdit',
        component: () => import('@/views/member/community/editor.vue'),
        meta: { title: '编辑帖子', portalAudience: 'member' },
      },
      {
        path: 'community/:postId',
        name: 'MemberCommunityPost',
        component: () => import('@/views/member/community/post.vue'),
        meta: { title: '社区帖子', portalAudience: 'member' },
      },
      {
        path: 'profile',
        name: 'MemberProfile',
        component: () => import('@/views/member/profile/index.vue'),
        meta: { title: '个人中心', portalAudience: 'member' },
      },
      {
        path: 'people/:userId',
        name: 'MemberPublicProfile',
        component: () => import('@/views/member/profile/member.vue'),
        meta: { title: '成员主页', portalAudience: 'member' },
      },
      {
        path: 'services',
        name: 'MemberServices',
        component: () => import('@/views/member/services/index.vue'),
        meta: { title: '我的服务', portalAudience: 'member' },
      },
      {
        path: 'library',
        name: 'MemberLibrary',
        component: () => import('@/views/member/library/index.vue'),
        meta: { title: '图书借阅', portalAudience: 'member' },
      },
      {
        path: 'clothing',
        name: 'MemberClothing',
        component: () => import('@/views/member/clothing/index.vue'),
        meta: { title: '塔服订购', portalAudience: 'member' },
      },
      {
        path: 'materials',
        name: 'MemberMaterials',
        component: () => import('@/views/member/materials/index.vue'),
        meta: { title: '学习资料', portalAudience: 'member' },
      },
      {
        path: 'recruitment',
        name: 'MemberRecruitment',
        component: () => import('@/views/member/recruitment/index.vue'),
        meta: { title: '招新报名', portalAudience: 'member' },
      },
      {
        path: 'security',
        name: 'MemberSecurity',
        component: () => import('@/views/portal-security/index.vue'),
        meta: { title: '账号安全', portalAudience: 'member' },
      },
    ],
  },
]
