import { isMockEnabled, request } from './request'

export function parseContentDispositionFileName(header = '') {
  const encoded = header.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) return decodeURIComponent(encoded)
  return header.match(/filename="?([^";]+)"?/i)?.[1] || ''
}

export function saveBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  URL.revokeObjectURL(url)
}

export async function requestBlob(config, fallbackFileName = 'download') {
  if (isMockEnabled()) {
    const payload = await request(config)
    const data = payload.data || {}
    const blob = data.blob instanceof Blob
      ? data.blob
      : new Blob([data.content || payload.msg || 'Mock download'])
    return { blob, fileName: data.fileName || fallbackFileName }
  }

  const response = await request({ ...config, responseType: 'blob' })
  const contentDisposition = response.headers?.['content-disposition'] || ''
  return {
    blob: response.data,
    fileName: parseContentDispositionFileName(contentDisposition) || fallbackFileName,
  }
}
