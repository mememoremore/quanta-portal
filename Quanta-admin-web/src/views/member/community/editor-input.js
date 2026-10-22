import { nextTick } from 'vue'
import { safeMarkdownUrl } from './image-previews'

export function createEditorHistory(limit = 100) {
  let states = [], index = -1
  const copy = state => state ? { ...state } : null
  function ensure(state) {
    if (states[index]?.value !== state.value) { states = [copy(state)]; index = 0 }
    else states[index] = copy(state)
  }
  function record(state) {
    if (states[index]?.value === state.value) { states[index] = copy(state); return }
    states.splice(index + 1)
    states.push(copy(state))
    if (states.length > limit) states.shift()
    index = states.length - 1
  }
  return { ensure, record, get current() { return copy(states[index]) },
    undo() { return index > 0 ? copy(states[--index]) : null },
    redo() { return index < states.length - 1 ? copy(states[++index]) : null },
    rewrite(transform) { states = states.map(transform) },
  }
}

export function indentSelection({ value, start, end }, outdent) {
  const first = start === 0 ? 0 : value.lastIndexOf('\n', start - 1) + 1
  const last = end > start && value[end - 1] === '\n' ? end - 1 : end
  const endOfLine = value.indexOf('\n', last)
  const stop = endOfLine < 0 ? value.length : endOfLine
  let offset = first, deltaStart = 0, deltaEnd = 0
  const text = value.slice(first, stop).split('\n').map(line => {
    const removed = outdent ? (line.match(/^(?: {1,2}|\t)/)?.[0].length || 0) : 0
    const added = outdent ? 0 : 2
    if (offset <= start) deltaStart += added - Math.min(removed, start - offset)
    if (offset < end || start === end) deltaEnd += added - Math.min(removed, Math.max(0, end - offset))
    offset += line.length + 1
    return outdent ? line.slice(removed) : '  ' + line
  }).join('\n')
  return { value: value.slice(0, first) + text + value.slice(stop), start: start + deltaStart, end: end + deltaEnd }
}

