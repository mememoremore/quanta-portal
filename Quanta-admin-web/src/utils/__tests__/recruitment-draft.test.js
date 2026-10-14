import { describe, expect, it } from 'vitest'
import {
  clearRecruitmentDraft,
  loadRecruitmentDraft,
  saveRecruitmentDraft,
} from '../recruitment-draft'

describe('recruitment draft storage', () => {
  it('saves, loads and clears a recruitment draft', () => {
    saveRecruitmentDraft({ realName: '小李', firstChoice: 'FRONTEND' })
    expect(loadRecruitmentDraft()).toEqual({ realName: '小李', firstChoice: 'FRONTEND' })

    clearRecruitmentDraft()
    expect(loadRecruitmentDraft()).toBeNull()
  })

  it('does not try to persist a browser File', () => {
    const photoFile = new File(['photo'], 'photo.jpg', { type: 'image/jpeg' })
    const resumeFile = new File(['resume'], 'resume.pdf', { type: 'application/pdf' })

    saveRecruitmentDraft({ realName: '小李', photoFile, resumeFile, photoUrl: '/profile/a.jpg' })

    expect(loadRecruitmentDraft()).toEqual({ realName: '小李', photoUrl: '/profile/a.jpg' })
  })

  it('does not persist a temporary blob preview URL', () => {
    saveRecruitmentDraft({ realName: '小李', photoUrl: 'blob:temporary-preview' })

    expect(loadRecruitmentDraft()).toEqual({ realName: '小李', photoUrl: '' })
  })

  it('discards malformed storage instead of breaking the form', () => {
    localStorage.setItem('quanta_recruitment_draft', '{broken')

    expect(loadRecruitmentDraft()).toBeNull()
    expect(localStorage.getItem('quanta_recruitment_draft')).toBeNull()
  })
})
