const initialApplications = [
  {
    applicationId: 1001,
    userId: 2008,
    realName: '林欣欣',
    studentNo: '2025008',
    major: '工业设计',
    className: '2025级2班',
    email: 'linxx@stu.edu',
    phone: '138****1008',
    appliedAt: '2025-09-17 11:30',
    selfIntro: '专业课成绩优异，多次参加设计大赛，获得过省级奖项。',
    codingExperienceDesc: '无',
    quantaUnderstanding: '希望把设计能力投入真实产品项目。',
    applicationStatus: 'PROCESSING',
    tracks: [
      {
        choiceOrder: 1,
        department: 'DESIGN',
        rounds: {
          1: { status: 'PASS', evaluations: [{ evaluationId: 1, interviewerId: 12, interviewerName: '苏炳添', content: '设计基础扎实，表达清晰。' }] },
          2: { status: 'PENDING', advanced: true, evaluations: [] },
        },
      },
      {
        choiceOrder: 2,
        department: 'PRODUCT',
        rounds: {
          1: { status: 'PASS', evaluations: [{ evaluationId: 2, interviewerId: 11, interviewerName: '苏秉博', content: '产品意识不错，有同理心。' }] },
          2: { status: 'PENDING', advanced: false, evaluations: [] },
        },
      },
    ],
  },
  {
    applicationId: 1002,
    userId: 2007,
    realName: '赵子豪',
    studentNo: '2025007',
    major: '软件工程',
    className: '2025级1班',
    email: 'zhaozihao@stu.edu',
    phone: '139****2007',
    appliedAt: '2025-09-17 09:00',
    selfIntro: '喜欢后端开发，做过校园工具项目。',
    codingExperienceDesc: 'Java、Spring Boot、MySQL',
    quantaUnderstanding: '希望和优秀的伙伴一起做长期项目。',
    applicationStatus: 'OFFERED',
    tracks: [
      { choiceOrder: 1, department: 'BACKEND', rounds: { 1: { status: 'PASS', evaluations: [] }, 2: { status: 'PASS', advanced: true, evaluations: [] } } },
      { choiceOrder: 2, department: 'PRODUCT', rounds: { 1: { status: 'FAIL', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
    ],
  },
  {
    applicationId: 1003,
    userId: 2006,
    realName: '吴晓萌',
    studentNo: '2025006',
    major: '数字媒体',
    className: '2025级3班',
    email: 'wuxm@stu.edu',
    phone: '136****3006',
    appliedAt: '2025-09-16 14:00',
    selfIntro: '关注交互与内容体验，善于沟通。',
    codingExperienceDesc: '了解 HTML/CSS',
    quantaUnderstanding: 'Quanta 是开放且重视实践的团队。',
    applicationStatus: 'PROCESSING',
    tracks: [
      { choiceOrder: 1, department: 'DESIGN', rounds: { 1: { status: 'FAIL', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
      { choiceOrder: 2, department: 'PRODUCT', rounds: { 1: { status: 'PASS', evaluations: [] }, 2: { status: 'PENDING', advanced: true, evaluations: [] } } },
    ],
  },
  {
    applicationId: 1004,
    userId: 2005,
    realName: '刘浩然',
    studentNo: '2025005',
    major: '计算机科学',
    className: '2025级2班',
    email: 'liuhr@stu.edu',
    phone: '135****4005',
    appliedAt: '2025-09-16 10:30',
    selfIntro: '学习能力强，喜欢研究工程效率。',
    codingExperienceDesc: 'Go、Python',
    quantaUnderstanding: '希望参与实验室基础设施建设。',
    applicationStatus: 'REJECTED',
    tracks: [
      { choiceOrder: 1, department: 'BACKEND', rounds: { 1: { status: 'PASS', evaluations: [] }, 2: { status: 'FAIL', advanced: true, evaluations: [] } } },
      { choiceOrder: 2, department: 'FRONTEND', rounds: { 1: { status: 'PASS', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
    ],
  },
  {
    applicationId: 1005,
    userId: 2004,
    realName: '陈思思',
    studentNo: '2025004',
    major: '电子商务',
    className: '2025级1班',
    email: 'chenss@stu.edu',
    phone: '137****5004',
    appliedAt: '2025-09-16 09:15',
    selfIntro: '对用户研究和产品分析很感兴趣。',
    codingExperienceDesc: '无',
    quantaUnderstanding: '希望学习完整产品开发流程。',
    applicationStatus: 'SUBMITTED',
    tracks: [
      { choiceOrder: 1, department: 'PRODUCT', rounds: { 1: { status: 'PENDING', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
      { choiceOrder: 2, department: 'DESIGN', rounds: { 1: { status: 'PENDING', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
    ],
  },
  {
    applicationId: 1006,
    userId: 2003,
    realName: '张俊华',
    studentNo: '2025003',
    major: '信息管理',
    className: '2025级3班',
    email: 'zhangjh@stu.edu',
    phone: '138****6003',
    appliedAt: '2025-09-15 16:10',
    selfIntro: '擅长视觉设计与团队协作。',
    codingExperienceDesc: '无',
    quantaUnderstanding: '认可实验室分享和协作氛围。',
    applicationStatus: 'JOINED',
    tracks: [
      { choiceOrder: 1, department: 'DESIGN', rounds: { 1: { status: 'PASS', evaluations: [] }, 2: { status: 'PASS', advanced: true, evaluations: [] } } },
      { choiceOrder: 2, department: 'BACKEND', rounds: { 1: { status: 'FAIL', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
    ],
  },
  {
    applicationId: 1007,
    userId: 2002,
    realName: '王雨婷',
    studentNo: '2025002',
    major: '软件工程',
    className: '2025级2班',
    email: 'wangyt@stu.edu',
    phone: '139****7002',
    appliedAt: '2025-09-15 15:20',
    selfIntro: '做事细致，执行力强。',
    codingExperienceDesc: 'Vue 基础',
    quantaUnderstanding: '想参加真实项目锻炼能力。',
    applicationStatus: 'REJECTED',
    tracks: [
      { choiceOrder: 1, department: 'PRODUCT', rounds: { 1: { status: 'FAIL', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
      { choiceOrder: 2, department: 'DESIGN', rounds: { 1: { status: 'FAIL', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
    ],
  },
  {
    applicationId: 1008,
    userId: 2001,
    realName: '李小明',
    studentNo: '2025001',
    major: '计算机科学',
    className: '2025级1班',
    email: 'lixm@stu.edu',
    phone: '136****8001',
    appliedAt: '2025-09-15 14:30',
    selfIntro: '热爱技术，保持好奇。',
    codingExperienceDesc: 'C、Python',
    quantaUnderstanding: '希望找到长期学习伙伴。',
    applicationStatus: 'SUBMITTED',
    tracks: [
      { choiceOrder: 1, department: 'BACKEND', rounds: { 1: { status: 'PENDING', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
      { choiceOrder: 2, department: 'PRODUCT', rounds: { 1: { status: 'PENDING', evaluations: [] }, 2: { status: 'PENDING', advanced: false, evaluations: [] } } },
    ],
  },
]

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

export let mockRecruitmentApplications = clone(initialApplications)

export function resetMockRecruitment() {
  mockRecruitmentApplications = clone(initialApplications)
}

export function findMockApplication(applicationId) {
  return mockRecruitmentApplications.find(
    (application) => String(application.applicationId) === String(applicationId),
  )
}

export function recomputeAdvancement(application) {
  const first = application.tracks.find((track) => track.choiceOrder === 1)
  const second = application.tracks.find((track) => track.choiceOrder === 2)
  for (const track of application.tracks) track.rounds[2].advanced = false

  if (first?.rounds[1].status === 'PASS') first.rounds[2].advanced = true
  else if (first?.rounds[1].status === 'FAIL' && second?.rounds[1].status === 'PASS') {
    second.rounds[2].advanced = true
  }

  const advanced = application.tracks.find((track) => track.rounds[2].advanced)
  if (!advanced) {
    application.applicationStatus = application.tracks.every(
      (track) => track.rounds[1].status === 'FAIL',
    )
      ? 'REJECTED'
      : 'SUBMITTED'
    return
  }

  const secondStatus = advanced.rounds[2].status
  application.applicationStatus =
    secondStatus === 'PASS'
      ? 'OFFERED'
      : secondStatus === 'FAIL'
        ? 'REJECTED'
        : 'PROCESSING'
}
