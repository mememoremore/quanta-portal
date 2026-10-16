import { expect, test as base } from '@playwright/test'

const appOrigin = 'http://127.0.0.1:5177'
const apiOrigin = 'http://127.0.0.1:18080'

const accounts = {
  freshman_demo: {
    loginType: '0', roles: [], permissions: [],
    user: { userId: 101, userName: 'freshman_demo', nickName: '新生同学', isQuantaMember: 0 },
  },
  member_demo: {
    loginType: '1', roles: ['member'], permissions: [],
    user: { userId: 102, userName: 'member_demo', nickName: '塔员同学', isQuantaMember: 1, memberDepartment: '产品部' },
  },
  admin_demo: {
    loginType: '1', roles: ['admin'], permissions: ['*:*:*'],
    user: { userId: 103, userName: 'admin_demo', nickName: '测试管理员', isQuantaMember: 1, deptCode: 'PRODUCT' },
  },
  viewer_demo: {
    loginType: '1', roles: ['qt_manager'], permissions: ['qt:dashboard:stats', 'qt:interview:admin:list'],
    user: { userId: 104, userName: 'viewer_demo', nickName: '测试面试官', isQuantaMember: 1, deptCode: 'PRODUCT' },
  },
}

const adminRoutes = [
  { path: '/dashboard', name: 'Dashboard', component: 'dashboard/index', meta: { title: '控制台', permission: 'qt:dashboard:stats' } },
  { path: '/members', name: 'Members', component: 'members/index', meta: { title: '成员管理', permission: 'qt:member:list' } },
  { path: '/recruitment', name: 'Recruitment', component: 'recruitment/index', meta: { title: '招新管理', permission: 'qt:interview:admin:list' } },
]

const candidate = {
  applicationId: 1,
  studentNo: '20241000001',
  realName: '测试候选人',
  major: '软件工程',
  className: '软工 2401',
  firstChoice: 'PRODUCT',
  secondChoice: 'DESIGN',
  firstChoiceFirstRoundStatus: 'PENDING',
  secondChoiceFirstRoundStatus: 'PENDING',
  applyStatus: 'SUBMITTED',
  createTime: '2026-09-20 18:30:00',
}

const forumPost = {
  postId: 1,
  title: '一起优化移动端报名页',
  content: '欢迎讨论移动端交互。',
  category: 'PROJECT',
  authorId: 102,
  authorName: '塔员同学',
  authorDepartment: '产品部',
  commentCount: 2,
  viewCount: 23,
  createdAt: '2026-10-14 12:00:00',
}

function accountFromRequest(request) {
  const token = (request.headers().authorization || '').replace(/^Bearer\s+/i, '')
  return accounts[token.replace(/^e2e-token-/, '')]
}

