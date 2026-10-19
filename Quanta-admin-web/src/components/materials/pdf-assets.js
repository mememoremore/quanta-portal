// Vite emits these as local, fingerprinted assets. No private PDF or asset request
// is sent to an external viewer/CDN; the lookup also rejects PDF-supplied URLs.
const assets = import.meta.glob([
  '/node_modules/pdfjs-dist/cmaps/*.bcmap',
  '/node_modules/pdfjs-dist/standard_fonts/*.{pfb,ttf}',
  '/node_modules/pdfjs-dist/wasm/{openjpeg,jbig2,qcms_bg}.wasm',
], { query: '?url', import: 'default', eager: true })

const directories = { cMapUrl: 'cmaps', standardFontDataUrl: 'standard_fonts', wasmUrl: 'wasm' }

export class LocalPdfDataFactory {
  async fetch({ kind, filename }) {
    const directory = directories[kind]
    const url = directory && assets[`/node_modules/pdfjs-dist/${directory}/${filename}`]
    if (!url) throw new Error('PDF 阅读资源不可用')
    const response = await fetch(url, { credentials: 'same-origin' })
    if (!response.ok) throw new Error('PDF 阅读资源加载失败')
    return new Uint8Array(await response.arrayBuffer())
  }
}
