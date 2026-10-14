import { request } from '@/utils/request'

export function mapDashboardStats(data = {}) {
  return {
    members: data.members ?? data.memberCount ?? null,
    resumesToday: data.resumesToday ?? data.todayResumeCount ?? null,
    pendingPayments: data.pendingPayments ?? data.pendingPaymentCount ?? null,
  }
}

export async function getDashboardStats() {
  const response = await request({ url: '/dashboard/stats', method: 'get' })
  return { ...response, data: mapDashboardStats(response.data || {}) }
}
