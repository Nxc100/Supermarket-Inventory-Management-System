import { test,expect,type Page } from '../../frontend/test-support/playwright'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

test.beforeEach(async({page})=>{
 page.on('requestfailed',request=>console.warn('E2E_REQUEST_FAILED',request.method(),new URL(request.url()).pathname,request.failure()?.errorText))
 page.on('response',response=>{if(response.url().includes('/api/')&&response.status()>=400)console.warn('E2E_HTTP_ERROR',response.request().method(),new URL(response.url()).pathname,response.status())})
})

async function login(page:Page,name:string){await page.goto('/login');await page.getByRole('textbox',{name:'*用户名',exact:true}).fill(name);await page.getByRole('textbox',{name:'*密码',exact:true}).fill('Test123456');await page.getByRole('button',{name:'登录',exact:true}).click();await expect(page.getByRole('heading',{name:/欢迎回来/})).toBeVisible()}
async function logout(page:Page){await page.getByRole('button',{name:'退出登录',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'退出登录',exact:true}).click();await expect(page).toHaveURL(/\/login/)}

test('product option read failure keeps creation disabled until an explicit successful retry',async({page})=>{
 await login(page,'admin')
 await page.route('**/api/v1/products/category-options',route=>route.abort('failed'))
 await page.getByRole('menuitem',{name:'商品档案',exact:true}).click()
 await expect(page.getByRole('button',{name:'重试选项',exact:true})).toBeVisible()
 await expect(page.getByRole('button',{name:'新增商品',exact:true})).toBeDisabled()
 await page.unroute('**/api/v1/products/category-options')
 await page.getByRole('button',{name:'重试选项',exact:true}).click()
 await expect(page.getByRole('button',{name:'新增商品',exact:true})).toBeEnabled()
 await page.getByRole('button',{name:'新增商品',exact:true}).click()
 await expect(page.getByRole('dialog',{name:'新增商品',exact:true})).toBeVisible()
})

test('product archive, zero values, exact money, image and status through the UI',async({page})=>{
 await login(page,'admin');await page.getByRole('menuitem',{name:'商品档案'}).click()
 const code=`UI_P_${Date.now()}`,name=`页面商品${code.slice(-10)}`
 await page.getByRole('button',{name:'新增商品',exact:true}).click()
 const editor=page.getByRole('dialog',{name:'新增商品',exact:true})
 await editor.getByRole('textbox',{name:'商品编码'}).fill(code)
 await editor.getByRole('textbox',{name:'商品名称'}).fill(name)
 await editor.getByRole('combobox',{name:'商品分类'}).click();await page.getByRole('option',{name:'饮料乳品',exact:true}).click()
 await editor.getByRole('textbox',{name:'参考进价（元）'}).fill('3.50')
 await editor.getByRole('textbox',{name:'销售单价（元）'}).fill('5.50')
 await editor.getByRole('spinbutton',{name:'库存下限'}).fill('0')
 await editor.getByRole('spinbutton',{name:'临期预警（天）'}).fill('0')
 await editor.getByRole('button',{name:'保存商品',exact:true}).click();await expect(editor).toBeHidden()
 await page.getByRole('textbox',{name:'商品关键词'}).fill(name);await page.getByRole('button',{name:'查询',exact:true}).click()
 const row=page.getByRole('row').filter({hasText:code});await expect(row).toContainText('5.50');await expect(row).toContainText('0 件')
 await row.getByRole('button',{name:'编辑',exact:true}).click();const edit=page.getByRole('dialog',{name:'编辑商品',exact:true})
 await expect(edit.getByRole('spinbutton',{name:'库存下限'})).toHaveValue('0')
 await expect(edit.getByRole('spinbutton',{name:'临期预警（天）'})).toHaveValue('0')
 await edit.getByRole('textbox',{name:'销售单价（元）'}).fill('6.25');await edit.getByRole('button',{name:'保存商品',exact:true}).click();await expect(edit).toBeHidden();await expect(row).toContainText('6.25')
 await row.getByRole('button',{name:'图片',exact:true}).click();const images=page.getByRole('dialog',{name:'商品图片',exact:true})
 await images.locator('input[type=file]').setInputFiles({name:'product.png',mimeType:'image/png',buffer:readFileSync(resolve('../tests/e2e/fixtures/avatar.png'))})
 await expect(images.getByRole('img',{name:'待上传商品图片预览'})).toBeVisible();await images.getByRole('button',{name:'确认上传',exact:true}).click();await expect(images.getByRole('img',{name:'商品图片',exact:true})).toBeVisible()
 await images.getByRole('button',{name:'关闭',exact:true}).click();await page.reload();await page.getByRole('textbox',{name:'商品关键词'}).fill(name);await page.getByRole('button',{name:'查询',exact:true}).click();await expect(row.getByRole('img',{name:'商品图片',exact:true})).toBeVisible()
 await row.getByRole('button',{name:'停用',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'停用商品',exact:true}).click();await expect(row).toContainText('已停用')
 await row.getByRole('button',{name:'详情',exact:true}).click();await expect(page.getByRole('dialog',{name:'商品详情'})).toContainText('6.25 元');await page.getByRole('dialog',{name:'商品详情'}).getByRole('button',{name:'关闭此对话框'}).click()
 await row.getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'删除商品',exact:true}).click();await expect(page.getByText('暂无符合条件的商品')).toBeVisible()
})

