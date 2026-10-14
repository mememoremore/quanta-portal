import { request } from '@/utils/request'

function mapActivity(row = {}) {
  return {
    activityId: Number(row.activityId),
    activityType: row.activityType || 'GENERAL',
    title: row.title || '',
    description: row.description || '',
    signupStart: row.signupStart || '',
    signupEnd: row.signupEnd || '',
    activityStart: row.activityStart || '',
    activityEnd: row.activityEnd || '',
    locationDesc: row.locationDesc || '',
    capacity: Number(row.capacity || 0),
    status: row.status || '',
  }
}

function mapSignup(row = {}) {
  return {
    signupId: Number(row.signupId),
    activityId: Number(row.activityId),
    status: row.status || '',
    signupTime: row.signupTime || '',
    cancelTime: row.cancelTime || '',
    activityTitle: row.activityTitle || '',
    activityStart: row.activityStart || '',
    activityEnd: row.activityEnd || '',
    locationDesc: row.locationDesc || '',
  }
}

export async function getPublishedActivities() {
  const response = await request({
    url: '/qt/activity/list',
    method: 'get',
    params: { status: 'PUBLISHED', pageNum: 1, pageSize: 100 },
  })
  return (response.rows || []).map(mapActivity)
}

export async function getMyActivitySignup(activityId) {
  const normalizedActivityId = Number(activityId)
  const response = await request({
    url: '/qt/signup/detailList',
    method: 'get',
    params: { activityId: normalizedActivityId, pageNum: 1, pageSize: 20 },
  })
  const row = (response.rows || []).find(
    (item) => Number(item.activityId) === normalizedActivityId,
  )
  return row ? mapSignup(row) : null
}

export function signupActivity(activityId, remark = '') {
  return request({
    url: '/qt/signup',
    method: 'post',
    data: { activityId: Number(activityId), remark },
  })
}
