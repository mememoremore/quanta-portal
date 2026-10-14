import { request } from '@/utils/request'

export function getMetricsLatest() {
  return request({ url: '/qt/metrics/latest', method: 'get' })
}

export function getMetricsList(params) {
  return request({ url: '/qt/metrics/list', method: 'get', params })
}

export function getMetricsUris(params) {
  return request({ url: '/qt/metrics/uris', method: 'get', params })
}
