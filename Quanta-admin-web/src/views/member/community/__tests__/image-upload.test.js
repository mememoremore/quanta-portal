import { afterEach, describe, expect, it, vi } from 'vitest'
import { prepareForumImage } from '../image-upload'

describe('forum image preparation', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('keeps small images unchanged to preserve quality', async () => {
    const file = new File(['small'], 'photo.jpg', { type: 'image/jpeg' })
    expect(await prepareForumImage(file)).toBe(file)
  })

  it('rejects unsupported files and oversized animated GIFs', async () => {
    await expect(prepareForumImage(new File(['x'], 'x.svg', { type: 'image/svg+xml' }))).rejects.toThrow('仅支持')
    const gif = { type: 'image/gif', size: 5 * 1024 * 1024 + 1 }
    await expect(prepareForumImage(gif)).rejects.toThrow('GIF 图片不能超过 5 MB')
  })

  it('downscales a large JPEG at a quality-first setting before upload', async () => {
    vi.stubGlobal('Image', class {
      naturalWidth = 4000
      naturalHeight = 3000
      set src(value) { if (value) queueMicrotask(() => this.onload()) }
    })
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage: vi.fn() })
    const toBlob = vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((callback, type) => callback(new Blob(['compressed'], { type })))
    const original = { type: 'image/jpeg', size: 7 * 1024 * 1024, name: 'photo.jpg', lastModified: 1 }

    const result = await prepareForumImage(original)

    expect(result).toBeInstanceOf(File)
    expect(result.name).toBe('photo.jpg')
    expect(result.size).toBeLessThan(5 * 1024 * 1024)
    expect(toBlob).toHaveBeenCalledWith(expect.any(Function), 'image/jpeg', 0.88)
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test')
  })
})
