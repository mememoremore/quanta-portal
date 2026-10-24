import { expect, test as base } from '@playwright/test'

const appOrigin = 'http://127.0.0.1:5177'
const apiOrigin = 'http://127.0.0.1:18080'

const accounts = {
  ceo_demo: {
    loginType: '1', roles: ['ceo'], permissions: ['*:*:*'],
    user: { userId:105, userName:'ceo_demo', nickName:'测试CEO', isQuantaMember:1, deptCode:'PRODUCT' },
  },
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
  { path: '/learning-materials', name: 'LearningMaterials', component: 'learning-materials/index', meta: { title: '学习资料', icon: 'Notebook', permission: 'qt:material:list' } },
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

function profileFor(id, state, own = false) {
  const account = Object.values(accounts).find(a=>a.user.userId === Number(id))
  if (!account && (Number(id) < 1 || Number(id) > 22)) return null
  const user = account?.user || {userId:Number(id),nickName:`测试成员${id}`,memberDepartment:'FRONTEND',memberTitle:'实习生',memberCohort:'第21届'}
  const data = {headline:'喜欢把想法做成有温度的界面。',bio:'正在学习 Vue 和 TypeScript，关注交互体验与前端工程化。',skills:['Vue','TypeScript','交互设计'],githubUrl:'https://github.com/example',websiteUrl:'',contactEmail:'',showContactEmail:false,...state.profiles[id]}
  return {userId:user.userId,nickName:user.nickName,memberDepartment:user.memberDepartment,memberTitle:user.memberTitle || '实习生',memberCohort:user.memberCohort || '第21届',...(own?{memberNo:'QT02102'}:{}),...data,avatar:state.avatars[id] || '',contactEmail:own || data.showContactEmail ? data.contactEmail : ''}
}

function responseFor(request, state) {
  const url = new URL(request.url())
  const path = url.pathname
  const method = request.method()
  const account = accountFromRequest(request)
  const forumSnapshot = post => {
    const entries = (state.forumReactions || []).filter(item => item.postId === post.postId)
    return { ...post, likeCount: entries.filter(item => item.kind === 'liked').length,
      liked: entries.some(item => item.kind === 'liked' && item.userId === account?.user.userId),
      favorited: entries.some(item => item.kind === 'favorites' && item.userId === account?.user.userId) }
  }

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
      ? { code: 200, user: {...account.user,avatar:state.avatars[account.user.userId] || '', ...state.userInfoOverrides}, roles: account.roles, permissions: account.permissions }
      : { code: 401, msg: '登录状态已失效' }
  }
  if (method === 'GET' && path === '/getRouters') {
    return { code: 200, data: adminRoutes.filter((route) => account?.permissions.includes('*:*:*') || account?.permissions.includes(route.meta.permission)) }
  }
  if (method === 'POST' && path === '/logout') return { code: 200 }
  if (path === '/qt/member/profile' && method === 'GET') return state.profileUnavailable ? {code:404,msg:'接口未提供'} : {code:200,data:profileFor(account?.user.userId,state,true)}
  if (path === '/qt/member/profile' && method === 'PUT') {
    if(state.profileSaveFailure)return {code:500,msg:'模拟档案保存失败'}
    state.profiles[account.user.userId]=request.postDataJSON()
    return {code:200}
  }
  if (method === 'POST' && path === '/system/user/profile/avatar') {
    if(state.avatarFailure)return {code:500,msg:'模拟头像上传失败'}
    const raw=request.postDataBuffer(),start=raw.indexOf(Buffer.from('\r\n\r\n'))+4,end=raw.lastIndexOf(Buffer.from('\r\n--'))
    state.avatars[account.user.userId]=`data:image/jpeg;base64,${raw.subarray(start,end).toString('base64')}`
    return {code:200,imgUrl:state.avatars[account.user.userId]}
  }
  if(method === 'GET' && /^\/qt\/member\/\d+\/profile$/.test(path)) {
    const data=profileFor(path.split('/')[3],state)
    return data ? {code:200,data} : {code:404,msg:'成员不存在'}
  }
  if(method === 'GET' && /^\/qt\/member\/\d+\/posts$/.test(path)) {
    const rows=Number(path.split('/')[3]) === state.forumPost.authorId ? [state.forumPost] : []
    return {code:200,rows:rows.map(forumSnapshot),total:rows.length}
  }

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
  if (method === 'GET' && ['/qt/forum/me/likes', '/qt/forum/me/favorites'].includes(path)) {
    if (!account) return { code: 401, msg: '请先登录' }
    const kind = path.endsWith('/likes') ? 'liked' : 'favorites'
    const post = state.forumPost
    const rows = (state.forumReactions || []).some(item => item.postId === post.postId && item.userId === account.user.userId && item.kind === kind)
      && (!url.searchParams.get('category') || url.searchParams.get('category') === post.category) ? [forumSnapshot(post)] : []
    const page = Number(url.searchParams.get('pageNum') || 1), size = Number(url.searchParams.get('pageSize') || 10)
    return { code: 200, rows: rows.slice((page - 1) * size, page * size), total: rows.length }
  }
  if (/^\/qt\/forum\/posts\/\d+\/(like|favorite|reactions)$/.test(path)) {
    if (!account) return { code: 401, msg: '请先登录' }
    if (Number(path.split('/')[4]) !== state.forumPost.postId) return { code: 404, msg: '帖子不存在或已删除' }
    if (['PUT', 'DELETE'].includes(method)) {
      if (state.forumReactionFailure) return { code: 403, msg: '模拟互动失败' }
      state.forumReactions ||= []
      const kind = path.endsWith('/like') ? 'liked' : 'favorites'
      const index = state.forumReactions.findIndex(item => item.postId === state.forumPost.postId && item.userId === account.user.userId && item.kind === kind)
      if (method === 'PUT' && index === -1) state.forumReactions.unshift({ postId: state.forumPost.postId, userId: account.user.userId, kind })
      if (method === 'DELETE' && index !== -1) state.forumReactions.splice(index, 1)
    }
    return { code: 200, data: forumSnapshot(state.forumPost) }
  }
  if (method === 'GET' && path === '/qt/forum/posts') {
    const post = state.forumPost
    const rows = url.searchParams.get('category') && url.searchParams.get('category') !== post.category
      ? []
      : url.searchParams.get('scope') === 'mine' && account?.user.userId !== post.authorId
        ? []
        : [post]
    return { code: 200, rows:rows.map(p=>({...forumSnapshot(p),authorAvatar:state.avatars[p.authorId] || '',authorCommunityNickname:state.profiles[p.authorId]?.communityNickname || ''})), total: rows.length }
  }
  if (method === 'GET' && /^\/qt\/forum\/posts\/\d+$/.test(path)) return { code: 200, data: {...forumSnapshot(state.forumPost),authorAvatar:state.avatars[state.forumPost.authorId] || '',authorCommunityNickname:state.profiles[state.forumPost.authorId]?.communityNickname || ''} }
  if (method === 'GET' && /^\/qt\/forum\/posts\/\d+\/comments$/.test(path)) return { code: 200, rows:state.forumComments, total:state.forumComments.length }
  if ((method === 'POST' && path === '/qt/forum/posts') || (method === 'PUT' && /^\/qt\/forum\/posts\/\d+$/.test(path))) {
    if (state.forumPublishFailure) return { code: 500, msg: '模拟发布失败' }
    state.forumPost = { ...state.forumPost, ...request.postDataJSON(), postId: method === 'POST' ? 2 : state.forumPost.postId }
    return { code: 200, data: state.forumPost }
  }
  if (path === '/qt/materials/folders' && method === 'GET') {
    if (state.materialFoldersUnavailable) return { code: 503, msg: '资料分类接口暂不可用' }
    const category = url.searchParams.get('category')
    return { code: 200, data: state.materialFolders.filter(folder => folder.category === category && folder.parentId === 0) }
  }
  if (path === '/qt/materials/folders' && method === 'POST') {
    const body = request.postDataJSON()
    if (state.materialFolders.some(folder => folder.category === body.category && folder.folderName === body.folderName)) return { code: 409, msg: '同级重名' }
    state.materialFolders.push({ ...body, folderId: Math.max(0, ...state.materialFolders.map(folder => folder.folderId)) + 1, depth: 1 })
    return { code: 200 }
  }
  if (path === '/qt/materials' && method === 'GET') {
    const category = url.searchParams.get('category'), keyword = url.searchParams.get('fileName'), folderId = url.searchParams.get('folderId')
    const rows = state.materials.filter(row => (!category || row.category === category) && (keyword ? row.fileName.includes(keyword) : folderId === null || row.folderId === Number(folderId)))
    const page = Number(url.searchParams.get('pageNum') || 1), size = Number(url.searchParams.get('pageSize') || 20)
    return { code: 200, rows: rows.slice((page - 1) * size, page * size).map(row => ({ ...row, folderName: state.materialFolders.find(folder => folder.folderId === row.folderId)?.folderName || '' })), total: rows.length }
  }
  if (method === 'GET' && /^\/qt\/materials\/\d+$/.test(path)) {
    const material = state.materials.find(row => row.materialId === Number(path.split('/').pop()))
    return material ? { code: 200, data: material } : { code: 404, msg: '资料不存在或已删除' }
  }
  if (path === '/qt/materials' && method === 'POST') {
    const raw = request.postDataBuffer().toString('utf8')
    const field = name => raw.match(new RegExp('name="' + name + '"\\r\\n\\r\\n([^\\r\\n]*)'))?.[1]
    const fileName = raw.match(/filename="([^"]+)"/)?.[1] || '上传资料.pdf'
    if (state.materialFailureName === fileName) return { code: 500, msg: '模拟文件上传失败' }
    const category = field('category'), folderId = Number(field('folderId'))
    if (!state.materialFolders.some(folder => folder.folderId === folderId && folder.category === category)) return { code: 400, msg: '目标分类不正确' }
    state.materials.push({ materialId: Math.max(0, ...state.materials.map(row => row.materialId)) + 1, fileName, category, folderId, fileSize: 128, uploaderName: '测试管理员', uploadTime: '2026-10-17 17:00:00', visibility: 'MEMBER' })
    return { code: 200 }
  }
  if (path === '/qt/materials/batch-delete' && method === 'POST') {
    const body = request.postDataJSON()
    state.materialBatchRequests ||= []
    state.materialBatchRequests.push(body)
    if (!account?.roles.includes('ceo') || !account.permissions.some(permission => ['*:*:*', 'qt:material:remove'].includes(permission))) return { code: 403, msg: '仅 CEO 可执行批量删除' }
    if (state.materialBatchMode === 'missing') return { code: 404, msg: '接口未提供' }
    if (state.materialBatchMode === 'forbidden') return { code: 403, msg: '仅 CEO 可执行批量删除' }
    if (state.materialBatchMode === 'conflict') return { code: 409, msg: '资料存在业务引用，整个批次未删除' }
    const ids = body.materialIds
    if (!Array.isArray(ids) || !ids.length || ids.length > 20 || new Set(ids).size !== ids.length || !ids.every(id => Number.isSafeInteger(id) && id > 0)) return { code: 400, msg: '资料ID无效' }
    const deletedIds = ids.filter(id => state.materials.some(row => row.materialId === id))
    const alreadyDeletedIds = ids.filter(id => !deletedIds.includes(id))
    state.materials = state.materials.filter(row => !ids.includes(row.materialId))
    return state.materialBatchMode === 'unconfirmed'
      ? { code: 200 }
      : { code: 200, data: { deletedIds, alreadyDeletedIds } }
  }
  if (method === 'GET' && path === '/dashboard/stats') return { code: 200, data: { members: 12, resumesToday: 3, pendingPayments: 0 } }
  if (path.startsWith('/qt/member/manage')) {
    if (!account?.roles.includes('ceo')) return {code:403,msg:'仅CEO可维护成员'}
    if (!state.memberManagementAvailable) return {code:404,msg:'成员维护接口未提供'}
    if (method === 'GET' && path.endsWith('/options')) return {code:200,data:{contractVersion:'member-admin-v1',canCreate:true,departments:['FRONTEND','BACKEND','PRODUCT','DESIGN'].map((value,i)=>({value,label:['前端','后端','产品','设计'][i]})),cohorts:[{value:'21',label:'第21届'}],roleProfiles:[{value:'MGMT',label:'管理层',titles:['VP','负责人'],permissions:['按现有管理层模板及部门校验']},{value:'MANAGER',label:'经理层',titles:['经理'],permissions:['本部门管理']},{value:'INTERN',label:'普通塔员（实习生）',titles:['实习生'],permissions:['塔员门户']}]}}
    const id=Number(path.split('/').pop())
    if (method === 'GET' && id) return {code:200,data:{userId:id,nickName:`测试成员${id}`,userName:`2024100${id}`,memberDepartment:'PRODUCT',memberTitle:'经理',memberCohort:'21',roleCategory:'MANAGER',version:'1',canEdit:true,canAssignRoles:true,...state.memberEdits?.[id]}}
    if (method === 'POST' || method === 'PUT') {
      const data=request.postDataJSON();state.memberMutations ||= [];state.memberEdits ||= {}
      if(state.memberManagementSaveFailure)return {code:409,msg:'版本冲突，请重新加载'}
      const userId=id || 23
      state.memberMutations.push({userId,...data})
      state.memberEdits[userId]={...state.memberEdits[userId],...data,version:String(Number(data.version||0)+1)}
      return {code:200,data:{userId,version:state.memberEdits[userId].version}}
    }
  }
  if (method === 'GET' && path === '/qt/member/cohorts') {
    return { code: 200, data: [21, 20, 19, 18, 17, 16].map((value) => ({ value, label: `第${value}届`, isCurrent: value === 21 })) }
  }
  if (method === 'GET' && path === '/qt/member/list') {
    const departments = [{ value: 'PRODUCT', label: '产品部' }, { value: 'BACKEND', label: '后端部' }]
    const rows = Array.from({ length: 22 }, (_, index) => ({
      userId: index + 1, nickName: `测试成员${index + 1}`, memberDepartment: index % 2 ? 'BACKEND' : 'PRODUCT',
      memberTitle: '经理', roleCategory: 'MANAGER', memberCohort: String(parseInt(url.searchParams.get('memberCohort') || '21', 10)),
      phonenumber: '138****0000', joinTime: '2026-09-23 18:30:00', memberStatus: 'active', ...state.memberEdits?.[index + 1],
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
  api: async ({ context }, use) => {
    const materialFolders = ['FRONTEND', 'BACKEND', 'PRODUCT', 'DESIGN'].flatMap((category, i) => ['培训资料', '技术分享', '项目实践'].map((folderName, j) => ({ category, parentId: 0, depth: 1, folderId: i * 3 + j + 1, folderName })))
    const materials = [
      { materialId: 1, category: 'FRONTEND', folderId: 1, fileName: 'Vue 入门培训.pdf' },
      { materialId: 2, category: 'FRONTEND', folderId: 2, fileName: '浏览器技术分享.pdf' },
      { materialId: 3, category: 'FRONTEND', folderId: 3, fileName: '门户项目实践.pdf' },
      { materialId: 4, category: 'FRONTEND', folderId: 0, fileName: '历史未分类资料.pdf' },
      { materialId: 5, category: 'BACKEND', folderId: 4, fileName: 'Java 培训资料.pdf' },
    ].map(row => ({ ...row, fileSize: 1024, uploaderName: '测试上传者', uploadTime: '2026-10-17', visibility: 'MEMBER' }))
    const state = { materialFolders, materials, materialFailureName: '', materialFoldersUnavailable: false, recruitmentOpen: false, applyResult: 'success', candidateCount: 1, forumPost: { ...forumPost }, forumComments:[], forumPublishFailure: false, profiles:{}, avatars:{}, profileUnavailable:false, profileSaveFailure:false, avatarFailure:false, unexpected: [], requests: [] }
    await context.route('**/*', async (route) => {
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
    ceo: { url:'/admin/login',username:'ceo_demo',button:'登录' },
  }[audience]
  await page.goto(identity.url)
  await page.getByPlaceholder(['admin','viewer','ceo'].includes(audience) ? '用户名' : audience === 'freshman' ? '请输入学号' : '请输入学号或用户名').fill(identity.username)
  await page.getByPlaceholder(['admin','viewer','ceo'].includes(audience) ? '密码' : '请输入密码', { exact: true }).fill('demo-password')
  await page.getByRole('button', { name: identity.button, exact: true }).click()
  const destination = audience === 'freshman' ? '/freshman/home' : audience === 'member' ? '/member/home' : '/admin/dashboard'
  await expect(page).toHaveURL(new URL(destination, appOrigin).href)
}
