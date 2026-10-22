import { afterEach, describe, expect, it, vi } from 'vitest'
import { createEditorHistory, createEditorInput, indentSelection } from '../editor-input'

const snapshot = value => ({ value, start: value.length, end: value.length })
afterEach(() => vi.restoreAllMocks())
describe('editor selection and history', () => {
  it('keeps undo/redo bounded and rewrites asynchronous placeholders in redo too', () => {
    const history = createEditorHistory(3)
    history.ensure(snapshot('a'))
    history.record(snapshot('a[TOKEN]'))
    history.record(snapshot('a[TOKEN]b'))
    expect(history.undo().value).toBe('a[TOKEN]')
    history.rewrite(state => ({ ...state, value: state.value.replace('[TOKEN]', 'image') }))
    expect(history.redo().value).toBe('aimageb')
    history.record(snapshot('c')); history.record(snapshot('d')); history.record(snapshot('e'))
    expect(history.undo().value).toBe('d')
    expect(history.undo().value).toBe('c')
    expect(history.undo()).toBeNull()
  })
  it('indents selected lines and outdents without changing adjacent text', () => {
    expect(indentSelection({ value: '\na', start: 0, end: 0 }, false)).toEqual({ value: '  \na', start: 2, end: 2 })
    expect(indentSelection({ value: 'a\nb\nc', start: 0, end: 4 }, false).value).toBe('  a\n  b\nc')
    expect(indentSelection({ value: '  a\n\tb\nc', start: 2, end: 7 }, true).value).toBe('a\nb\nc')
  })
})

function setup(upload = vi.fn().mockResolvedValue('/files/pasted.png')) {
  const field = document.createElement('textarea'), content = { value: '原文' }
  field.value = content.value
  field.setSelectionRange(2, 2)
  const uploading = { value: false }, errors = vi.fn(), saveDraft = vi.fn()
  const input = createEditorInput({ content, textArea: { value: field }, activePane: { value: 'edit' }, uploading, upload, prepare: async file => file, onError: errors, saveDraft })
  return { input, field, content, uploading, errors, saveDraft }
}
describe('editor clipboard and shortcuts', () => {
  it('preserves native text paste/copy and skips shortcuts during composition', () => {
    const { input, content } = setup()
    const preventDefault = vi.fn()
    input.onPaste({ clipboardData: { items: [], files: [] }, preventDefault })
    input.onKeydown({ key: 'c', ctrlKey: true, preventDefault })
    input.onKeydown({ key: 'b', ctrlKey: true, isComposing: true, preventDefault })
    expect(preventDefault).not.toHaveBeenCalled()
    expect(content.value).toBe('原文')
    input.destroy()
  })
  it('wraps selection, saves draft without publishing and undoes the edit', () => {
    const { input, field, content, saveDraft } = setup()
    field.setSelectionRange(0, 2)
    const preventDefault = vi.fn()
    input.onKeydown({ key: 'b', ctrlKey: true, preventDefault })
    expect(content.value).toBe('**原文**')
    input.onKeydown({ key: 's', ctrlKey: true, preventDefault })
    expect(saveDraft).toHaveBeenCalledOnce()
    input.onKeydown({ key: 'z', ctrlKey: true, preventDefault })
    expect(content.value).toBe('原文')
    input.onKeydown({ key: 'z', metaKey: true, shiftKey: true, preventDefault })
    expect(content.value).toBe('**原文**')
    input.destroy()
  })
  it('inserts uploaded images at the pending marker without replacing later typing', async () => {
    let resolve
    const { input, field, content, uploading } = setup(() => new Promise(done => { resolve = done }))
    const pending = input.uploadFiles([new File(['x'], '截图.png', { type: 'image/png' })])
    await Promise.resolve(); await Promise.resolve()
    field.value = content.value + '后来输入的文字'
    field.setSelectionRange(field.value.length, field.value.length)
    content.value = field.value
    input.onInput({ target: field })
    resolve('/files/pasted.png')
    await pending
    expect(content.value).toBe('原文![截图.png](/files/pasted.png)后来输入的文字')
    expect(uploading.value).toBe(false)
    input.onKeydown({ key: 'z', ctrlKey: true, preventDefault() {} })
    expect(content.value).not.toContain('图片上传中')
    input.destroy()
  })
  it('does not resurrect images after undo or update an unmounted editor', async () => {
    let resolve
    const { input, content } = setup(() => new Promise(done => { resolve = done }))
    const pending = input.uploadFiles([new File(['x'], '截图.png', { type: 'image/png' })])
    await Promise.resolve(); await Promise.resolve()
    input.onKeydown({ key: 'z', ctrlKey: true, preventDefault() {} })
    resolve('/files/pasted.png'); await pending
    expect(content.value).toBe('原文')
    input.onKeydown({ key: 'y', ctrlKey: true, preventDefault() {} })
    expect(content.value).toContain('![截图.png](/files/pasted.png)')
    const second = input.uploadFiles([new File(['x'], 'later.png', { type: 'image/png' })])
    await Promise.resolve(); await Promise.resolve()
    const before = content.value
    input.destroy(); resolve('/files/later.png'); await second
    expect(content.value).toBe(before)
  })
  it('removes failed placeholders and rejects unsafe returned image URLs', async () => {
    const { input, content, errors } = setup(vi.fn().mockResolvedValue('javascript:alert(1)'))
    await input.uploadFiles([new File(['x'], '截图.png', { type: 'image/png' })])
    expect(content.value).toBe('原文')
    expect(errors).toHaveBeenCalled()
    input.destroy()
  })
  it('does not let a resolved image URL exceed the body limit', async () => {
    const { input, field, content, errors } = setup()
    content.value = 'a'.repeat(19985)
    field.value = content.value
    field.setSelectionRange(19985, 19985)
    await input.uploadFiles([new File(['x'], '截图.png', { type: 'image/png' })])
    expect(content.value).toBe('a'.repeat(19985))
    expect(errors).toHaveBeenCalledWith(expect.stringContaining('20000'))
    input.destroy()
  })
})
