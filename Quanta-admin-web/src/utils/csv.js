function escapeCsv(value) {
  const text = String(value ?? '')
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text
}

export function createCsv(columns, rows) {
  const lines = [columns.map((column) => escapeCsv(column.label)).join(',')]
  rows.forEach((row) => {
    lines.push(columns.map((column) => escapeCsv(row[column.key])).join(','))
  })
  return `\uFEFF${lines.join('\r\n')}`
}

export function downloadCsv(filename, columns, rows) {
  const blob = new Blob([createCsv(columns, rows)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