test('purchaser maintains shared supplier while only administrator can delete',async({page})=>{
 await login(page,'buyer01');await expect(page.getByRole('menuitem',{name:'商品档案'})).toHaveCount(0)
 await page.getByRole('menuitem',{name:'供应商管理'}).click();const name=`页面供应商${Date.now()}`
 await page.getByRole('button',{name:'新增供应商',exact:true}).click();const editor=page.getByRole('dialog',{name:'新增供应商',exact:true})
 await editor.getByRole('textbox',{name:'供应商名称'}).fill(name);await editor.getByRole('textbox',{name:'联系人',exact:false}).fill('页面联系人');await editor.getByRole('textbox',{name:'联系电话'}).fill('bad-phone')
 await editor.getByRole('button',{name:'保存供应商',exact:true}).click();await expect(editor.getByText('电话为6～20位数字、空格或短横线')).toBeVisible()
 await editor.getByRole('textbox',{name:'联系电话'}).fill('0710-1234567');await editor.getByRole('button',{name:'保存供应商',exact:true}).click();await expect(editor).toBeHidden()
 await page.getByRole('textbox',{name:'供应商关键词'}).fill(name);await page.getByRole('button',{name:'查询',exact:true}).click();const row=page.getByRole('row').filter({hasText:name});await expect(row).toBeVisible();await expect(row.getByRole('button',{name:'删除',exact:true})).toHaveCount(0)
 await row.getByRole('button',{name:'停用',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'停用供应商',exact:true}).click();await expect(row).toContainText('已停用')
 await logout(page);await login(page,'admin');await page.getByRole('menuitem',{name:'供应商管理'}).click();await page.getByRole('textbox',{name:'供应商关键词'}).fill(name);await page.getByRole('button',{name:'查询',exact:true}).click();await expect(row).toContainText('页面联系人')
 await row.getByRole('button',{name:'编辑',exact:true}).click();const edit=page.getByRole('dialog',{name:'编辑供应商'});await edit.getByRole('textbox',{name:'联系人',exact:false}).fill('管理员接续');await edit.getByRole('button',{name:'保存供应商',exact:true}).click();await expect(edit).toBeHidden();await expect(row).toContainText('管理员接续')
 await row.getByRole('button',{name:'删除',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'删除供应商',exact:true}).click();await expect(page.getByText('暂无符合条件的供应商')).toBeVisible()
})

test('cashier stock query is read only and shows frozen saleable quantities without costs',async({page})=>{
 await login(page,'cashier02');await page.getByRole('menuitem',{name:'库存查询',exact:true}).click()
 await page.getByRole('textbox',{name:'库存关键词'}).fill('盘点中文具笔');await page.getByRole('button',{name:'查询',exact:true}).click()
 const row=page.getByRole('row').filter({hasText:'P0010'});await expect(row).toContainText('20 件');await expect(row).toContainText('盘点中暂不可销售')
 await expect(page.getByText('参考进价',{exact:true})).toHaveCount(0);await expect(page.getByRole('button',{name:'编辑',exact:true})).toHaveCount(0);await expect(page.getByRole('button',{name:'删除',exact:true})).toHaveCount(0)
 await page.getByRole('textbox',{name:'库存关键词'}).fill('已停用历史商品');await page.getByRole('button',{name:'查询',exact:true}).click();await expect(page.getByRole('row').filter({hasText:'P0013'})).toContainText('0 件');await expect(page.getByRole('row').filter({hasText:'P0013'})).toContainText('暂无可售库存')
 await page.goto('/catalog/products');await expect(page.getByRole('main').getByText('访问受限',{exact:true})).toBeVisible()
})
