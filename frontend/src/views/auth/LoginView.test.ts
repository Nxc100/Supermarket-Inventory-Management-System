import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import LoginView from './LoginView.vue'
import * as api from '../../api/auth'

vi.mock('../../api/auth', () => ({ login: vi.fn() }))
const replace = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ replace }),
  useRoute: () => ({ query: {} }),
}))
let wrapper: VueWrapper | undefined
beforeEach(() => {
  sessionStorage.clear()
  vi.clearAllMocks()
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
})
function render() {
  wrapper = mount(LoginView, { global: { plugins: [createPinia(), ElementPlus] } })
  return wrapper
}

describe('login interaction', () => {
  it('keeps username, clears password and recovers submit after server failure', async () => {
    vi.mocked(api.login).mockRejectedValue(new Error('账号或密码错误'))
    const view = render()
    await view.get('input[name=username]').setValue('cashier01')
    await view.get('input[name=password]').setValue('incorrect')
    await view.get('form').trigger('submit')
    await flushPromises()
    expect((view.get('input[name=username]').element as HTMLInputElement).value).toBe('cashier01')
    expect((view.get('input[name=password]').element as HTMLInputElement).value).toBe('')
    expect(view.text()).toContain('账号或密码错误')
    expect(view.get('button[type=submit]').attributes('disabled')).toBeUndefined()
  })
  it('does not issue login requests for missing inputs', async () => {
    const view = render()
    await view.get('form').trigger('submit')
    expect(api.login).not.toHaveBeenCalled()
    expect(view.text()).toContain('请输入用户名与密码')
  })
  it('blocks repeated submit while the initial request remains pending', async () => {
    let rejectRequest: (reason: Error) => void = () => {}
    vi.mocked(api.login).mockImplementation(
      () =>
        new Promise((_resolve, reject) => {
          rejectRequest = reject
        }),
    )
    const view = render()
    await view.get('input[name=username]').setValue('admin')
    await view.get('input[name=password]').setValue('Test123456')
    await view.get('form').trigger('submit')
    await view.get('form').trigger('submit')
    expect(api.login).toHaveBeenCalledTimes(1)
    expect(view.get('button[type=submit]').attributes('disabled')).toBeDefined()
    rejectRequest(new Error('network failure'))
    await flushPromises()
    expect(view.get('button[type=submit]').attributes('disabled')).toBeUndefined()
  })
})
