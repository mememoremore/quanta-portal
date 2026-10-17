import { request } from '@/utils/request'
import { assetUrl, normalizeProfile, validMemberId, validateProfile } from '@/utils/member-profile'
import { mapForumPost } from './forum'

const departmentNames = {
  PRODUCT: '产品',
  DESIGN: '设计',
  FRONTEND: '前端',
  BACKEND: '后端',
}

export function departmentLabel(value = '') {
  return departmentNames[value] || value || '未分配'
}

export function mapMember(row = {}) {
  const memberFlag = String(row.isQuantaMember ?? '') === '1'
  return {
    id: Number(row.userId),
    name: row.nickName || row.userName || row.memberNo || 'Quanta 成员',
    userName: row.userName || '',
    memberNo: row.memberNo || '',
    departmentCode: row.memberDepartment || '',
    department: departmentLabel(row.memberDepartment),
    title: row.memberTitle || row.roleCategory || '成员',
    cohort: row.memberCohort || '未分届',
    phone: row.phonenumber || '',
    avatar: assetUrl(row.avatar || ''),
    headline: row.headline || '',
    isActive: row.memberStatus ? row.memberStatus === 'ACTIVE' : memberFlag,
  }
}

function mapProfile(row) {
  if (!row || !validMemberId(row.userId)) throw new Error('服务端未返回有效的成员档案')
  return { ...mapMember(row), ...normalizeProfile(row) }
}

export async function getOwnMemberProfile() {
  const response = await request({ url: '/qt/member/profile', method: 'get' })
  return mapProfile(response.data)
}

export async function saveMemberProfile(value) {
  const data = normalizeProfile(value)
  const error = validateProfile(data)
  if (error) throw new Error(error)
  await request({ url: '/qt/member/profile', method: 'put', data })
  if (Object.hasOwn(value, 'communityNickname')) {
    const saved = await getOwnMemberProfile()
    if (saved.communityNickname !== data.communityNickname) throw new Error('服务端未保存社区昵称，请联系后端确认昵称接口；修改内容已保留')
  }
  return data
}

export async function getPublicMemberProfile(id) {
  if (!validMemberId(id)) throw new Error('无效的成员编号')
  const response = await request({ url: `/qt/member/${Number(id)}/profile`, method: 'get' })
  const profile = mapProfile(response.data)
  if (profile.id !== Number(id)) throw new Error('服务端返回的成员档案编号不匹配')
  const { name, department, title, cohort, avatar } = profile
  return { id:Number(id), name, department, title, cohort, avatar, ...normalizeProfile(profile), contactEmail:profile.showContactEmail ? profile.contactEmail : '' }
}

export async function getMemberPosts(id, params = {}) {
  if (!validMemberId(id)) throw new Error('无效的成员编号')
  const response = await request({ url: `/qt/member/${Number(id)}/posts`, method: 'get', params: { pageNum: 1, pageSize: 10, ...params } })
  return { rows: (response.rows || []).map(mapForumPost).filter((post) => post.authorId === Number(id)), total: Number(response.total) || 0 }
}

export async function uploadMemberAvatar(file) {
  const data = new FormData()
  data.append('avatarfile', file)
  const response = await request({ url: '/system/user/profile/avatar', method: 'post', data })
  const url = assetUrl(response.imgUrl)
  if (!url) throw new Error('服务端未返回有效的头像图片地址')
  return url
}

export async function getMemberDirectory(params = {}) {
  const response = await request({
    url: '/qt/member/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 500, ...params },
  })
  return {
    rows: (response.rows || []).map(mapMember),
    total: Number(response.total) || 0,
  }
}

export async function getCurrentProfile() {
  const response = await request({ url: '/getInfo', method: 'get' })
  return {
    ...mapMember(response.user || {}),
    roles: response.roles || [],
    permissions: response.permissions || [],
  }
}
