import request from '../utils/request'
import type { LocalActivitySignup, MockActivity } from '../utils/mockActivity'
import type { TableResponse } from './contracts'

export interface ActivityDto {
  activityId: number
  activityType?: 'LECTURE' | 'SHARING' | 'GENERAL'
  title: string
  description?: string
  signupStart?: string
  signupEnd?: string
  activityStart?: string
  activityEnd?: string
  locationDesc?: string
  capacity?: number
  status: MockActivity['status']
}

export interface ActivitySignupDto {
  activityId: number
  status: LocalActivitySignup['status']
  signupTime: string
  remark?: string | null
}

export type ActivityKind = 'LECTURE' | 'SHARING'

export const mapActivity = (row: ActivityDto): MockActivity => {
  const kind = row.activityType === 'SHARING' ? 'ELITE_SHARE' : 'TALK'
  return {
    activityId: Number(row.activityId),
    activityType: kind,
    title: row.title || '',
    scenePrefix: kind === 'TALK' ? '广外' : '',
    brandName: 'Quanta',
    sceneSuffix: kind === 'TALK' ? '专场宣讲会' : '精英分享会',
    description: row.description || '',
    signupStart: row.signupStart || '',
    signupEnd: row.signupEnd || '',
    activityStart: row.activityStart || '',
    activityEnd: row.activityEnd || '',
    locationDesc: row.locationDesc || '',
    capacity: Number(row.capacity || 0),
    signupCount: 0,
    status: row.status,
  }
}

export const selectActivity = (rows: ActivityDto[], kind: ActivityKind) => {
  const typed = rows.find((row) => row.activityType === kind)
  if (typed) return typed
  const keyword = kind === 'LECTURE' ? '宣讲' : '分享'
  return rows.find((row) => row.title?.includes(keyword))
}

export const mapSignup = (row: ActivitySignupDto): LocalActivitySignup => ({
  activityId: Number(row.activityId),
  status: row.status,
  signupTime: row.signupTime,
  remark: row.remark || '',
})

export const getPublishedActivityRows = async () => {
  const response = await request<TableResponse<ActivityDto>>({
    url: '/system/activity/list',
    data: { status: 'PUBLISHED', pageNum: 1, pageSize: 100 },
  })
  return response.rows || []
}

export const getPublishedActivities = async () => (await getPublishedActivityRows()).map(mapActivity)

export const getActivityByKind = async (kind: ActivityKind) => {
  const row = selectActivity(await getPublishedActivityRows(), kind)
  if (!row) throw new Error(kind === 'LECTURE' ? '暂无已发布宣讲会' : '暂无已发布精英分享会')
  return mapActivity({ ...row, activityType: kind })
}

export const getMyActivitySignup = async (activityId: number) => {
  const response = await request<TableResponse<ActivitySignupDto>>({
    url: '/system/signup/detailList',
    data: { activityId, pageNum: 1, pageSize: 20 },
  })
  const row = (response.rows || []).find((item) => Number(item.activityId) === Number(activityId))
  return row ? mapSignup(row) : null
}

export const signupActivity = async (activityId: number, remark = '') => {
  await request({
    url: '/system/signup',
    method: 'POST',
    data: { activityId, remark },
  })
  return getMyActivitySignup(activityId)
}
