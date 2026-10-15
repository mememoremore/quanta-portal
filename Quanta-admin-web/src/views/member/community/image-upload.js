const MB = 1024 * 1024
const MAX_UPLOAD = 5 * MB
const MAX_SOURCE = 20 * MB
const MAX_EDGE = 2560
const IMAGE_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp', 'image/gif'])

function canvasBlob(canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error('图片压缩失败，请换一张图片重试')), type, quality)
  })
}

export async function prepareForumImage(file) {
  if (!IMAGE_TYPES.has(file.type)) throw new Error('仅支持 PNG、JPG、WEBP、GIF 图片')
  if (file.size > MAX_SOURCE) throw new Error('原图不能超过 20 MB')
  if (file.type === 'image/gif') {
    if (file.size > MAX_UPLOAD) throw new Error('GIF 图片不能超过 5 MB')
    return file // Do not flatten animated GIFs to a single canvas frame.
  }
  if (file.size <= 1.5 * MB) return file
  if (typeof URL.createObjectURL !== 'function' || typeof Image !== 'function' || typeof document === 'undefined') {
    if (file.size <= MAX_UPLOAD) return file
    throw new Error('当前浏览器无法压缩图片，请选择 5 MB 内的图片')
  }

  let objectUrl
  try {
    objectUrl = URL.createObjectURL(file)
    const image = await new Promise((resolve, reject) => {
      const element = new Image()
      element.onload = () => resolve(element)
      element.onerror = () => reject(new Error('无法读取图片，请更换图片重试'))
      element.src = objectUrl
    })
    const scale = Math.min(1, MAX_EDGE / Math.max(image.naturalWidth, image.naturalHeight))
    if (scale === 1 && file.size <= MAX_UPLOAD) return file

    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale))
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale))
    const context = canvas.getContext('2d')
    if (!context || typeof canvas.toBlob !== 'function') throw new Error('当前浏览器无法压缩图片')
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    let blob = await canvasBlob(canvas, file.type, 0.88)
    if (blob.size > MAX_UPLOAD && file.type !== 'image/png') blob = await canvasBlob(canvas, file.type, 0.82)
    if (blob.type !== file.type) throw new Error('当前浏览器不支持此图片格式的压缩')
    if (blob.size > MAX_UPLOAD) throw new Error('图片压缩后仍超过 5 MB，请选择更小的图片')
    if (blob.size >= file.size && file.size <= MAX_UPLOAD) return file
    return new File([blob], file.name, { type: file.type, lastModified: file.lastModified })
  } catch (error) {
    if (file.size <= MAX_UPLOAD && (error.message === '当前浏览器无法压缩图片' || error.message === '当前浏览器不支持此图片格式的压缩')) return file
    throw error
  } finally {
    if (objectUrl) URL.revokeObjectURL(objectUrl)
  }
}
