import { test, expect, type Page } from '../../frontend/test-support/playwright'
import { login, logout, confirm, stockedProduct } from './support/fixtures'

async function start(page: Page, code: string) {
 await page.getByRole('menuitem', { name: '库存盘点', exact: true }).click()
 await page.getByRole('button', { name: '新建盘点', exact: true }).click()
 await page.getByRole('textbox', { name: '盘点选品关键词', exact: true }).fill(code)
 await page.getByRole('button', { name: '搜索盘点商品', exact: true }).click()
 await page.getByRole('row').filter({ hasText: code }).getByRole('button', { name: '选取商品', exact: true }).click()
 await page.getByRole('button', { name: '开始盘点', exact: true }).click(); await confirm(page, '开始盘点')
 await expect(page.getByRole('heading', { name: '盘点详情', exact: true })).toBeVisible()
 await expect(page.getByRole('spinbutton', { name: '第1批实盘数', exact: true })).toHaveValue('')
 return page.url()
}
async function count(page: Page, qty: string) {
 await page.getByRole('spinbutton', { name: '第1批实盘数', exact: true }).fill(qty)
 await page.getByRole('spinbutton', { name: '第1批实盘数', exact: true }).press('Tab')
 await page.locator('.el-select').filter({has: page.getByRole('combobox', { name: '第1批差异原因', exact: true })}).click()
 await page.getByRole('option', { name: '清点差异', exact: true }).click()
 await page.getByRole('textbox', { name: '第1批原因说明', exact: true }).fill('页面测试逐批清点差异，核实实物数量')
 await page.getByRole('button', { name: '暂存实盘', exact: true }).click()
 await expect(page.getByRole('button', { name: '提交盘点审核', exact: true })).toBeEnabled()
}
async function prompt(page: Page, label: string) {
 await page.getByRole('button', { name: label, exact: true }).click()
 const dialog = page.getByRole('dialog', { name: label, exact: true })
 await dialog.getByRole('textbox').fill('页面验证后核实清点，保留操作历史')
 await dialog.getByRole('button', { name: label, exact: true }).click()
 await expect(dialog).toBeHidden()
}
test('stocktake freezes checkout, requires actual counts, preserves ownership and adjusts with review', async ({ page }) => {
 test.setTimeout(180000)
 const product = await stockedProduct(page); await login(page, 'buyer01'); const url = await start(page, product.code)
 await expect(page.getByRole('button', { name: '提交盘点审核', exact: true })).toBeDisabled()
 const cashier = await page.context().newPage(); await login(cashier, 'cashier01')
 await cashier.getByRole('menuitem', { name: '前台开单', exact: true }).click(); await cashier.getByRole('textbox', { name: '销售商品关键词', exact: true }).fill(product.code)
 await cashier.getByRole('button', { name: '搜索商品', exact: true }).click(); await expect(cashier.getByRole('button', { name: '盘点中暂不可销售', exact: true })).toBeDisabled()
 await expect(cashier.getByRole('row').filter({ hasText: product.code })).toContainText('5 件'); await logout(cashier); await cashier.close()
 await page.getByRole('spinbutton', { name: '第1批实盘数', exact: true }).fill('4'); await page.getByRole('spinbutton', { name: '第1批实盘数', exact: true }).press('Tab')
 await page.getByRole('button', { name: '暂存实盘', exact: true }).click(); await expect(page.getByRole('alert').filter({ hasText: '差异须选择原因' })).toBeVisible()
 await page.getByRole('menuitem', { name: '库存盘点', exact: true }).click(); await page.getByRole('dialog', { name: '离开页面', exact: true }).getByRole('button', { name: '继续编辑', exact: true }).click()
 await count(page, '4'); await page.getByRole('button', { name: '提交盘点审核', exact: true }).click(); await confirm(page, '提交盘点审核'); await expect(page.getByText('待审核', { exact: true })).toBeVisible()
 await logout(page); await login(page, 'buyer02'); await page.goto(url); await expect(page.getByRole('alert').filter({ hasText: '记录不存在或无权访问' })).toBeVisible()
 await logout(page); await login(page, 'admin'); await page.goto(url); await page.getByRole('button', { name: '审核通过并调整库存', exact: true }).click(); await confirm(page, '审核通过并调整库存')
 await expect(page.getByText('已完成', { exact: true })).toBeVisible(); await expect(page.getByRole('button', { name: '审核通过并调整库存', exact: true })).toHaveCount(0)
 await page.getByRole('menuitem', { name: '库存查询', exact: true }).click(); await page.getByRole('textbox', { name: '库存关键词', exact: true }).fill(product.code); await page.getByRole('button', { name: '查询', exact: true }).click()
 await expect(page.getByRole('row').filter({ hasText: product.code })).toContainText('4 件')
 await start(page, product.code); await count(page, '5'); await page.getByRole('button', { name: '提交盘点审核', exact: true }).click(); await confirm(page, '提交盘点审核'); await page.getByRole('button', { name: '审核通过并调整库存', exact: true }).click(); await confirm(page, '审核通过并调整库存'); await expect(page.getByText('已完成', { exact: true })).toBeVisible()
})

test('zero is a valid actual count; rejection and withdrawal retain freeze while close releases without adjustment', async ({ page }) => {
 test.setTimeout(180000)
 const product = await stockedProduct(page); await login(page, 'buyer01'); const url = await start(page, product.code)
 await count(page, '0'); await page.reload(); await expect(page.getByRole('spinbutton', { name: '第1批实盘数', exact: true })).toHaveValue('0')
 await page.getByRole('button', { name: '提交盘点审核', exact: true }).click(); await confirm(page, '提交盘点审核'); await expect(page.getByRole('button', { name: '关闭盘点', exact: true })).toBeDisabled()
 await logout(page); await login(page, 'admin'); await page.goto(url); await prompt(page, '驳回盘点'); await expect(page.getByText('已驳回', { exact: true })).toBeVisible(); await expect(page.getByRole('alert').filter({ hasText: '商品仍处于盘点冻结' })).toBeVisible()
 await logout(page); await login(page, 'buyer01'); await page.goto(url); await count(page, '0'); await page.getByRole('button', { name: '提交盘点审核', exact: true }).click(); await confirm(page, '提交盘点审核'); await page.getByRole('button', { name: '撤回盘点', exact: true }).click(); await confirm(page, '撤回盘点')
 await expect(page.getByRole('alert').filter({ hasText: '商品仍处于盘点冻结' })).toBeVisible(); await prompt(page, '关闭盘点'); await expect(page.getByText('已关闭', { exact: true })).toBeVisible()
 await page.getByRole('menuitem', { name: '库存查询', exact: true }).click(); await page.getByRole('textbox', { name: '库存关键词', exact: true }).fill(product.code); await page.getByRole('button', { name: '查询', exact: true }).click(); await expect(page.getByRole('row').filter({ hasText: product.code })).toContainText('5 件')
})
