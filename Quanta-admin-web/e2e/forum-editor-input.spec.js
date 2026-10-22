import { test, expect, loginAs } from './support/fixture.js'

test('editor native text clipboard, formatting, indentation and local draft shortcuts', async ({ page, api }) => {
  await loginAs(page, 'member')
  await page.goto('/member/community/1/edit')
  const editor = page.getByRole('textbox', { name: '帖子正文' })
  await editor.fill('原文')
  await editor.press('ControlOrMeta+a')
  await editor.press('ControlOrMeta+b')
  await expect(editor).toHaveValue('**原文**')
  await editor.press('ControlOrMeta+z')
  await expect(editor).toHaveValue('原文')
  await editor.press('ControlOrMeta+Shift+z')
  await expect(editor).toHaveValue('**原文**')
  await page.context().grantPermissions(['clipboard-read', 'clipboard-write'])
  await editor.press('ControlOrMeta+a'); await editor.press('ControlOrMeta+c')
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe('**原文**')
  await page.evaluate(() => navigator.clipboard.writeText('粘贴文本'))
  await editor.press('ControlOrMeta+v')
  await expect(editor).toHaveValue('粘贴文本')
  await editor.press('Tab'); await expect(editor).toHaveValue('  粘贴文本')
  await editor.press('Shift+Tab'); await expect(editor).toHaveValue('粘贴文本')
  await editor.press('ControlOrMeta+s')
  await expect.poll(() => page.evaluate(() => Object.values(localStorage).some(value => value.includes('粘贴文本')))).toBe(true)
  expect(api.requests.some(request => request.startsWith('PUT /qt/forum/posts'))).toBe(false)
})

test('pasted image uploads once and preserves text typed while upload is pending', async ({ page, api }) => {
  await loginAs(page, 'member')
  await page.goto('/member/community/new')
  const editor = page.getByRole('textbox', { name: '帖子正文' })
  await editor.fill('开始')
  await editor.press('ControlOrMeta+End')
  let release, uploads = 0
  const pending = new Promise(resolve => { release = resolve })
  await page.route('**/qt/forum/images', async route => {
    if (route.request().method() === 'OPTIONS') return route.fallback()
    uploads++
    await pending
    await route.fulfill({ json: { code: 200, url: '/test-upload.png' }, headers: { 'access-control-allow-origin': 'http://127.0.0.1:5177', 'access-control-allow-credentials': 'true' } })
  })
  await page.route('**/test-upload.png', route => route.fulfill({ contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2n7sAAAAASUVORK5CYII=', 'base64') }))
  await editor.evaluate(field => {
    const canvas = document.createElement('canvas'); canvas.width = canvas.height = 10
    return new Promise(resolve => canvas.toBlob(blob => {
      const data = new DataTransfer(); data.items.add(new File([blob], 'clipboard.png', { type: 'image/png' }))
      field.dispatchEvent(new ClipboardEvent('paste', { clipboardData: data, bubbles: true, cancelable: true }))
      resolve()
    }))
  })
  await expect.poll(() => uploads).toBe(1)
  await editor.press('ControlOrMeta+End'); await editor.pressSequentially('继续编辑')
  release()
  await expect(editor).toHaveValue('开始![clipboard.png](/test-upload.png)继续编辑')
  await editor.press('ControlOrMeta+z')
  expect(await editor.inputValue()).not.toContain('图片上传中')
  expect(api.unexpected).toEqual([])
})
