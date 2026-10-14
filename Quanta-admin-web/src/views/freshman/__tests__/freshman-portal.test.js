import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as recruitmentApi from '@/api/portal/recruitment'
import * as activityApi from '@/api/portal/activities'
import * as draftStorage from '@/utils/recruitment-draft'
import ApplicationForm, {
  emptyApplication,
} from '@/views/freshman/recruitment/components/ApplicationForm.vue'
import RecruitmentDepartmentSelect from '@/views/freshman/recruitment/components/RecruitmentDepartmentSelect.vue'
import InterviewTimeline from '@/views/freshman/recruitment/components/InterviewTimeline.vue'
import OfferAcceptanceDialog from '@/views/freshman/recruitment/components/OfferAcceptanceDialog.vue'
import RecruitmentPage from '@/views/freshman/recruitment/index.vue'
import EventsPage from '@/views/freshman/events/index.vue'
import PortalNoticeDialog from '@/components/PortalNoticeDialog.vue'

const navigation = vi.hoisted(() => ({ replace: vi.fn(), reset: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: navigation.replace }) }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ reset: navigation.reset }) }))

vi.mock('@/api/portal/recruitment', () => ({
  recruitmentDepartments: [
    { value: 'PRODUCT', label: '产品' },
    { value: 'DESIGN', label: '设计' },
    { value: 'FRONTEND', label: '全栈（前端）' },
    { value: 'BACKEND', label: '全栈（后端）' },
  ],
  getMyApplication: vi.fn(),
  getMyRecruitmentState: vi.fn(),
  getMyInterviewProcess: vi.fn(),
  acceptOffer: vi.fn(),
  submitApplication: vi.fn(),
}))

vi.mock('@/api/portal/activities', () => ({
  getPublishedActivities: vi.fn(),
  getMyActivitySignup: vi.fn(),
  signupActivity: vi.fn(),
}))

vi.mock('@/utils/recruitment-draft', () => ({
  loadRecruitmentDraft: vi.fn(),
  saveRecruitmentDraft: vi.fn(),
  clearRecruitmentDraft: vi.fn(),
  loadRecruitmentPhotoDraft: vi.fn(),
  saveRecruitmentPhotoDraft: vi.fn(),
  clearRecruitmentPhotoDraft: vi.fn(),
  loadRecruitmentResumeDraft: vi.fn(),
  saveRecruitmentResumeDraft: vi.fn(),
  clearRecruitmentResumeDraft: vi.fn(),
}))

