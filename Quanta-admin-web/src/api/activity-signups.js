import { request } from '@/utils/request'

function normalizeActivity(row = {}) {
  return {
    activityId: Number(row.activityId),
    title: row.title || '',
    activityType: row.activityType || 'GENERAL',
    activityStart: row.activityStart || '',
    locationDesc: row.locationDesc || '',
    capacity: Number(row.capacity || 0),
    status: row.status || '',
  }
}

function normalizeSignup(row = {}) {
  return {
    signupId: Number(row.signupId),
    activityId: Number(row.activityId),
    nickName: row.nickName || row.userName || '',
    studentNo: row.studentNo || row.userName || '',
    major: row.major || '',
    phonenumber: row.phonenumber || '',
    signupTime: row.signupTime || '',
    status: row.status || '',
  }
}

export async function getActivities(params = {}) {
  const response = await request({
    url: '/qt/activity/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 100, ...params },
  })
  return { rows: (response.rows || []).map(normalizeActivity), total: Number(response.total || 0) }
}

export async function getActivitySignups(activityId, params = {}) {
  const response = await request({
    url: '/qt/signup/detailList',
    method: 'get',
    params: { activityId: Number(activityId), ...params },
  })
  return { rows: (response.rows || []).map(normalizeSignup), total: Number(response.total || 0) }
}
