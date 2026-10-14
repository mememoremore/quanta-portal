import { beforeEach, describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ClothingOrdersView from '../index.vue'
import { useUserStore } from '@/stores/user'
import { resetMockClothingOrders } from '@/mock/data/clothing-orders'
import { setToken } from '@/utils/token'

describe('clothing orders view', () => {
  beforeEach(() => { resetMockClothingOrders(); setToken('mock-token-product-manager') })

  it('loads the Element Plus message box styles used by receipt confirmation', () => {
    const mainSource = readFileSync(join(process.cwd(), 'src/main.js'), 'utf8')
    expect(mainSource).toContain("element-plus/es/components/message-box/style/css")
  })

  it('shows filters, payment proof, and management confirmation', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useUserStore()
    store.permissions = ['qt:order:list', 'qt:order:approve']
    store.roles = ['qt_mgmt']
    const wrapper = mount(ClothingOrdersView, { global: { plugins: [pinia, ElementPlus] } })
    await flushPromises()
    expect(wrapper.text()).toContain('所有状态')
    expect(wrapper.text()).toContain('待确认(已传图)')
    expect(wrapper.text()).toContain('确认收款')
    expect(wrapper.text()).toContain('查看凭证')
  })
})
