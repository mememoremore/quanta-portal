import { describe, expect, it, vi } from 'vitest'
import { validateAvatarFile, avatarPreview, cropAvatar } from '../avatar-image'

describe('avatar image processing', () => {
  it('accepts static image types up to 5 MB only', () => {
    expect(validateAvatarFile(new File(['x'], 'a.png', { type: 'image/png' }))).toBe('')
    expect(validateAvatarFile({ type: 'image/gif', size: 20 })).toContain('JPG')
    expect(validateAvatarFile({ type: 'image/jpeg', size: 5 * 1024 * 1024 + 1 })).toContain('5 MB')
  })
  it('clamps crop offsets so a portrait never shows an empty edge', () => {
    const preview = avatarPreview({ naturalWidth: 1000, naturalHeight: 2000 }, { zoom: 1, x: 999, y: -999 })
    expect(preview).toMatchObject({ width: 256, height: 512, x: 0, y: -128 })
  })
  it('exports a 512px JPEG at moderate quality with the selected crop', async () => {
    const context = { fillRect: vi.fn(), drawImage: vi.fn() }
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(context)
    const blob = vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((cb) => cb(new Blob(['jpeg'], { type: 'image/jpeg' })))
    const file = await cropAvatar({ naturalWidth: 1000, naturalHeight: 2000 }, { zoom: 1, x: 0, y: 0 })
    expect(file.type).toBe('image/jpeg')
    expect(context.drawImage.mock.calls[0].slice(1)).toEqual([0, 500, 1000, 1000, 0, 0, 512, 512])
    expect(blob).toHaveBeenCalledWith(expect.any(Function), 'image/jpeg', .88)
  })
})
