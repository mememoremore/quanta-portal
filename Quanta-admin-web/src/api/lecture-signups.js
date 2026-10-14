import { request } from '@/utils/request'

export async function getLectureRegistrations() {
  const response = await request({ url: '/qt/activity/lecture/registrations', method: 'get' })
  return {
    rows: response.rows || [],
    total: Number(response.total) || 0,
    quota: Number(response.quota) || 0,
  }
}
