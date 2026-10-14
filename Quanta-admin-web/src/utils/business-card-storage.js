const STORAGE_KEY = 'quanta_portal_business_cards_v1'

function readCards() {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  } catch {
    return {}
  }
}

function clean(value, max = 120) {
  return String(value || '').trim().slice(0, max)
}

export function loadBusinessCard(memberId) {
  if (!memberId) return null
  return readCards()[String(memberId)] || null
}

export function saveBusinessCard(card = {}) {
  const memberId = clean(card.memberId, 80)
  if (!memberId) throw new Error('缺少成员编号，无法保存名片')
  const cards = readCards()
  const saved = {
    memberId,
    name: clean(card.name),
    title: clean(card.title),
    department: clean(card.department),
    phone: clean(card.phone, 40),
    email: clean(card.email, 160),
    bio: clean(card.bio, 800),
    updatedAt: new Date().toISOString(),
  }
  cards[memberId] = saved
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cards))
  return saved
}
