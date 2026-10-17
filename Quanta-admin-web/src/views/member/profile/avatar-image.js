export function validateAvatarFile(file) {
  if (!file || !/^image\/(jpeg|png|webp)$/.test(file.type)) return '请选择 JPG、PNG 或 WebP 图片'
  if (!file.size || file.size > 5 * 1024 * 1024) return '图片不能为空，且不能超过 5 MB'
  return ''
}

export async function readAvatarFile(file) {
  const error = validateAvatarFile(file)
  if (error) throw new Error(error)
  const src = await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new Error('图片读取失败，请重新选择'))
    reader.readAsDataURL(file)
  })
  const image = await new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('图片无法解析，请更换图片'))
    img.src = src
  })
  if (!image.naturalWidth || !image.naturalHeight || image.naturalWidth * image.naturalHeight > 40_000_000) throw new Error('图片尺寸过大或无效，请选择 4000 万像素以内的图片')
  return { src, image }
}

export function avatarPreview(image, transform, frame = 256) {
  const zoom = Math.max(1, Math.min(3, Number(transform.zoom) || 1))
  const scale = frame / Math.min(image.naturalWidth, image.naturalHeight) * zoom
  const width = image.naturalWidth * scale, height = image.naturalHeight * scale
  const clamp = (offset, length) => Math.max(-(length - frame) / 2, Math.min((length - frame) / 2, Number(offset) || 0))
  return { width, height, x: clamp(transform.x, width), y: clamp(transform.y, height), scale }
}

export async function cropAvatar(image, transform) {
  const preview = avatarPreview(image, transform)
  const side = 256 / preview.scale
  const sx = (image.naturalWidth - side) / 2 - preview.x / preview.scale
  const sy = (image.naturalHeight - side) / 2 - preview.y / preview.scale
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = 512
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('浏览器无法处理图片，请更换浏览器后重试')
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, 512, 512)
  ctx.drawImage(image, sx, sy, side, side, 0, 0, 512, 512)
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', .88))
  if (!blob) throw new Error('头像处理失败，请重新选择图片')
  return new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
}
