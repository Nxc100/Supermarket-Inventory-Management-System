import { test, expect, type Page } from '../../frontend/test-support/playwright'
import { login, logout } from './support/fixtures'
async function saveDefaults(page:Page){
 const saved=page.waitForResponse(r=>r.request().method()==='PUT'&&r.url().endsWith('/api/v1/system/defaults'))
 await page.getByRole('button',{name:'保存默认参数',exact:true}).click();expect((await saved).status()).toBe(200)
 await expect(page.getByRole('button',{name:'保存默认参数',exact:true})).toBeDisabled()
 await expect(page.getByRole('button',{name:'保存默认参数',exact:true})).not.toHaveClass(/is-loading/)
}

test('defaults preserve failed input, initialize new product forms and leave existing products unchanged',async({page})=>{
 test.setTimeout(90000)
 await login(page,'admin');await page.getByRole('menuitem',{name:'业务参数',exact:true}).click()
 const lower=page.getByRole('spinbutton',{name:'默认库存下限',exact:true}),near=page.getByRole('spinbutton',{name:'默认临期天数',exact:true})
 await expect(lower).toBeVisible();const originalLower=await lower.inputValue(),originalNear=await near.inputValue();const nextLower=originalLower==='0'?'9':'0',nextNear=originalNear==='0'?'3':'0'
 try{
  await lower.fill(nextLower);await near.fill(nextNear);await near.press('Tab');await page.getByRole('menuitem',{name:'操作记录',exact:true}).click();await page.getByRole('dialog',{name:'离开页面',exact:true}).getByRole('button',{name:'继续编辑',exact:true}).click();await expect(lower).toHaveValue(nextLower)
  await page.route('**/api/v1/system/defaults',route=>route.request().method()==='PUT'?route.abort('failed'):route.continue());await page.getByRole('button',{name:'保存默认参数',exact:true}).click();await expect(page.getByRole('alert')).toBeVisible();await expect(lower).toHaveValue(nextLower);await page.unroute('**/api/v1/system/defaults');await saveDefaults(page)
  await page.getByRole('menuitem',{name:'商品档案',exact:true}).click();await page.getByRole('button',{name:'新增商品',exact:true}).click();const create=page.getByRole('dialog',{name:'新增商品',exact:true});await expect(create.getByRole('spinbutton',{name:'*库存下限',exact:true})).toHaveValue(nextLower);await expect(create.getByRole('spinbutton',{name:'*临期预警（天）',exact:true})).toHaveValue(nextNear);await create.getByRole('button',{name:'取消',exact:true}).click()
  await page.getByRole('textbox',{name:'商品关键词',exact:true}).fill('P0001');await page.getByRole('button',{name:'查询',exact:true}).click();await page.getByRole('row').filter({hasText:'P0001'}).getByRole('button',{name:'编辑',exact:true}).click();const editor=page.getByRole('dialog',{name:'编辑商品',exact:true});await expect(editor.getByRole('spinbutton',{name:'*库存下限',exact:true})).toHaveValue('5');await editor.getByRole('button',{name:'取消',exact:true}).click()
 }finally{
  await page.unroute('**/api/v1/system/defaults');await page.goto('/system/defaults');await expect(lower).toBeVisible();await lower.fill(originalLower);await near.fill(originalNear);await near.press('Tab');if(await page.getByRole('button',{name:'保存默认参数',exact:true}).isEnabled())await saveDefaults(page)
 }
})

test('operation records filter and show snapshots while ordinary roles cannot open system pages',async({page})=>{
 await login(page,'admin');await page.getByRole('menuitem',{name:'操作记录',exact:true}).click();await expect(page.getByRole('button',{name:'查看记录',exact:true}).first()).toBeVisible();await page.getByRole('textbox',{name:'操作对象关键词',exact:true}).fill('inventory.defaults');await page.getByRole('button',{name:'查询操作记录',exact:true}).click()
 const row=page.getByRole('row').filter({hasText:'CONFIG_UPDATE'}).first();await expect(row).toBeVisible();await row.getByRole('button',{name:'查看记录',exact:true}).click();const detail=page.getByRole('dialog',{name:'操作记录详情',exact:true});await expect(detail.getByRole('heading',{name:'变更前',exact:true})).toBeVisible();await expect(detail.locator('pre').last()).toContainText('stockLowerLimit');await detail.getByRole('button',{name:'关闭此对话框',exact:true}).click()
 await page.getByRole('textbox',{name:'操作对象关键词',exact:true}).fill('no_log_'+Date.now());await page.getByRole('button',{name:'查询操作记录',exact:true}).click();await expect(page.getByText('暂无符合条件的操作记录',{exact:true})).toBeVisible()
 for(const account of ['buyer01','cashier01']){await logout(page);await login(page,account);await expect(page.getByRole('menuitem',{name:'业务参数',exact:true})).toHaveCount(0);await expect(page.getByRole('menuitem',{name:'操作记录',exact:true})).toHaveCount(0);await page.goto('/system/logs');await expect(page.locator('main').getByText('访问受限',{exact:true})).toBeVisible()}
})
