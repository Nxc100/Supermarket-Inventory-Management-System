import { test, expect } from '../../frontend/test-support/playwright'
import { login, logout } from './support/fixtures'

test('administrator changes the model with conflict input preserved while other roles cannot manage keys or usage', async ({ page }) => {
  test.setTimeout(120000)
  await login(page,'admin2')
  const originalResponse=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/system/ai/settings'&&r.request().method()==='GET')
  await page.getByRole('menuitem',{name:'模型配置与用量',exact:true}).click()
  const original=(await (await originalResponse).json()).data
  await expect(page.getByRole('heading',{name:'模型配置与用量',exact:true})).toBeVisible()
  const target=original.defaultModel==='qwen-plus'?'qwen-flash':'qwen-plus'
  await page.getByRole('combobox',{name:'默认千问模型',exact:true}).press('ArrowDown');await page.getByRole('option',{name:target,exact:true}).click()
  await page.route('**/api/v1/system/ai/settings', async route=>{ if(route.request().method()==='PUT')await route.fulfill({status:409,contentType:'application/json',body:JSON.stringify({code:'STATE_CONFLICT',message:'模型配置已由其他管理员修改，请刷新核对后保存'})});else await route.continue() })
  await page.getByRole('button',{name:'保存模型配置',exact:true}).click();await expect(page.getByRole('alert')).toContainText('已由其他管理员修改')
  await expect(page.getByRole('combobox',{name:'默认千问模型',exact:true}).locator('..').locator('..')).toContainText(target)
  await page.unroute('**/api/v1/system/ai/settings')
  const saved=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/system/ai/settings'&&r.request().method()==='PUT')
  await page.getByRole('button',{name:'保存模型配置',exact:true}).click();const response=await saved;expect(response.status()).toBe(200);expect(await response.text()).not.toContain('encryptedKey')
  await page.reload();await expect(page.getByRole('combobox',{name:'默认千问模型',exact:true}).locator('..').locator('..')).toContainText(target)
  await page.getByRole('combobox',{name:'默认千问模型',exact:true}).press('ArrowDown');await page.getByRole('option',{name:original.defaultModel,exact:true}).click();await page.getByRole('button',{name:'保存模型配置',exact:true}).click();await expect(page.getByText('模型配置已保存，新调用将使用更新后的设置',{exact:true})).toBeVisible()
  await page.getByRole('tab',{name:'调用用量',exact:true}).click();await expect(page.getByRole('heading',{name:'按模型汇总',exact:true})).toBeVisible();await expect(page.locator('main')).toContainText('未报告');await expect(page.locator('main')).toContainText('不代表阿里云账户账单')
  await logout(page);await login(page,'buyer01');await expect(page.getByRole('menuitem',{name:'模型配置与用量',exact:true})).toHaveCount(0);await page.goto('/system/ai');await expect(page.locator('main')).toContainText('访问受限')
  const denied=await page.evaluate(async()=>{const token=sessionStorage.getItem('inventory-token');return Promise.all(['/system/ai/settings','/system/ai/usage'].map(async path=>(await fetch('/api/v1'+path,{headers:{Authorization:'Bearer '+token}})).status))});expect(denied).toEqual([403,403])
})

test('suggestion page selects a model, formats safe stream events and keeps the same key after a lost response',async({page})=>{
  test.setTimeout(90000)
  await login(page,'admin2');await page.getByRole('menuitem',{name:'智能经营建议',exact:true}).click()
  await expect(page.getByRole('combobox',{name:'建议使用模型',exact:true})).toBeEnabled();await page.getByRole('combobox',{name:'建议使用模型',exact:true}).press('ArrowDown');await page.getByRole('option',{name:'qwen-flash',exact:true}).click()
  const captured=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/suggestions/preview')
  await page.getByRole('button',{name:'查看数据摘要',exact:true}).click();const preview=(await (await captured).json()).data;expect(preview.model).toBe('qwen-flash')
  const keys:string[]=[];let lose=true
  await page.route('**/api/v1/suggestions/by-request/*/stream',async route=>{
    keys.push(new URL(route.request().url()).pathname.split('/').at(-2)??'')
    if(lose){lose=false;await route.abort('failed');return}
    const content='### 数据现象\n\n**页面事件夹具，非真实模型调用**\n\n1. 人工核实库存\n2. 核对补货\n\n<script>window.injected=true</script>'
    const detail={record:{id:'1',type:'INVENTORY',operatorName:'页面事件夹具',periodStart:preview.periodStart,periodEnd:preview.periodEnd,snapshotAt:preview.snapshotAt,provider:'FIXTURE',modelName:'qwen-flash',fixture:true,status:'SUCCESS',requestedAt:preview.snapshotAt,finishedAt:preview.snapshotAt,errorMessage:null},filters:{},summary:preview.summary,content,errorCode:null,durationMs:1,recoverablePending:false,usage:{promptTokens:20,completionTokens:10,totalTokens:30,cachedTokens:null,requestId:'page-fixture'}}
    await route.fulfill({status:200,contentType:'text/event-stream',body:`event: delta\ndata: ${JSON.stringify({text:content})}\n\nevent: result\ndata: ${JSON.stringify(detail)}\n\n`})
  })
  await page.getByRole('button',{name:'已核对摘要，生成建议',exact:true}).click();await expect(page.getByText('本次结果尚未核实，请查询原结果或同键重试；不要新建另一请求。',{exact:true})).toBeVisible()
  await page.getByRole('button',{name:'同键重试本次生成',exact:true}).click();await expect(page.locator('.suggestion-markdown h3')).toHaveText('数据现象');await expect(page.locator('.suggestion-markdown strong')).toContainText('非真实模型调用');await expect(page.locator('.suggestion-markdown li')).toHaveCount(2);await expect(page.locator('.suggestion-markdown script')).toHaveCount(0);expect(keys).toEqual([preview.requestKey,preview.requestKey]);await expect(page.locator('.usage-note')).toContainText('合计 30')
})
