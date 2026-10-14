import type { UserProfile } from '../types/session'

export interface MemberBanner {
  id: string
  title: string
  image: string
}

export interface MemberActivity {
  id: number
  title: string
  subtitle: string
}

export interface MemberContact {
  id: number
  name: string
  code: string
  batch: string
  role: string
  roleColor: string
  department: string
  position: string
  hasBusinessCard: boolean
  isActiveTalent: boolean
  avatar?: string
  bio: string
}

export interface MemberBusinessCard {
  memberId: string
  name: string
  code: string
  department: string
  batch: string
  position: string
  role: string
  bio: string
  hasBusinessCard: boolean
  isActiveTalent: boolean
}

export interface ShirtProduct {
  images: string[]
  colors: string[]
  sizes: string[]
  price: number
}

export interface ShirtSelection {
  color: string
  size: string
}

export interface MockShirtOrder extends ShirtSelection {
  id: string
  price: number
  status: 'pending-payment'
  createdAt: string
}

const biography = '热爱技术与管理，负责统筹 Quanta 日常运营工作，推动团队协作与项目落地。乐于分享经验，希望和每一位塔员一起把想法变成作品。'

const banners: MemberBanner[] = [
  { id: 'pet', title: '宠物真的可以治愈心情？', image: '/static/picture/member-banner.svg' },
]

const activities: MemberActivity[] = [
  { id: 1, title: 'Quanta 活动', subtitle: '招新进行中' },
  { id: 2, title: 'Quanta 活动', subtitle: '招新进行中' },
  { id: 3, title: 'Quanta 活动', subtitle: '招新进行中' },
]

const contacts: MemberContact[] = [
  { id: 1, name: '方东升', code: 'PM2301', batch: '20th', role: 'COO', roleColor: '#FBBF89', department: '产品部', position: '产品经理', hasBusinessCard: true, isActiveTalent: true, bio: biography },
  { id: 2, name: '官嘉辉', code: 'PO2301', batch: '20th', role: 'CEO', roleColor: '#FBBF89', department: '产品部', position: '产品经理', hasBusinessCard: true, isActiveTalent: true, bio: '负责 Quanta 团队统筹与方向规划，持续推动技术与组织成长。' },
  { id: 3, name: '陈壮志', code: 'PT2301', batch: '20th', role: 'CTO', roleColor: '#FBBF89', department: '技术部', position: '技术负责人', hasBusinessCard: false, isActiveTalent: false, bio: '' },
  { id: 4, name: '曾春梦', code: 'AP2301', batch: '20th', role: '安卓VP', roleColor: '#F7A684', department: '安卓组', position: '安卓开发', hasBusinessCard: false, isActiveTalent: false, bio: '' },
  { id: 5, name: '周俊灿', code: 'DP2301', batch: '20th', role: 'CDO', roleColor: '#FBBF89', department: '设计部', position: '视觉设计', hasBusinessCard: false, isActiveTalent: false, bio: '' },
  { id: 6, name: '陈佳曼', code: 'PO2302', batch: '20th', role: '后端VP', roleColor: '#F7A684', department: '后端组', position: '后端开发', hasBusinessCard: false, isActiveTalent: false, bio: '' },
  { id: 7, name: '刘泳淇', code: 'PM2402', batch: '20th', role: '执行层', roleColor: '#A7F39C', department: '产品部', position: '产品助理', hasBusinessCard: false, isActiveTalent: false, bio: '' },
  { id: 8, name: '林子里', code: 'PM2502', batch: '20th', role: '实习生', roleColor: '#82E0F5', department: '产品部', position: '实习生', hasBusinessCard: false, isActiveTalent: false, bio: '' },
]

const memberProfile: UserProfile = {
  id: 'PM2301',
  name: '方东升',
  account: 'PM2301',
  role: 'tower',
  department: '产品部',
  batch: '20th',
}

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))

export const getMemberHome = async () => clone({ banners, activities })
export const getMemberBanners = async () => clone(banners)
export const getMemberDirectory = async () => {
  const savedIds = new Set(readStoredCards().map((card) => card.memberId))
  return clone(contacts.map((contact) => savedIds.has(contact.code) ? { ...contact, hasBusinessCard: true } : contact))
}
export const getMemberProfile = async () => clone(memberProfile)

const BUSINESS_CARD_KEY = 'memberBusinessCards'

export const canOpenBusinessCard = (member: Pick<MemberContact, 'hasBusinessCard'>) => member.hasBusinessCard

export const normalizeBusinessCardBio = (bio: string) => bio.trim().slice(0, 800)

const contactToCard = (contact: MemberContact): MemberBusinessCard => ({
  memberId: contact.code,
  name: contact.name,
  code: contact.code,
  department: contact.department,
  batch: contact.batch,
  position: contact.position,
  role: contact.role,
  bio: contact.bio,
  hasBusinessCard: contact.hasBusinessCard,
  isActiveTalent: contact.isActiveTalent,
})

const readStoredCards = (): MemberBusinessCard[] => {
  const raw = uni.getStorageSync(BUSINESS_CARD_KEY)
  if (!raw) return []
  try { return typeof raw === 'string' ? JSON.parse(raw) : raw } catch { return [] }
}

export const hasStoredBusinessCard = (memberId: string) => readStoredCards().some((card) => card.memberId === memberId)

export const getBusinessCard = async (memberId: string): Promise<MemberBusinessCard | null> => {
  const stored = readStoredCards().find((card) => card.memberId === memberId)
  if (stored) return clone(stored)
  const contact = contacts.find((item) => item.code === memberId && item.hasBusinessCard)
  return contact ? clone(contactToCard(contact)) : null
}

export const saveBusinessCard = async (card: MemberBusinessCard): Promise<MemberBusinessCard> => {
  const normalized = { ...card, bio: normalizeBusinessCardBio(card.bio), hasBusinessCard: true }
  const cards = readStoredCards().filter((item) => item.memberId !== normalized.memberId)
  cards.push(normalized)
  uni.setStorageSync(BUSINESS_CARD_KEY, JSON.stringify(cards))
  return clone(normalized)
}

export const getShirtProduct = async (): Promise<ShirtProduct> => clone({
  images: ['/static/picture/member-shirt-photo.png'],
  colors: ['星曜黑', '电光白'],
  sizes: ['S', 'L', 'M', 'XL', '2XL', '3XL', '4XL', '5XL', '6XL'],
  price: 45,
})

export const filterContacts = (list: MemberContact[], batch: string, keyword: string) => {
  const query = keyword.trim().toLocaleLowerCase()
  return list.filter((item) => {
    const matchesBatch = item.batch === batch
    const matchesQuery = !query || `${item.name} ${item.code} ${item.role}`.toLocaleLowerCase().includes(query)
    return matchesBatch && matchesQuery
  })
}

export const validateShirtSelection = ({ color, size }: ShirtSelection) => {
  if (!color) return '请选择塔服颜色'
  if (!size) return '请选择塔服尺码'
  return null
}

export const saveMockShirtOrder = async (selection: ShirtSelection): Promise<MockShirtOrder> => {
  const order: MockShirtOrder = {
    id: `shirt-${Date.now()}`,
    ...selection,
    price: 45,
    status: 'pending-payment',
    createdAt: new Date().toISOString(),
  }
  uni.setStorageSync('mockMemberShirtOrder', JSON.stringify(order))
  return order
}
