import { test, expect, type Page } from '../../frontend/test-support/playwright'
import { login, logout, confirm, stockedProduct } from './support/fixtures'

function cart(page:Page){return page.locator('section.panel').filter({has:page.getByRole('heading',{name:'当前开单',exact:true})})}
function searchPanel(page:Page){return page.locator('section.panel').filter({has:page.getByRole('heading',{name:'查找销售商品',exact:true})})}
async function add(page:Page,code:string){await page.getByRole('menuitem',{name:'前台开单',exact:true}).click();const panel=searchPanel(page);await panel.getByRole('textbox',{name:'销售商品关键词',exact:true}).fill(code);await panel.getByRole('button',{name:'搜索商品',exact:true}).click();await panel.getByRole('row').filter({hasText:code}).getByRole('button',{name:'加入开单',exact:true}).click()}
async function correct(page:Page,url:string){await page.goto(url);await page.getByRole('button',{name:'整单纠错',exact:true}).click();const dialog=page.getByRole('dialog',{name:'当天整单纠错',exact:true});await dialog.getByText('我确认货物实际仍在店内或已完整收回',{exact:true}).click();await dialog.getByRole('textbox',{name:'整单纠错原因',exact:true}).fill('页面验证整单误录，货物完整仍在测试门店');await dialog.getByRole('button',{name:'确认整单纠错',exact:true}).click();await expect(dialog).toBeHidden();await expect(page.getByText('已纠错',{exact:true}).first()).toBeVisible()}
test.beforeEach(async({page})=>{page.on('pageerror',error=>console.warn('SALES_UI_ERROR',error.message));page.on('response',response=>{if(response.url().includes('/api/')&&response.status()>=400)console.warn('SALES_HTTP_ERROR',response.request().method(),new URL(response.url()).pathname,response.status())})})

test('cashier cart, cash change, private records and administrator whole-sale correction',async({page})=>{
 test.setTimeout(180000)
 const product=await stockedProduct(page);await login(page,'cashier01');await add(page,product.code)
 await searchPanel(page).getByRole('row').filter({hasText:product.code}).getByRole('button',{name:'加入开单',exact:true}).click();await expect(cart(page).getByRole('spinbutton',{name:`${product.name}销售数量`,exact:true})).toHaveValue('2')
 await cart(page).getByRole('button',{name:'清空开单',exact:true}).click();await page.getByRole('dialog',{name:'清空开单',exact:true}).getByRole('button',{name:'继续开单',exact:true}).click();await expect(cart(page)).toContainText('10.00')
 const quantity=cart(page).getByRole('spinbutton',{name:`${product.name}销售数量`,exact:true});await quantity.fill('3');await quantity.press('Tab');await expect(cart(page).locator('.table-footer')).toContainText('15.00')
 await cart(page).getByRole('button',{name:'移除',exact:true}).click();await expect(cart(page).getByRole('button',{name:'结算',exact:true})).toBeDisabled()
 const barcodeSearch=searchPanel(page).getByRole('textbox',{name:'销售商品关键词',exact:true});await barcodeSearch.fill(product.barcode);await barcodeSearch.press('Enter');await expect(quantity).toHaveValue('1')
 await cart(page).getByRole('button',{name:'清空开单',exact:true}).click();await page.getByRole('dialog',{name:'清空开单',exact:true}).getByRole('button',{name:'清空开单',exact:true}).click();await expect(cart(page).getByRole('button',{name:'结算',exact:true})).toBeDisabled()
 await barcodeSearch.press('Enter');await expect(quantity).toHaveValue('1');await quantity.fill('2');await quantity.press('Tab')
 await page.getByRole('menuitem',{name:'销售记录',exact:true}).click();await page.getByRole('dialog',{name:'离开页面',exact:true}).getByRole('button',{name:'继续编辑',exact:true}).click();await expect(cart(page)).toContainText(product.code)
 await cart(page).getByRole('button',{name:'结算',exact:true}).click();const payment=page.getByRole('dialog',{name:'确认收款',exact:true});await payment.getByRole('textbox',{name:'现金实收金额',exact:true}).fill('9.99');await payment.getByRole('button',{name:'确认收款并完成销售',exact:true}).click();await expect(page.getByText('实收金额必须不少于应收金额，且最多两位小数',{exact:true})).toBeVisible()
 await payment.getByRole('textbox',{name:'现金实收金额',exact:true}).fill('20.00');await payment.getByRole('button',{name:'确认收款并完成销售',exact:true}).click();await expect(payment).toBeHidden();await expect(page.getByRole('cell',{name:'¥ 10.00',exact:true})).toHaveCount(2)
 const orderNo=await page.getByRole('heading',{name:/^XS/}).innerText();await page.getByRole('link',{name:'查看销售详情',exact:true}).click();await expect(page.getByRole('heading',{name:'销售单详情',exact:true})).toBeVisible();const detailUrl=page.url();await expect(page.getByRole('button',{name:'整单纠错',exact:true})).toHaveCount(0)
 await logout(page);await login(page,'cashier02');await page.goto(detailUrl);await expect(page.getByRole('alert').filter({hasText:'记录不存在或无权访问'})).toBeVisible()
 await logout(page);await login(page,'admin');await correct(page,detailUrl);await expect(page.getByRole('button',{name:'整单纠错',exact:true})).toHaveCount(0)
 await page.getByRole('menuitem',{name:'销售记录',exact:true}).click();await page.getByRole('textbox',{name:'销售记录关键词',exact:true}).fill(orderNo);await page.locator('form.query-bar .el-select').filter({has:page.getByRole('combobox',{name:'收银员筛选',exact:true})}).click();await page.getByRole('option',{name:'收银王五',exact:true}).click();await page.getByRole('button',{name:'查询销售',exact:true}).click();await expect(page.getByRole('row').filter({hasText:orderNo})).toContainText('已纠错');await expect(page.locator('.sales-totals')).toContainText('¥ 0.00')
 await page.getByRole('menuitem',{name:'库存查询',exact:true}).click();await page.getByRole('textbox',{name:'库存关键词',exact:true}).fill(product.code);await page.getByRole('button',{name:'查询',exact:true}).click();await expect(page.getByRole('row').filter({hasText:product.code})).toContainText('5 件')
})