let uploadSequence = 0
export function createEditorInput({ content, textArea, activePane, uploading, upload, prepare, saveDraft, onError = () => {}, onSuccess = () => {}, canEdit = () => true }) {
  const history = createEditorHistory()
  const abort = new AbortController()
  let alive = true, queue = Promise.resolve(), pending = 0
  function snapshot() {
    const value = content.value, field = textArea.value
    return { value, start: field?.selectionStart ?? value.length, end: field?.selectionEnd ?? value.length }
  }
  history.ensure(snapshot())
  function apply(state, focus = false) {
    if (!state || !alive) return
    content.value = state.value
    const field = textArea.value
    if (field) { field.value = state.value; field.setSelectionRange(state.start, state.end) }
    if (focus) {
      activePane.value = 'edit'
      nextTick(() => {
        if (alive && content.value === state.value) { textArea.value?.focus(); textArea.value?.setSelectionRange(state.start, state.end) }
      })
    }
  }
  function commit(state) {
    if (!canEdit()) return false
    if (state.value.length > 20000) { onError('正文最多 20000 字，请缩短后再插入'); return false }
    history.ensure(snapshot()); history.record(state); apply(state, true)
    return true
  }
  function literal(text) {
    const before = snapshot()
    return commit({ value: before.value.slice(0, before.start) + text + before.value.slice(before.end), start: before.start + text.length, end: before.start + text.length })
  }
  function insertText(before, after = '', placeholder = '') {
    const state = snapshot(), selected = state.value.slice(state.start, state.end) || placeholder
    return commit({ value: state.value.slice(0, state.start) + before + selected + after + state.value.slice(state.end), start: state.start + before.length, end: state.start + before.length + selected.length })
  }
  function onInput(event) { history.record({ value: event.target.value, start: event.target.selectionStart, end: event.target.selectionEnd }) }
  function undo(redo = false) { history.ensure(snapshot()); apply(redo ? history.redo() : history.undo()) }
  function onBeforeInput(event) {
    if (event.inputType === 'historyUndo' || event.inputType === 'historyRedo') {
      if (event.cancelable) { event.preventDefault(); undo(event.inputType === 'historyRedo') }
    } else history.ensure(snapshot())
  }
  function onKeydown(event) {
    if (event.isComposing || event.keyCode === 229 || event.altKey || !canEdit()) return
    const key = event.key.toLowerCase(), modifier = event.ctrlKey || event.metaKey
    if (key === 'tab' && !modifier) { event.preventDefault(); commit(indentSelection(snapshot(), event.shiftKey)); return }
    if (!modifier) return
    if (key === 'z' || key === 'y') { event.preventDefault(); undo(key === 'y' || event.shiftKey); return }
    if (key === 's') { event.preventDefault(); saveDraft?.(); return }
    const format = { b: ['**', '**', '加粗文字'], i: ['*', '*', '斜体文字'], k: ['[', '](https://)', '链接文字'] }[key]
    if (format && !event.shiftKey && !event.repeat) { event.preventDefault(); insertText(...format) }
  }
  function replaceMarker(marker, replacement) {
    if (!alive) return
    history.ensure(snapshot())
    history.rewrite(state => {
      const start = state.value.indexOf(marker)
      if (start < 0) return state
      if (state.value.length - marker.length + replacement.length > 20000) throw new Error('图片链接将超出正文 20000 字限制，请缩短正文后重试')
      const end = start + marker.length, delta = replacement.length - marker.length
      const position = value => value <= start ? value : value >= end ? value + delta : start + replacement.length
      return { value: state.value.slice(0, start) + replacement + state.value.slice(end), start: position(state.start), end: position(state.end) }
    })
    apply(history.current)
  }
  async function uploadFiles(files, text = '') {
    if (!alive || !canEdit()) return
    const images = [...files].filter(file => file.type.startsWith('image/'))
    if (!images.length) return
    if (pending + images.length > 10) { onError('请等待当前图片上传完成后再继续粘贴'); return }
    const markers = images.map(() => `[图片上传中 · ${++uploadSequence}]`)
    if (!literal(text + markers.join('\n'))) return
    pending += images.length; uploading.value = true
    const tasks = images.map((file, index) => {
      queue = queue.then(async () => {
        if (!alive) return
        try {
          const prepared = await prepare(file)
          if (!alive) return
          const url = safeMarkdownUrl(await upload(prepared, abort.signal), true)
          if (!url) throw new Error('服务端返回的图片地址无效')
          const name = file.name.replace(/[\\[\]()<>\r\n]/g, '').slice(0, 200) || '粘贴图片'
          const visible = content.value.includes(markers[index])
          replaceMarker(markers[index], `![${name}](${url.replaceAll('(', '%28').replaceAll(')', '%29')})`)
          if (alive && visible) onSuccess('图片已插入正文')
        } catch (error) {
          replaceMarker(markers[index], '')
          if (alive && error.name !== 'AbortError') onError(error.message || '图片上传失败，请重试')
        } finally {
          pending -= 1
          if (alive) uploading.value = pending > 0
        }
      })
      return queue
    })
    await Promise.all(tasks)
  }
  function onPaste(event) {
    const data = event.clipboardData
    if (!data) return
    let files = [...(data.items || [])].filter(item => item.kind === 'file' && item.type.startsWith('image/')).map(item => item.getAsFile()).filter(Boolean)
    if (!files.length) files = [...(data.files || [])].filter(file => file.type.startsWith('image/'))
    if (!files.length) return // Keep the browser's native text paste and copy behavior.
    event.preventDefault()
    void uploadFiles(files, data.getData?.('text/plain') || '')
  }
  return { insertText, onInput, onBeforeInput, onKeydown, onPaste, uploadFiles, destroy() { alive = false; abort.abort() } }
}
