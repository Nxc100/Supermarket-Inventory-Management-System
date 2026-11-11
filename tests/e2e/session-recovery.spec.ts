import { test, expect } from '../../frontend/test-support/playwright'
import { login } from './support/fixtures'

test('a failed identity read preserves the session, hides business data and explicitly retries the original route', async ({page}) => {
  await login(page, 'cashier02')
  await page.getByRole('menuitem', {name:'销售记录',exact:true}).click()
  await expect(page).toHaveURL(/\/sales$/)
  await page.route('**/api/v1/auth/me', route => route.abort('failed'))
  await page.reload()
  await expect(page.getByRole('heading',{name:'登录身份暂未核实',exact:true})).toBeVisible()
  await expect(page.getByRole('menubar')).toHaveCount(0)
  await page.getByRole('button',{name:'重试身份核验',exact:true}).click()
  await expect(page.getByText('登录身份仍未核实，请检查后端与数据库连接后再试。',{exact:true})).toBeVisible()
  await page.unroute('**/api/v1/auth/me')
  await page.getByRole('button',{name:'重试身份核验',exact:true}).click()
  await expect(page).toHaveURL(/\/sales$/)
  await expect(page.getByRole('heading',{name:'销售记录',exact:true})).toBeVisible()
  await expect(page.getByRole('textbox',{name:'*密码',exact:true})).toHaveCount(0)
})