test('price changed after checkout is shown and requires explicit adoption before payment',async({page})=>{
 test.setTimeout(180000)
 const product=await stockedProduct(page);await login(page,'cashier01');await add(page,product.code);await cart(page).getByRole('button',{name:'结算',exact:true}).click();const payment=page.getByRole('dialog',{name:'确认收款',exact:true});await payment.getByRole('textbox',{name:'现金实收金额',exact:true}).fill('100.00')
 const manager=await page.context().newPage();await login(manager,'admin');await manager.getByRole('menuitem',{name:'商品档案',exact:true}).click();await manager.getByRole('textbox',{name:'商品关键词',exact:true}).fill(product.code);await manager.getByRole('button',{name:'查询',exact:true}).click();await manager.getByRole('row').filter({hasText:product.code}).getByRole('button',{name:'编辑',exact:true}).click();const edit=manager.getByRole('dialog',{name:'编辑商品',exact:true});await edit.getByRole('textbox',{name:'*销售单价（元）',exact:true}).fill('6.25');await edit.getByRole('button',{name:'保存商品',exact:true}).click();await expect(edit).toBeHidden()
 await page.bringToFront();const rejected=page.waitForResponse(response=>response.url().endsWith('/sales/complete'));await payment.getByRole('button',{name:'确认收款并完成销售',exact:true}).click();expect((await rejected).status()).toBe(409);await expect(payment.getByRole('row').filter({hasText:product.name})).toContainText('6.25');await expect(payment.getByRole('button',{name:'确认收款并完成销售',exact:true})).toBeDisabled()
 await payment.getByRole('button',{name:'已核对，采用当前售价并重新校验',exact:true}).click();await expect(payment.getByRole('button',{name:'确认收款并完成销售',exact:true})).toBeEnabled();await payment.getByRole('button',{name:'确认收款并完成销售',exact:true}).click();await expect(payment).toBeHidden();await expect(page.getByRole('cell',{name:'¥ 6.25',exact:true})).toBeVisible();await expect(page.getByRole('cell',{name:'¥ 93.75',exact:true})).toBeVisible()
 const path=await page.getByRole('link',{name:'查看销售详情',exact:true}).getAttribute('href');if(!path)throw new Error('Missing sale detail link');await correct(manager,new URL(path,page.url()).href);await manager.close()
})

test('lost successful response recovers across reload through result lookup without a second write',async({page})=>{
 test.setTimeout(180000)
 const product=await stockedProduct(page);await login(page,'cashier01');await add(page,product.code)
 let confirmations=0
 await page.route('**/api/v1/sales/complete',async route=>{confirmations++;const response=await route.fetch();expect(response.status()).toBe(200);await route.abort('failed')})
 await page.route('**/api/v1/sales/by-request/*',route=>route.abort('failed'))
 await cart(page).getByRole('button',{name:'结算',exact:true}).click();const payment=page.getByRole('dialog',{name:'确认收款',exact:true});await payment.getByText('模拟电子支付',{exact:true}).click();await expect(payment).toContainText('模拟确认，不发生真实扣款');await payment.getByRole('button',{name:'确认收款并完成销售',exact:true}).click();await expect(page.getByRole('heading',{name:'本次收款结果待核实',exact:true})).toBeVisible();expect(confirmations).toBe(1)
 await page.reload();await expect(page.getByRole('heading',{name:'本次收款结果待核实',exact:true})).toBeVisible();await page.unroute('**/api/v1/sales/by-request/*');await page.getByRole('button',{name:'查询本次结果',exact:true}).click();await expect(page.getByRole('link',{name:'查看销售详情',exact:true})).toBeVisible();expect(confirmations).toBe(1);await expect(page.getByRole('cell',{name:'¥ 5.00',exact:true})).toHaveCount(2)
 const path=await page.getByRole('link',{name:'查看销售详情',exact:true}).getAttribute('href');if(!path)throw new Error('Missing sale detail link');await logout(page);await login(page,'admin');await correct(page,new URL(path,page.url()).href)
})
