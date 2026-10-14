export function createDepartmentFileCache() {
  const files = new Map()
  const keyOf = (department) => String(department || '').trim().toUpperCase()

  return {
    get(department) {
      return files.get(keyOf(department)) || null
    },
    set(department, file) {
      const key = keyOf(department)
      if (key && file) files.set(key, file)
    },
    clear(department) {
      const key = keyOf(department)
      if (key) files.delete(key)
    },
  }
}