function responseFor(request, state) {
  const url = new URL(request.url())
  const path = url.pathname
  const method = request.method()
  const account = accountFromRequest(request)

  if (method === 'GET' && path === '/captchaImage') return { code: 200, captchaEnabled: false }
  if (method === 'POST' && path === '/auth/refresh') {
    return { code: 401, msg: '登录状态已过期，请重新登录' }
  }
  if (method === 'POST' && path === '/login') {
    const body = request.postDataJSON()
    const selected = accounts[body.username]
    return selected && body.password === 'demo-password' && body.loginType === selected.loginType
      ? { code: 200, token: `e2e-token-${body.username}` }
      : { code: 500, msg: '用户名或密码错误' }
  }
  if (method === 'POST' && path === '/register/emailCode') return { code: 200, msg: '验证码已发送' }
  if (method === 'POST' && path === '/register') return { code: 200, msg: '注册成功' }
  if (method === 'GET' && path === '/getInfo') {
    return account
      ? { code: 200, user: account.user, roles: account.roles, permissions: account.permissions }
      : { code: 401, msg: '登录状态已失效' }
  }
  if (method === 'GET' && path === '/getRouters') {
    return { code: 200, data: adminRoutes.filter((route) => account?.permissions.includes('*:*:*') || account?.permissions.includes(route.meta.permission)) }
  }
  if (method === 'POST' && path === '/logout') return { code: 200 }

  if (method === 'GET' && path === '/qt/interview/my') {
    return { code: 200, data: { application: null, profile: null, newApplicationsOpen: state.recruitmentOpen, canUpdate: state.recruitmentOpen } }
  }
  if (method === 'GET' && path === '/qt/interview/myResults') return { code: 200, data: [] }
  if (method === 'POST' && path === '/qt/interview/apply') {
    return state.applyResult === 'failure'
      ? { code: 500, msg: '模拟提交失败' }
      : { code: 200, msg: '提交成功' }
  }
  if (method === 'GET' && path === '/qt/activity/list') return { code: 200, rows: [], total: 0 }
  if (method === 'GET' && path === '/system/notice/listTop') return { code: 200, data: [], unreadCount: 0 }
  if (method === 'GET' && path === '/qt/forum/posts') {
    const post = state.forumPost
    const rows = url.searchParams.get('category') && url.searchParams.get('category') !== post.category
      ? []
      : url.searchParams.get('scope') === 'mine' && account?.user.userId !== post.authorId
        ? []
        : [post]
    return { code: 200, rows, total: rows.length }
  }
  if (method === 'GET' && /^\/qt\/forum\/posts\/\d+$/.test(path)) return { code: 200, data: state.forumPost }
  if (method === 'GET' && /^\/qt\/forum\/posts\/\d+\/comments$/.test(path)) return { code: 200, rows: [], total: 0 }
  if ((method === 'POST' && path === '/qt/forum/posts') || (method === 'PUT' && /^\/qt\/forum\/posts\/\d+$/.test(path))) {
    if (state.forumPublishFailure) return { code: 500, msg: '模拟发布失败' }
    state.forumPost = { ...state.forumPost, ...request.postDataJSON(), postId: method === 'POST' ? 2 : state.forumPost.postId }
    return { code: 200, data: state.forumPost }
  }
  if (method === 'GET' && path === '/dashboard/stats') return { code: 200, data: { members: 12, resumesToday: 3, pendingPayments: 0 } }
  if (method === 'GET' && path === '/qt/member/cohorts') {
    return { code: 200, data: [21, 20, 19, 18, 17, 16].map((value) => ({ value, label: `第${value}届`, isCurrent: value === 21 })) }
  }
  if (method === 'GET' && path === '/qt/member/list') {
    const departments = [{ value: 'PRODUCT', label: '产品部' }, { value: 'BACKEND', label: '后端部' }]
    const rows = Array.from({ length: 22 }, (_, index) => ({
      userId: index + 1, nickName: `测试成员${index + 1}`, memberDepartment: index % 2 ? 'BACKEND' : 'PRODUCT',
      memberTitle: '经理', roleCategory: 'MANAGER', memberCohort: String(parseInt(url.searchParams.get('memberCohort') || '21', 10)),
      phonenumber: '138****0000', joinTime: '2026-09-23 18:30:00', memberStatus: 'active',
    })).filter((row) => (!url.searchParams.get('nickName') || row.nickName.includes(url.searchParams.get('nickName')))
      && (!url.searchParams.get('memberDepartment') || row.memberDepartment === url.searchParams.get('memberDepartment'))
      && (!url.searchParams.get('roleCategory') || row.roleCategory === url.searchParams.get('roleCategory')))
    const page = Number(url.searchParams.get('pageNum') || 1)
    const pageSize = Number(url.searchParams.get('pageSize') || 10)
    return { code: 200, rows: rows.slice((page - 1) * pageSize, page * pageSize), total: rows.length, departments }
  }
  if (method === 'GET' && path === '/qt/interview/admin/statistics') {
    return { code: 200, data: { submissions: 1, firstPassed: 0, secondPassed: 0, joined: 0 } }
  }
  if (method === 'GET' && path === '/qt/interview/admin/applications') {
    const rows = Array.from({ length: state.candidateCount }, (_, index) => index === 0
      ? candidate
      : { ...candidate, applicationId: index + 1, studentNo: `202410000${String(index + 1).padStart(2, '0')}`, realName: `测试候选人${index + 1}` })
    const page = Number(url.searchParams.get('pageNum') || 1)
    const pageSize = Number(url.searchParams.get('pageSize') || 10)
    return { code: 200, rows: rows.slice((page - 1) * pageSize, page * pageSize), total: rows.length }
  }
  return null
}

export const test = base.extend({
  api: async ({ page }, use) => {
    const state = { recruitmentOpen: false, applyResult: 'success', candidateCount: 1, forumPost: { ...forumPost }, forumPublishFailure: false, unexpected: [], requests: [] }
    await page.route('**/*', async (route) => {
      const request = route.request()
      const url = new URL(request.url())
      if (url.origin === appOrigin) return route.continue()
      if (url.origin !== apiOrigin) {
        state.unexpected.push(`${request.method()} ${request.url()}`)
        return route.abort()
      }
      const headers = {
        'access-control-allow-origin': appOrigin,
        'access-control-allow-credentials': 'true',
        'access-control-allow-methods': 'GET, POST, PUT, DELETE, OPTIONS',
        'access-control-allow-headers': 'authorization, content-type, x-trace-id',
      }
      if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
      state.requests.push(`${request.method()} ${url.pathname}`)
      const payload = responseFor(request, state)
      if (!payload) {
        state.unexpected.push(`${request.method()} ${url.pathname}${url.search}`)
        return route.fulfill({ status: 599, headers })
      }
      return route.fulfill({ status: 200, contentType: 'application/json', headers, body: JSON.stringify(payload) })
    })
    await use(state)
    expect(state.unexpected, '测试中出现未声明的 API 或外部请求').toEqual([])
  },
})

export { expect }

export async function loginAs(page, audience) {
  const identity = {
    freshman: { url: '/login/freshman', username: 'freshman_demo', button: '登录并进入新生门户' },
    member: { url: '/login/member', username: 'member_demo', button: '登录并进入塔员门户' },
    admin: { url: '/admin/login', username: 'admin_demo', button: '登录' },
    viewer: { url: '/admin/login', username: 'viewer_demo', button: '登录' },
  }[audience]
  await page.goto(identity.url)
  await page.getByPlaceholder(audience === 'admin' || audience === 'viewer' ? '用户名' : audience === 'freshman' ? '请输入学号' : '请输入学号或用户名').fill(identity.username)
  await page.getByPlaceholder(audience === 'admin' || audience === 'viewer' ? '密码' : '请输入密码', { exact: true }).fill('demo-password')
  await page.getByRole('button', { name: identity.button, exact: true }).click()
  const destination = audience === 'freshman' ? '/freshman/home' : audience === 'member' ? '/member/home' : '/admin/dashboard'
  await expect(page).toHaveURL(new URL(destination, appOrigin).href)
}
