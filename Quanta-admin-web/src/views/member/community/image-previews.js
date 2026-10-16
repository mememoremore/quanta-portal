export function safeMarkdownUrl(value, image = false) {
  const url = String(value || '').trim()
  if (url.startsWith('/') && !url.startsWith('//')) return url
  if (/^https:\/\/[^\s"'<>]+$/i.test(url)) return url
  if (image && /^data:image\/(?:png|jpeg|webp|gif);base64,[a-z0-9+/=]+$/i.test(url)) return url
  if (!image && /^http:\/\/[^\s"'<>]+$/i.test(url)) return url
  return ''
}

// The feed only needs image URLs. Keep the full Markdown renderer and highlighter
// in the editor/detail routes instead of loading them for every feed card.
export function markdownImagePreviews(source) {
  const images = []
  let fenceChar = ''
  let fenceLength = 0

  for (const line of String(source || '').split(/\r?\n/)) {
    const marker = line.match(/^\s{0,3}(`{3,}|~{3,})/)
    if (marker) {
      if (!fenceChar) {
        fenceChar = marker[1][0]
        fenceLength = marker[1].length
        continue
      }
      if (marker[1][0] === fenceChar && marker[1].length >= fenceLength) {
        fenceChar = ''
        fenceLength = 0
        continue
      }
    }
    if (fenceChar) continue

    let inlineTicks = 0
    for (let index = 0; index < line.length; index += 1) {
      if (line[index] === '`') {
        let length = 1
        while (line[index + length] === '`') length += 1
        if (!inlineTicks) inlineTicks = length
        else if (inlineTicks === length) inlineTicks = 0
        index += length - 1
        continue
      }
      if (inlineTicks || line[index] !== '!' || line[index + 1] !== '[' || line[index - 1] === '\\') continue

      let cursor = index + 2
      let alt = ''
      while (cursor < line.length && line[cursor] !== ']') {
        if (line[cursor] === '\\' && cursor + 1 < line.length) cursor += 1
        alt += line[cursor]
        cursor += 1
      }
      if (line[cursor] !== ']' || line[cursor + 1] !== '(') continue

      cursor += 2
      let depth = 1
      let url = ''
      while (cursor < line.length && depth > 0) {
        const char = line[cursor]
        if (char === '\\' && cursor + 1 < line.length) {
          cursor += 1
          url += line[cursor]
        } else if (char === '(') {
          depth += 1
          url += char
        } else if (char === ')') {
          depth -= 1
          if (depth > 0) url += char
        } else {
          url += char
        }
        cursor += 1
      }
      if (depth !== 0) continue
      const src = safeMarkdownUrl(url, true)
      if (src) images.push({ alt: alt.trim() || '帖子配图', src })
      index = cursor - 1
    }
  }

  return images
}
