import { test, expect } from '../../frontend/test-support/playwright'
import { login, logout } from './support/fixtures'

test('role dashboards show authorized metrics and preserve their exact list scope', async ({page}) => {
 test.setTimeout(90000)
 await login(page,'admin')
 const pending=page.locator('.dashboard-card[data-code="pendingPurchases"]');await expect(pending).toBeVisible()
 const pendingCount=Number((await pending.locator('.metric-value').innerText()).replace(/\D/g,''));await pending.click()
 await expect(page.locator('.el-select').filter({has:page.getByRole('combobox',{name:'采购状态',exact:true})})).toContainText('待审核');await expect(page.locator('.table-footer')).toContainText(`共 ${pendingCount} 张采购单`)
 await page.getByRole('menuitem',{name:'工作台',exact:true}).click();await page.locator('.dashboard-card[data-code="pendingStocktakes"]').click()
 await expect(page.locator('.el-select').filter({has:page.getByRole('combobox',{name:'盘点状态',exact:true})})).toContainText('待审核')
 await page.getByRole('menuitem',{name:'工作台',exact:true}).click();await page.locator('.dashboard-card[data-code="todaySales"]').click()
 await expect(page.locator('.el-select').filter({has:page.getByRole('combobox',{name:'销售状态筛选',exact:true})})).toContainText('已完成');await expect(page.locator('.sales-totals')).toContainText('全店')
 await logout(page);await login(page,'buyer01');await expect(page.locator('.dashboard-card[data-code="draftPurchases"]')).toBeVisible()
 await expect(page.locator('.dashboard-card[data-code="todaySales"]')).toHaveCount(0);await expect(page.locator('.dashboard-shortcuts')).not.toContainText('销售经营建议')
 const draft=page.locator('.dashboard-card[data-code="draftPurchases"]');const drafts=Number((await draft.locator('.metric-value').innerText()).replace(/\D/g,''));await draft.click();await expect(page.locator('.el-select').filter({has:page.getByRole('combobox',{name:'采购状态',exact:true})})).toContainText('草稿');await expect(page.locator('.table-footer')).toContainText(`共 ${drafts} 张采购单`)
 await page.getByRole('menuitem',{name:'工作台',exact:true}).click();await page.locator('.dashboard-card[data-code="EXPIRED"]').click();await expect(page.getByRole('tab',{name:'过期商品',exact:true})).toHaveAttribute('aria-selected','true')
 await logout(page);await login(page,'cashier01');await expect(page.locator('.dashboard-card')).toHaveCount(2);await expect(page.locator('.dashboard-shortcuts')).not.toContainText('采购')
 await page.locator('.dashboard-card[data-code="todaySales"]').click();await expect(page.locator('.el-select').filter({has:page.getByRole('combobox',{name:'销售状态筛选',exact:true})})).toContainText('已完成');await expect(page.locator('.sales-totals')).toContainText('本人')
})

test('dashboard read failure clears numbers and allows explicit retry',async({page})=>{
 await login(page,'cashier02');await expect(page.locator('.dashboard-card')).toHaveCount(2)
 await page.route('**/api/v1/dashboard',route=>route.abort('failed'));await page.getByRole('button',{name:'刷新工作台',exact:true}).click();await expect(page.getByRole('alert')).toBeVisible();await expect(page.locator('.dashboard-card')).toHaveCount(0)
 await page.unroute('**/api/v1/dashboard');await page.getByRole('button',{name:'重试工作台',exact:true}).click();await expect(page.locator('.dashboard-card')).toHaveCount(2);await expect(page.getByRole('alert')).toHaveCount(0)
})
