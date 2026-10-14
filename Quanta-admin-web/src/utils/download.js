import { isMockEnabled, request, service } from './request'
import { notifyUnauthorized } from './unauthorized'

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

async function throwBlobBusinessError(blob) {
  if (!blob?.type?.includes('json')) return
  const payload = JSON.parse(await blob.text())
  if (Number(payload.code ?? 200) !== 200) {
    const error = new Error(payload.msg || '下载失败')
    error.code = Number(payload.code)
    error.payload = payload
    throw error
  }
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

  try {
    const response = await service.request({ ...config, responseType: 'blob' })
    await throwBlobBusinessError(response.data)
    const contentDisposition = response.headers?.['content-disposition'] || ''
    return {
      blob: response.data,
      fileName: parseContentDispositionFileName(contentDisposition) || fallbackFileName,
    }
  } catch (error) {
    const code = Number(error?.code ?? error?.response?.status)
    if (code === 401) await notifyUnauthorized(error)
    throw error
  }
}