describe('freshman recruitment components', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    navigation.replace.mockResolvedValue(undefined)
    recruitmentApi.getMyApplication.mockResolvedValue(null)
    recruitmentApi.getMyRecruitmentState.mockResolvedValue({
      application: { ...emptyApplication(), applicationId: 1, realName: '新生小李' },
      newApplicationsOpen: false,
      canUpdate: true,
    })
    recruitmentApi.getMyInterviewProcess.mockResolvedValue([])
    recruitmentApi.submitApplication.mockResolvedValue({ code: 200 })
    recruitmentApi.acceptOffer.mockResolvedValue({ code: 200, data: { acceptedDepartment: 'FRONTEND' } })
    draftStorage.loadRecruitmentDraft.mockReturnValue(null)
    draftStorage.loadRecruitmentPhotoDraft.mockResolvedValue(null)
    draftStorage.loadRecruitmentResumeDraft.mockResolvedValue(null)
    draftStorage.saveRecruitmentPhotoDraft.mockResolvedValue(undefined)
    draftStorage.saveRecruitmentResumeDraft.mockResolvedValue(undefined)
    draftStorage.clearRecruitmentPhotoDraft.mockResolvedValue(undefined)
    draftStorage.clearRecruitmentResumeDraft.mockResolvedValue(undefined)
  })

  it('offers only active departments and submits a browser form model', async () => {
    const wrapper = mount(ApplicationForm, {
      props: {
        initialValue: {
          ...emptyApplication(),
          realName: '新生小李',
          gender: '男',
          className: '软工2402',
          firstChoice: 'FRONTEND',
          secondChoice: 'BACKEND',
          storedPhotoUrl: '/profile/photo.jpg',
          storedResumeUrl: '/profile/resume.pdf',
          selfIntro: '自我介绍',
          codingExperience: '0',
          quantaUnderstanding: '开放与创造',
        },
      },
    })

    expect(wrapper.text()).toContain('全栈（后端）')
    expect(wrapper.text()).not.toContain('安卓')
    expect(wrapper.findAll('select[name="gender"] option:not([disabled])')).toHaveLength(2)
    const choiceSelectors = wrapper.findAllComponents(RecruitmentDepartmentSelect)
    expect(choiceSelectors).toHaveLength(2)
    await choiceSelectors[0].vm.$emit('update:modelValue', 'PRODUCT')
    await choiceSelectors[1].vm.$emit('update:modelValue', 'DESIGN')
    await wrapper.find('input[name="realName"]').setValue('新生小李')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toHaveLength(1)
    expect(wrapper.emitted('submit')[0][0].realName).toBe('新生小李')
    expect(wrapper.emitted('submit')[0][0]).toEqual(expect.objectContaining({
      firstChoice: 'PRODUCT',
      secondChoice: 'DESIGN',
    }))
  })

  it('keeps the existing duplicate-choice validation with custom selectors', async () => {
    const wrapper = mount(ApplicationForm, {
      props: {
        initialValue: {
          ...emptyApplication(),
          realName: '新生小李',
          gender: '男',
          className: '软工2402',
          firstChoice: 'DESIGN',
          secondChoice: 'DESIGN',
          storedPhotoUrl: '/profile/photo.jpg',
          storedResumeUrl: '/profile/resume.pdf',
          selfIntro: '自我介绍',
          codingExperience: '0',
          quantaUnderstanding: '开放与创造',
        },
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.emitted('invalid')[0][0]).toContain('第一、第二志愿不能相同')
  })

  it('blocks incomplete applications and reports every missing required field', async () => {
    const wrapper = mount(ApplicationForm, {
      props: { initialValue: emptyApplication() },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.emitted('invalid')).toHaveLength(1)
    expect(wrapper.emitted('invalid')[0][0]).toEqual(
      expect.arrayContaining(['姓名', '性别', '班级', '第一志愿', '第二志愿', '证件照', 'PDF 简历', '自我介绍', '编程经验', '对 Quanta 的了解']),
    )
  })

  it('requires a description only when programming experience is selected', async () => {
    const wrapper = mount(ApplicationForm, {
      props: {
        initialValue: {
          ...emptyApplication(),
          realName: '新生小李',
          gender: '男',
          className: '软工2402',
          firstChoice: 'FRONTEND',
          secondChoice: 'BACKEND',
          storedPhotoUrl: '/profile/photo.jpg',
          storedResumeUrl: '/profile/resume.pdf',
          selfIntro: '自我介绍',
          codingExperience: '1',
          quantaUnderstanding: '开放与创造',
        },
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.emitted('invalid')[0][0]).toContain('编程经历说明')
  })

  it('limits resume long-text fields to 500 characters and shows live counters', async () => {
    const wrapper = mount(ApplicationForm, {
      props: {
        initialValue: {
          ...emptyApplication(),
          selfIntro: '你好',
          codingExperience: '1',
          codingExperienceDesc: 'Vue',
          quantaUnderstanding: '开放与创造',
        },
      },
    })

    const textareas = wrapper.findAll('textarea')
    expect(textareas).toHaveLength(3)
    textareas.forEach((textarea) => expect(textarea.attributes('maxlength')).toBe('500'))
    expect(wrapper.text().match(/500 字以内/g)).toHaveLength(3)
    expect(wrapper.get('[data-testid="selfIntro-count"]').text()).toBe('2 / 500')
    expect(wrapper.get('[data-testid="codingExperienceDesc-count"]').text()).toBe('3 / 500')
    expect(wrapper.get('[data-testid="quantaUnderstanding-count"]').text()).toBe('5 / 500')
  })

  it('blocks an oversized value restored from historical data', async () => {
    const wrapper = mount(ApplicationForm, {
      props: {
        initialValue: {
          ...emptyApplication(),
          realName: '新生小李',
          gender: '男',
          className: '软工2402',
          firstChoice: 'FRONTEND',
          secondChoice: 'BACKEND',
          storedPhotoUrl: '/profile/photo.jpg',
          storedResumeUrl: '/profile/resume.pdf',
          selfIntro: '测'.repeat(501),
          codingExperience: '0',
          quantaUnderstanding: '开放与创造',
        },
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.emitted('invalid')[0][0]).toContain('自我介绍请控制在 500 字以内')
    expect(wrapper.text()).toContain('自我介绍请控制在 500 字以内')
  })

  it('renders a branded photo picker and previews a valid image', async () => {
    const createObjectURL = vi.fn(() => 'blob:photo-preview')
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createObjectURL })
    const wrapper = mount(ApplicationForm, {
      props: { initialValue: emptyApplication() },
    })
    const input = wrapper.get('input[name="photoFile"]')
    const photo = new File(['photo'], 'quanta-photo.png', { type: 'image/png' })

    expect(wrapper.get('[data-testid="photo-upload-card"]').text()).toContain('选择照片')
    expect(input.classes()).toContain('sr-only')
    expect(wrapper.get('[data-testid="photo-file-name"]').classes()).not.toContain('is-ready')
    Object.defineProperty(input.element, 'files', { configurable: true, value: [photo] })
    await input.trigger('change')

    expect(createObjectURL).toHaveBeenCalledWith(photo)
    expect(wrapper.get('[data-testid="photo-file-name"]').text()).toBe('quanta-photo.png')
    expect(wrapper.get('[data-testid="photo-file-name"]').classes()).toContain('is-ready')
    expect(wrapper.get('img[alt="证件照预览"]').attributes('src')).toBe('blob:photo-preview')
  })

  it('rejects unsupported and oversized photo files in place', async () => {
    const wrapper = mount(ApplicationForm, {
      props: { initialValue: emptyApplication() },
    })
    const input = wrapper.get('input[name="photoFile"]')
    const unsupported = new File(['text'], 'notes.txt', { type: 'text/plain' })

    Object.defineProperty(input.element, 'files', { configurable: true, value: [unsupported] })
    await input.trigger('change')
    expect(wrapper.get('[data-testid="photo-error"]').text()).toContain('JPG、PNG 或 WebP')

    const gif = new File(['photo'], 'photo.gif', { type: 'image/gif' })
    Object.defineProperty(input.element, 'files', { configurable: true, value: [gif] })
    await input.trigger('change')
    expect(wrapper.get('[data-testid="photo-error"]').text()).toContain('JPG、PNG 或 WebP')

    const oversized = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.jpg', {
      type: 'image/jpeg',
    })
    Object.defineProperty(input.element, 'files', { configurable: true, value: [oversized] })
    await input.trigger('change')
    expect(wrapper.get('[data-testid="photo-error"]').text()).toContain('不能超过 10 MB')
  })

  it('accepts only PDF resumes up to 10 MB and includes the file in the form snapshot', async () => {
    const wrapper = mount(ApplicationForm, {
      props: {
        initialValue: {
          ...emptyApplication(),
          realName: '新生小李',
          gender: '男',
          className: '软工2402',
          firstChoice: 'FRONTEND',
          secondChoice: 'BACKEND',
          storedPhotoUrl: '/profile/photo.jpg',
          selfIntro: '自我介绍',
          codingExperience: '0',
          quantaUnderstanding: '开放与创造',
        },
      },
    })
    const input = wrapper.get('input[name="resumeFile"]')
    const resume = new File(['resume'], 'quanta-resume.pdf', { type: 'application/pdf' })

    Object.defineProperty(input.element, 'files', { configurable: true, value: [resume] })
    await input.trigger('change')
    expect(wrapper.get('[data-testid="resume-file-name"]').text()).toContain('quanta-resume.pdf')
    expect(wrapper.get('[data-testid="resume-file-name"]').classes()).toContain('is-ready')

    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit').at(-1)[0].resumeFile).toBe(resume)

    const unsupported = new File(['word'], 'resume.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    Object.defineProperty(input.element, 'files', { configurable: true, value: [unsupported] })
    await input.trigger('change')
    expect(wrapper.get('[data-testid="resume-error"]').text()).toContain('PDF')

    const oversized = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.pdf', {
      type: 'application/pdf',
    })
    Object.defineProperty(input.element, 'files', { configurable: true, value: [oversized] })
    await input.trigger('change')
    expect(wrapper.get('[data-testid="resume-error"]').text()).toContain('不能超过 10 MB')
  })

  it('falls back to the clean placeholder when a stored photo cannot load', async () => {
    const wrapper = mount(ApplicationForm, {
      props: { initialValue: { ...emptyApplication(), photoUrl: '/expired-photo.jpg' } },
    })

    await wrapper.get('img[alt="证件照预览"]').trigger('error')

    expect(wrapper.find('img[alt="证件照预览"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="photo-file-name"]').text()).toContain('暂时无法预览')
    expect(wrapper.get('[data-testid="photo-upload-card"]').text()).toContain('证件照预览')
  })

  it('renders interview invitations as read-only status', () => {
    const wrapper = mount(InterviewTimeline, {
      props: {
        processes: [
          {
            departmentCode: 'FRONTEND',
            departmentName: '全栈（前端）',
            stages: [
              { key: 'first', title: '一面', status: 'passed' },
              { key: 'second', title: '二面', status: 'invited' },
              { key: 'offer', title: '录用', status: 'locked' },
            ],
          },
        ],
      },
    })

    expect(wrapper.text()).toContain('等待面试')
    expect(wrapper.text()).not.toContain('接受邀请')
    expect(wrapper.text()).not.toContain('拒绝邀请')
    expect(wrapper.findAll('button')).toHaveLength(0)
  })

  it('shows accept only for passed departments after all advanced results are final', async () => {
    const processes = ['FRONTEND', 'BACKEND'].map((departmentCode) => ({
      departmentCode,
      departmentName: departmentCode,
      stages: [
        { key: 'first', title: '一面', status: 'passed' },
        { key: 'second', title: '二面', status: departmentCode === 'FRONTEND' ? 'passed' : 'invited' },
        { key: 'offer', title: '录用', status: 'pending' },
      ],
    }))
    const wrapper = mount(InterviewTimeline, { props: { processes, acceptanceReady: false } })
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.findAll('[data-test="accept-offer"]')).toHaveLength(0)

    processes[1].stages[1].status = 'rejected'
    await wrapper.setProps({ processes: structuredClone(processes), acceptanceReady: true })
    expect(wrapper.findAll('[data-test="accept-offer"]')).toHaveLength(1)
    await wrapper.get('[data-test="accept-offer"]').trigger('click')
    expect(wrapper.emitted('accept')?.[0]).toEqual(['FRONTEND'])

    processes[1].stages[1].status = 'passed'
    await wrapper.setProps({ processes: structuredClone(processes) })
    expect(wrapper.findAll('[data-test="accept-offer"]')).toHaveLength(2)
    await wrapper.setProps({ acceptedDepartment: 'FRONTEND' })
    expect(wrapper.findAll('[data-test="accept-offer"]')).toHaveLength(0)
  })

  it('warns before accepting and blocks a second confirm during submission', async () => {
    const wrapper = mount(OfferAcceptanceDialog, {
      props: { modelValue: true, departmentName: '前端部', submitting: false },
      global: { stubs: {
        ElDialog: { template: '<section><slot name="header"/><slot/><slot name="footer"/></section>' },
        ElButton: { template: '<button v-bind="$attrs"><slot/></button>' },
      } },
    })
    expect(wrapper.text()).toContain('确认后只能加入该部门，另一志愿将不可再接受')
    await wrapper.get('[data-test="confirm-offer"]').trigger('click')
    expect(wrapper.emitted('confirm')).toHaveLength(1)
    await wrapper.setProps({ submitting: true })
    expect(wrapper.get('[data-test="confirm-offer"]').attributes('disabled')).toBeDefined()
  })

  it('accepts once only after confirmation and then opens member login', async () => {
    let resolveOffer
    recruitmentApi.acceptOffer.mockReturnValue(new Promise((resolve) => { resolveOffer = resolve }))
    recruitmentApi.getMyRecruitmentState.mockResolvedValueOnce({
      application: { ...emptyApplication(), applicationId: 1 },
      newApplicationsOpen: false,
      canUpdate: true,
      offerAcceptance: { ready: true, acceptedDepartment: null },
    })
    recruitmentApi.getMyInterviewProcess.mockResolvedValueOnce(['FRONTEND', 'BACKEND'].map((departmentCode) => ({
      departmentCode, departmentName: departmentCode,
      stages: [{ key: 'first', title: '一面', status: 'passed' }, { key: 'second', title: '二面', status: 'passed' }, { key: 'offer', title: '录用', status: 'pending' }],
    })))
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    await wrapper.findAll('.freshman-tabs button')[2].trigger('click')
    wrapper.findComponent(InterviewTimeline).vm.$emit('accept', 'FRONTEND')
    await wrapper.vm.$nextTick()
    expect(recruitmentApi.acceptOffer).not.toHaveBeenCalled()
    const dialog = wrapper.findComponent(OfferAcceptanceDialog)
    expect(dialog.props('modelValue')).toBe(true)
    dialog.vm.$emit('confirm')
    dialog.vm.$emit('confirm')
    await wrapper.vm.$nextTick()
    expect(recruitmentApi.acceptOffer).toHaveBeenCalledTimes(1)
    expect(navigation.reset).not.toHaveBeenCalled()
    resolveOffer({ code: 200, data: { acceptedDepartment: 'FRONTEND' } })
    await flushPromises()
    expect(navigation.reset).toHaveBeenCalledOnce()
    expect(navigation.replace).toHaveBeenCalledWith('/login/member')
  })

  it('keeps freshman login and shows the server reason when acceptance fails', async () => {
    recruitmentApi.getMyRecruitmentState.mockResolvedValueOnce({
      application: { ...emptyApplication(), applicationId: 1 },
      newApplicationsOpen: false,
      canUpdate: true,
      offerAcceptance: { ready: true, acceptedDepartment: null },
    })
    recruitmentApi.getMyInterviewProcess.mockResolvedValueOnce([{ departmentCode: 'FRONTEND', departmentName: '前端部', stages: [
      { key: 'first', title: '一面', status: 'passed' }, { key: 'second', title: '二面', status: 'passed' }, { key: 'offer', title: '录用', status: 'pending' },
    ] }])
    recruitmentApi.acceptOffer.mockRejectedValueOnce({ payload: { msg: '已接受其他部门' } })
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    await wrapper.findAll('.freshman-tabs button')[2].trigger('click')
    wrapper.findComponent(InterviewTimeline).vm.$emit('accept', 'FRONTEND')
    await wrapper.vm.$nextTick()
    wrapper.findComponent(OfferAcceptanceDialog).vm.$emit('confirm')
    await flushPromises()
    expect(wrapper.text()).toContain('已接受其他部门')
    expect(navigation.reset).not.toHaveBeenCalled()
    expect(navigation.replace).not.toHaveBeenCalled()
  })

  it('shows an early PASS result without an accept action while another result is pending', async () => {
    recruitmentApi.getMyRecruitmentState.mockResolvedValueOnce({
      application: { ...emptyApplication(), applicationId: 1 },
      newApplicationsOpen: false,
      canUpdate: true,
      offerAcceptance: { ready: true, acceptedDepartment: null },
    })
    recruitmentApi.getMyInterviewProcess.mockResolvedValueOnce(['FRONTEND', 'BACKEND'].map((departmentCode) => ({
      departmentCode, departmentName: departmentCode,
      stages: [
        { key: 'first', title: '一面', status: 'passed' },
        { key: 'second', title: '二面', status: departmentCode === 'FRONTEND' ? 'passed' : 'invited' },
        { key: 'offer', title: '录用', status: 'pending' },
      ],
    })))
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    await wrapper.findAll('.freshman-tabs button')[2].trigger('click')
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.findAll('[data-test="accept-offer"]')).toHaveLength(0)
  })

  it('clears the draft only after the backend accepts an application', async () => {
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    const payload = { ...emptyApplication(), realName: '新生小李' }

    wrapper.findComponent(ApplicationForm).vm.$emit('submit', payload)
    await flushPromises()

    expect(recruitmentApi.submitApplication).toHaveBeenCalledWith(payload)
    expect(draftStorage.clearRecruitmentDraft).toHaveBeenCalledOnce()
    expect(draftStorage.clearRecruitmentPhotoDraft).toHaveBeenCalledOnce()
    expect(draftStorage.clearRecruitmentResumeDraft).toHaveBeenCalledOnce()
    expect(wrapper.findComponent(PortalNoticeDialog).props()).toEqual(expect.objectContaining({
      modelValue: true,
      title: '提交成功',
      message: '报名已成功提交，可在“查看进度”中查看后续安排。',
      type: 'success',
    }))
    expect(wrapper.get('.freshman-tabs button.active').text()).toBe('查看进度')
  })

  it('retains the draft when application submission fails', async () => {
    recruitmentApi.submitApplication.mockRejectedValueOnce(new Error('网络异常'))
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    const payload = { ...emptyApplication(), realName: '新生小李' }

    wrapper.findComponent(ApplicationForm).vm.$emit('submit', payload)
    await flushPromises()

    expect(draftStorage.clearRecruitmentDraft).not.toHaveBeenCalled()
    expect(draftStorage.saveRecruitmentDraft).toHaveBeenCalledWith(payload)
    expect(wrapper.findComponent(PortalNoticeDialog).props()).toEqual(expect.objectContaining({
      modelValue: true,
      title: '提交失败',
      message: '网络连接异常，请稍后重试',
      type: 'error',
    }))
  })

  it('shows an explicit backend submission message when one is provided', async () => {
    const error = Object.assign(new Error('请求失败'), { payload: { msg: 'PDF 文件上传失败，请重新选择' } })
    recruitmentApi.submitApplication.mockRejectedValueOnce(error)
    const wrapper = mount(RecruitmentPage)
    await flushPromises()

    wrapper.findComponent(ApplicationForm).vm.$emit('submit', emptyApplication())
    await flushPromises()

    expect(wrapper.findComponent(PortalNoticeDialog).props('message')).toBe('PDF 文件上传失败，请重新选择')
  })

  it('shows backend photo, resume and length limit messages from error.message', async () => {
    const cases = [
      '证件照不能超过 10 MB，请压缩后重试',
      'PDF 简历不能超过 10 MB，请压缩后重试',
      '编程经历说明过长，请控制在 2000 字以内',
    ]
    for (const message of cases) {
      recruitmentApi.submitApplication.mockRejectedValueOnce(new Error(message))
      const wrapper = mount(RecruitmentPage)
      await flushPromises()
      wrapper.findComponent(ApplicationForm).vm.$emit('submit', { ...emptyApplication(), realName: '新生小李' })
      await flushPromises()
      expect(wrapper.findComponent(PortalNoticeDialog).props()).toEqual(expect.objectContaining({
        modelValue: true,
        title: '提交失败',
        message,
        type: 'error',
      }))
      wrapper.unmount()
    }
  })

  it('keeps the generic submission message when the backend gives no explicit reason', async () => {
    recruitmentApi.submitApplication.mockRejectedValueOnce(new Error('Request failed with status code 500'))
    const wrapper = mount(RecruitmentPage)
    await flushPromises()

    wrapper.findComponent(ApplicationForm).vm.$emit('submit', emptyApplication())
    await flushPromises()

    expect(wrapper.findComponent(PortalNoticeDialog).props('message')).toBe('报名提交失败，请检查填写内容后重试')
  })

  it('restores a newer local draft over the last server application', async () => {
    recruitmentApi.getMyRecruitmentState.mockResolvedValueOnce({
      application: {
        ...emptyApplication(),
        realName: '服务端旧姓名',
        firstChoice: 'PRODUCT',
      },
      newApplicationsOpen: false,
      canUpdate: true,
    })
    draftStorage.loadRecruitmentDraft.mockReturnValueOnce({
      realName: '草稿新姓名',
      firstChoice: 'FRONTEND',
    })

    const wrapper = mount(RecruitmentPage)
    await flushPromises()

    expect(wrapper.findComponent(ApplicationForm).props('initialValue')).toEqual(
      expect.objectContaining({ realName: '草稿新姓名', firstChoice: 'FRONTEND' }),
    )
  })

  it('keeps a successful submission successful when the process refresh fails', async () => {
    recruitmentApi.getMyInterviewProcess
      .mockResolvedValueOnce([])
      .mockRejectedValueOnce(new Error('进度刷新失败'))
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    const payload = { ...emptyApplication(), realName: '新生小李' }

    wrapper.findComponent(ApplicationForm).vm.$emit('submit', payload)
    await flushPromises()

    expect(draftStorage.clearRecruitmentDraft).toHaveBeenCalledOnce()
    expect(draftStorage.saveRecruitmentDraft).not.toHaveBeenCalled()
    expect(wrapper.findComponent(PortalNoticeDialog).props('title')).toBe('提交成功')
  })

  it('saves the selected photo with the browser draft', async () => {
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    const photo = new File(['photo'], 'photo.png', { type: 'image/png' })
    const payload = { ...emptyApplication(), realName: '新生小李', photoFile: photo }

    wrapper.findComponent(ApplicationForm).vm.$emit('save-draft', payload)
    await flushPromises()

    expect(draftStorage.saveRecruitmentDraft).toHaveBeenCalledWith(payload)
    expect(draftStorage.saveRecruitmentPhotoDraft).toHaveBeenCalledWith(photo)
    expect(wrapper.findComponent(PortalNoticeDialog).props()).toEqual(expect.objectContaining({
      modelValue: true,
      title: '草稿已保存',
      message: '报名信息、证件照和 PDF 简历已保存在当前浏览器。',
    }))
  })

  it('saves the selected PDF resume with the browser draft', async () => {
    const wrapper = mount(RecruitmentPage)
    await flushPromises()
    const resume = new File(['resume'], 'resume.pdf', { type: 'application/pdf' })
    const payload = { ...emptyApplication(), realName: '新生小李', resumeFile: resume }

    wrapper.findComponent(ApplicationForm).vm.$emit('save-draft', payload)
    await flushPromises()

    expect(draftStorage.saveRecruitmentResumeDraft).toHaveBeenCalledWith(resume)
    expect(wrapper.findComponent(PortalNoticeDialog).props()).toEqual(expect.objectContaining({
      modelValue: true,
      title: '草稿已保存',
      message: '报名信息、证件照和 PDF 简历已保存在当前浏览器。',
    }))
  })

  it('hides the application form when new submissions are closed', async () => {
    recruitmentApi.getMyRecruitmentState.mockResolvedValueOnce({
      application: null,
      newApplicationsOpen: false,
      canUpdate: false,
    })

    const wrapper = mount(RecruitmentPage)
    await flushPromises()

    expect(wrapper.text()).toContain('已截止')
    expect(wrapper.get('[data-testid="recruitment-closed-notice"]').text()).toContain('不再接受新投递')
    expect(wrapper.findComponent(ApplicationForm).exists()).toBe(false)
  })

  it('keeps the form for existing applicants after the window closes', async () => {
    const wrapper = mount(RecruitmentPage)
    await flushPromises()

    expect(wrapper.text()).toContain('已截止（已报名可修改）')
    expect(wrapper.findComponent(ApplicationForm).exists()).toBe(true)
  })
})

describe('freshman activities', () => {
  it('disables signup while requesting and refreshes the signup after success', async () => {
    let resolveSignup
    activityApi.getPublishedActivities.mockResolvedValue([
      {
        activityId: 7,
        activityType: 'LECTURE',
        title: '新生宣讲会',
        activityStart: '2026-09-20',
        locationDesc: '教学楼 A101',
        status: 'PUBLISHED',
      },
    ])
    activityApi.getMyActivitySignup
      .mockResolvedValueOnce(null)
      .mockResolvedValueOnce({ activityId: 7, status: 'APPLIED' })
    activityApi.signupActivity.mockReturnValue(
      new Promise((resolve) => {
        resolveSignup = resolve
      }),
    )
    const wrapper = mount(EventsPage)
    await flushPromises()
    const button = wrapper.get('[data-testid="signup-7"]')

    await button.trigger('click')
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toContain('报名中')

    resolveSignup({ code: 200 })
    await flushPromises()

    expect(activityApi.getMyActivitySignup).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('已报名')
  })
})
