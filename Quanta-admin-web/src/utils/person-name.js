export function displayPersonName(row = {}, fallback = '') {
  const realName = String(row?.realName || '').trim()
  return realName || row?.nickName || row?.userName || fallback
}
