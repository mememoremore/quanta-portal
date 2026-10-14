const RECRUITMENT_DRAFT_KEY = 'quanta_recruitment_draft'
const PHOTO_DB_NAME = 'quanta_portal_drafts'
const PHOTO_STORE_NAME = 'files'
const PHOTO_KEY = 'recruitment_photo'
const RESUME_KEY = 'recruitment_resume'

function openPhotoDatabase() {
  if (typeof indexedDB === 'undefined') return Promise.resolve(null)
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(PHOTO_DB_NAME, 1)
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(PHOTO_STORE_NAME)) {
        request.result.createObjectStore(PHOTO_STORE_NAME)
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

async function usePhotoStore(mode, action) {
  const database = await openPhotoDatabase()
  if (!database) return null
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(PHOTO_STORE_NAME, mode)
    const request = action(transaction.objectStore(PHOTO_STORE_NAME))
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
    transaction.oncomplete = () => database.close()
    transaction.onerror = () => reject(transaction.error)
  })
}

export function loadRecruitmentDraft() {
  const stored = localStorage.getItem(RECRUITMENT_DRAFT_KEY)
  if (!stored) return null
  try {
    const value = JSON.parse(stored)
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('invalid draft')
    return value
  } catch {
    localStorage.removeItem(RECRUITMENT_DRAFT_KEY)
    return null
  }
}

export function saveRecruitmentDraft(form) {
  const draft = { ...form }
  delete draft.photoFile
  delete draft.resumeFile
  if (String(draft.photoUrl || '').startsWith('blob:')) draft.photoUrl = ''
  localStorage.setItem(RECRUITMENT_DRAFT_KEY, JSON.stringify(draft))
}

export function clearRecruitmentDraft() {
  localStorage.removeItem(RECRUITMENT_DRAFT_KEY)
}

export async function saveRecruitmentPhotoDraft(file) {
  if (!(file instanceof Blob)) return clearRecruitmentPhotoDraft()
  return usePhotoStore('readwrite', (store) => store.put({
    blob: file,
    name: file.name || '证件照',
    type: file.type || '',
    lastModified: file.lastModified || Date.now(),
  }, PHOTO_KEY))
}

export async function loadRecruitmentPhotoDraft() {
  const record = await usePhotoStore('readonly', (store) => store.get(PHOTO_KEY))
  if (!record?.blob) return null
  return new File([record.blob], record.name, {
    type: record.type,
    lastModified: record.lastModified,
  })
}

export async function clearRecruitmentPhotoDraft() {
  return usePhotoStore('readwrite', (store) => store.delete(PHOTO_KEY))
}

export async function saveRecruitmentResumeDraft(file) {
  if (!(file instanceof Blob)) return clearRecruitmentResumeDraft()
  return usePhotoStore('readwrite', (store) => store.put({
    blob: file,
    name: file.name || '简历.pdf',
    type: file.type || 'application/pdf',
    lastModified: file.lastModified || Date.now(),
  }, RESUME_KEY))
}

export async function loadRecruitmentResumeDraft() {
  const record = await usePhotoStore('readonly', (store) => store.get(RESUME_KEY))
  if (!record?.blob) return null
  return new File([record.blob], record.name, {
    type: record.type,
    lastModified: record.lastModified,
  })
}

export async function clearRecruitmentResumeDraft() {
  return usePhotoStore('readwrite', (store) => store.delete(RESUME_KEY))
}
