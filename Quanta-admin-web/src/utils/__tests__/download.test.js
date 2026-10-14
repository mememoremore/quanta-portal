import { afterEach, describe, expect, it, vi } from 'vitest'
import { parseContentDispositionFileName, saveBlob } from '../download'

describe('download helpers', () => {
  afterEach(() => vi.restoreAllMocks())

  it('parses UTF-8 and ordinary content disposition filenames', () => {
    expect(
      parseContentDispositionFileName(
        "attachment; filename*=UTF-8''%E6%88%90%E5%91%98.xlsx",
      ),
    ).toBe('成员.xlsx')
    expect(parseContentDispositionFileName('attachment; filename="list.xlsx"')).toBe(
      'list.xlsx',
    )
  })

  it('creates and revokes an object URL when saving a blob', () => {
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    const click = vi.fn()
    const createElement = vi.spyOn(document, 'createElement').mockReturnValue({ click })

    saveBlob(new Blob(['test']), '名单.xlsx')

    expect(createObjectURL).toHaveBeenCalledOnce()
    expect(createElement).toHaveBeenCalledWith('a')
    expect(click).toHaveBeenCalledOnce()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test')
  })
})
