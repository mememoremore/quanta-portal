export const mockDepartments = ['研发部', '产品部', '设计部']

export const mockCohorts = [
  { value: '21', label: '第21届', isCurrent: true },
  { value: '20', label: '第20届', isCurrent: false },
  { value: '19', label: '第19届', isCurrent: false },
  { value: '18', label: '第18届', isCurrent: false },
]

const initialMembers = [
  {
    userId: 2101,
    nickName: '陈思远',
    memberDepartment: '研发部',
    memberTitle: 'CTO',
    roleCategory: 'MGMT',
    memberCohort: '21',
    phonenumber: '138****1234',
    joinTime: '2024-09-01',
    memberStatus: 'active',
    canRetain: false,
  },
  {
    userId: 2102,
    nickName: '刘雨欣',
    memberDepartment: '产品部',
    memberTitle: '经理',
    roleCategory: 'MANAGER',
    memberCohort: '21',
    phonenumber: '139****5678',
    joinTime: '2024-09-01',
    memberStatus: 'active',
    canRetain: false,
  },
  {
    userId: 2103,
    nickName: '张伟',
    memberDepartment: '设计部',
    memberTitle: '实习生',
    roleCategory: 'INTERN',
    memberCohort: '21',
    phonenumber: '136****9012',
    joinTime: '2024-09-01',
    memberStatus: 'active',
    canRetain: false,
  },
  {
    userId: 2104,
    nickName: '王芳',
    memberDepartment: '产品部',
    memberTitle: '实习生',
    roleCategory: 'INTERN',
    memberCohort: '21',
    phonenumber: '135****3456',
    joinTime: '2024-09-01',
    memberStatus: 'active',
    canRetain: false,
  },
  {
    userId: 2105,
    nickName: '赵明',
    memberDepartment: '研发部',
    memberTitle: '经理',
    roleCategory: 'MANAGER',
    memberCohort: '21',
    phonenumber: '137****7890',
    joinTime: '2024-09-01',
    memberStatus: 'active',
    canRetain: false,
  },
  {
    userId: 2001,
    nickName: '孙丽',
    memberDepartment: '设计部',
    memberTitle: 'CDO',
    roleCategory: 'MGMT',
    memberCohort: '20',
    phonenumber: '138****2345',
    joinTime: '2023-09-01',
    memberStatus: 'resigned',
    canRetain: false,
  },
  {
    userId: 2002,
    nickName: '周浩',
    memberDepartment: '研发部',
    memberTitle: '经理',
    roleCategory: 'MANAGER',
    memberCohort: '20',
    phonenumber: '139****6789',
    joinTime: '2023-09-01',
    memberStatus: 'retention_pending',
    canRetain: true,
  },
  {
    userId: 2003,
    nickName: '吴倩',
    memberDepartment: '产品部',
    memberTitle: '实习生',
    roleCategory: 'INTERN',
    memberCohort: '20',
    phonenumber: '136****0123',
    joinTime: '2023-09-01',
    memberStatus: 'retention_pending',
    canRetain: true,
  },
  {
    userId: 1901,
    nickName: '郑凯',
    memberDepartment: '研发部',
    memberTitle: '经理',
    roleCategory: 'MANAGER',
    memberCohort: '19',
    phonenumber: '137****1122',
    joinTime: '2022-09-01',
    memberStatus: 'retained',
    canRetain: false,
  },
  {
    userId: 1801,
    nickName: '何静',
    memberDepartment: '设计部',
    memberTitle: '实习生',
    roleCategory: 'INTERN',
    memberCohort: '18',
    phonenumber: '135****7788',
    joinTime: '2021-09-01',
    memberStatus: 'resigned',
    canRetain: false,
  },
]

export let mockMembers = structuredClone(initialMembers)

export function resetMockMembers() {
  mockMembers = structuredClone(initialMembers)
}

export function findMockMember(userId, cohort) {
  return mockMembers.find(
    (member) => String(member.userId) === String(userId) && (!cohort || member.memberCohort === cohort),
  )
}
