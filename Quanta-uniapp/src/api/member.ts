import request from '../utils/request'
import type { MemberContact } from '../utils/memberMock'
import { hasStoredBusinessCard } from '../utils/memberMock'
import type { TableResponse } from './contracts'
import { departmentLabel, resolveApiAssetUrl } from './mappers'

export interface LabMemberDto {
  userId: number
  nickName?: string
  userName?: string
  avatar?: string
  memberNo?: string
  memberDepartment?: string
  memberTitle?: string
  memberCohort?: string
  roleCategory?: string
  memberStatus?: string
}

const roleColor = (role = '') => {
  if (['CEO', 'COO', 'CTO', 'CDO'].some((value) => role.includes(value))) return '#FBBF89'
  if (role.includes('VP') || role.includes('负责人')) return '#F7A684'
  if (role.includes('实习')) return '#82E0F5'
  return '#A7F39C'
}

export const mapLabMember = (row: LabMemberDto): MemberContact => {
  const code = row.memberNo || row.userName || String(row.userId)
  const role = row.memberTitle || row.roleCategory || '成员'
  return {
    id: Number(row.userId),
    name: row.nickName || row.userName || code,
    code,
    batch: row.memberCohort || '未分届',
    role,
    roleColor: roleColor(role),
    department: departmentLabel(row.memberDepartment),
    position: row.memberTitle || '成员',
    hasBusinessCard: hasStoredBusinessCard(code),
    isActiveTalent: row.memberStatus ? row.memberStatus === 'ACTIVE' : false,
    avatar: resolveApiAssetUrl(row.avatar),
    bio: '',
  }
}

export const getMemberDirectory = async (data: Record<string, any> = {}) => {
  const response = await request<TableResponse<LabMemberDto>>({
    url: '/qt/member/list',
    data: { pageNum: 1, pageSize: 500, ...data },
  })
  return (response.rows || []).map(mapLabMember)
}
