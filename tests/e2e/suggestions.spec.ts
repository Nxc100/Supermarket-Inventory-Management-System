import { test, expect } from '../../frontend/test-support/playwright'
import { login, logout } from './support/fixtures'

test('purchaser previews actual inventory, sees fixture history and cannot open sales suggestions', async ({page}) => {
 await login(page,'buyer01');await page.getByRole('menuitem',{name:'智能经营建议',exact:true}).click()
 await expect(page.getByRole('tab',{name:'销售经营分析建议',exact:true})).toHaveCount(0)
 await page.goto('/suggestions?type=INVENTORY&productId=1')
 await expect(page.getByRole('button',{name:'查看数据摘要',exact:true})).toBeEnabled();await expect(page.getByRole('alert')).toHaveCount(0)
 await page.getByRole('button',{name:'查看数据摘要',exact:true}).click();await expect(page.getByRole('heading',{name:'请核对本次输入摘要',exact:true})).toBeVisible()
 await expect(page.locator('.preview-summary pre')).toContainText('P0001');await expect(page.locator('.preview-summary pre')).toContainText('recentNetQty');await expect(page.locator('.preview-summary pre')).not.toContainText('unitCost')
 await expect(page.getByRole('button',{name:'已核对摘要，生成建议',exact:true})).toBeEnabled()
 await page.getByRole('tab',{name:'历史记录',exact:true}).click();await expect(page.getByRole('button',{name:'查询历史',exact:true})).toBeVisible();await expect(page.locator('main')).toContainText('测试夹具，非真实调用')
 const fixture=page.getByRole('row').filter({hasText:'fixture-not-api'}).filter({hasText:'已生成'}).first();await fixture.getByRole('button',{name:'查看建议',exact:true}).click();await expect(page.getByText('初始或测试夹具，非真实模型调用；不得作为真实生成证据',{exact:true})).toBeVisible();await expect(page.locator('.suggestion-text')).toContainText('非真实模型调用')
 await page.goto('/suggestions/2');await expect(page.getByRole('alert')).toBeVisible();await expect(page.locator('.suggestion-text')).toHaveCount(0)
 await logout(page);await login(page,'cashier01');await expect(page.getByRole('menuitem',{name:'智能经营建议',exact:true})).toHaveCount(0);await page.goto('/suggestions');await expect(page.locator('main').getByText('访问受限',{exact:true})).toBeVisible()
})

test('administrator enters a sales suggestion from the matching product statistics and previews safe aggregates',async({page})=>{
 await login(page,'admin');await page.getByRole('menuitem',{name:'销售统计',exact:true}).click();await page.getByRole('textbox',{name:'销售统计关键词',exact:true}).fill('P0001');await page.getByRole('button',{name:'查询统计',exact:true}).click()
 const row=page.getByRole('row').filter({hasText:'P0001'});await row.getByRole('button',{name:'经营建议',exact:true}).click();await expect(page.getByRole('tab',{name:'销售经营分析建议',exact:true})).toHaveAttribute('aria-selected','true')
 await page.getByRole('button',{name:'查看数据摘要',exact:true}).click();await expect(page.locator('.preview-summary pre')).toContainText('salesTotals');await expect(page.locator('.preview-summary pre')).toContainText('70.00');await expect(page.locator('.preview-summary pre')).not.toContainText('cashierId')
 const summary=JSON.parse(await page.locator('.preview-summary pre').innerText());expect(summary.salesTotals.netAmount).toBe('70.00');expect(summary.salesTotals.netQty).toBe(14)
 await page.getByRole('combobox',{name:'建议开始日期',exact:true}).fill('2026-12-30');await page.getByRole('combobox',{name:'建议开始日期',exact:true}).press('Tab');await page.getByRole('button',{name:'查看数据摘要',exact:true}).click();await expect(page.getByRole('alert').filter({hasText:'有效的分析日期区间'})).toBeVisible();await expect(page.getByRole('button',{name:'已核对摘要，生成建议',exact:true})).toHaveCount(0)
})
