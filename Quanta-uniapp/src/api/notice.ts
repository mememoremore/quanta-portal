import request from '../utils/request'
import type { AjaxResponse } from './contracts'

export interface NoticeDto {
  noticeId: number
  noticeTitle: string
  noticeContent?: string
  noticeType?: string
  createTime?: string
  isRead?: boolean
}

interface NoticeResponse extends AjaxResponse<NoticeDto[]> { unreadCount?: number }

export const getTopNotices = async () => {
  const response = await request<NoticeResponse>({ url: '/system/notice/listTop' })
  return { notices: response.data || [], unreadCount: Number(response.unreadCount || 0) }
}

export const markNoticeRead = (noticeId: number) => request({
  url: '/system/notice/markRead',
  method: 'POST',
  data: { noticeId },
  header: { 'Content-Type': 'application/x-www-form-urlencoded' },
})

const plainText = (value = '') => value.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()

export const openNoticePicker = async () => {
  const snapshot = await getTopNotices()
  if (!snapshot.notices.length) {
    uni.showToast({ title: '暂无通知', icon: 'none' })
    return snapshot
  }
  uni.showActionSheet({
    itemList: snapshot.notices.map((notice) => notice.noticeTitle || '未命名通知'),
    success: async ({ tapIndex }) => {
      const notice = snapshot.notices[tapIndex]
      if (!notice) return
      uni.showModal({ title: notice.noticeTitle, content: plainText(notice.noticeContent), showCancel: false })
      if (!notice.isRead) await markNoticeRead(notice.noticeId)
    },
  })
  return snapshot
}
