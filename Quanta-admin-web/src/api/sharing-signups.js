import { request } from '@/utils/request'

export async function getSharingRegistrations() {
  const response = await request({
    url: '/qt/activity/sharing/registrations',
    method: 'get',
  })
  return {
    rows: response.rows || [],
    total: Number(response.total) || 0,
    quota: Number(response.quota) || 0,
  }
}
