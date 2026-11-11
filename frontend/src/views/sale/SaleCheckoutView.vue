<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Decimal from 'decimal.js'
import { ApiError } from '../../api/http'
import { quoteSale, saleContext } from '../../api/sales'
import { useAuth } from '../../stores/auth'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import { useSaleCheckout } from '../../composables/useSaleCheckout'
import { showError } from '../../utils/feedback'
import type { SaleProduct } from '../../types/catalog'
import type {
  PaymentMethod,
  SaleCartLine,
  SaleContext,
  SaleLineInput,
  SaleQuote,
} from '../../types/sale'
import SaleProductSearch from './SaleProductSearch.vue'
import SaleCart from './SaleCart.vue'
import SalePaymentDialog from './SalePaymentDialog.vue'
import SaleReceiptPanel from './SaleReceiptPanel.vue'

const auth = useAuth(),
  user = auth.user
if (!user) throw new Error('当前登录信息尚未就绪')
const cart = ref<SaleCartLine[]>([]),
  context = ref<SaleContext | null>(null),
  contextError = ref(''),
  contextLoading = ref(false)
const quote = ref<SaleQuote | null>(null),
  quoteBusy = ref(false),
  paymentOpen = ref(false)
const productSearch = ref<InstanceType<typeof SaleProductSearch>>()
const { pending, receipt, busy, uncertain, problem, submit, recover, retry, continueSelling } =
  useSaleCheckout(user.id, () => {
    cart.value = []
    paymentOpen.value = false
    quote.value = null
    ElMessage.success('已确认本次销售结果')
  })
const locked = computed(() => busy.value || quoteBusy.value || pending.value !== null)
const dirty = computed(() => cart.value.length > 0 && pending.value === null)
useDirtyGuard(dirty)
async function loadContext() {
  contextLoading.value = true
  contextError.value = ''
  try {
    context.value = await saleContext()
    return true
  } catch (reason) {
    contextError.value = reason instanceof Error ? reason.message : '开单设置加载失败'
    return false
  } finally {
    contextLoading.value = false
  }
}
function addProduct(product: SaleProduct) {
  if (locked.value) return
  if (product.frozen || product.saleableQty < 1) {
    ElMessage.warning(product.frozen ? '该商品盘点中暂不可销售' : '该商品当前没有可售库存')
    return
  }
  const existing = cart.value.find((item) => item.id === product.id)
  if (existing) {
    const next = (existing.quantity ?? 0) + 1
    if (next > product.saleableQty || next > 999999) {
      ElMessage.warning('数量超过页面已知可售量，请刷新核实')
      return
    }
    existing.quantity = next
    existing.saleableQty = product.saleableQty
    if (existing.salePrice !== product.salePrice)
      ElMessage.warning('该商品售价已有变化，结算时请核对新旧售价')
  } else {
    if (cart.value.length >= 100) {
      ElMessage.warning('每张销售单最多100种商品')
      return
    }
    cart.value.push({ ...product, quantity: 1 })
  }
  quote.value = null
}
function quantity(id: string, value: number | null) {
  if (locked.value) return
  const row = cart.value.find((item) => item.id === id)
  if (row) {
    row.quantity = value
    quote.value = null
  }
}
function remove(id: string) {
  if (!locked.value) {
    cart.value = cart.value.filter((item) => item.id !== id)
    quote.value = null
  }
}
async function clear() {
  if (locked.value) return
  try {
    await ElMessageBox.confirm('确认清空尚未完成收款的开单？库存不会变化。', '清空开单', {
      type: 'warning',
      confirmButtonText: '清空开单',
      cancelButtonText: '继续开单',
    })
    cart.value = []
    quote.value = null
  } catch (reason) {
    showError(reason)
  }
}
function inputs(): SaleLineInput[] | null {
  if (!cart.value.length) {
    ElMessage.warning('请先添加商品')
    return null
  }
  const result: SaleLineInput[] = []
  for (const row of cart.value) {
    const count = row.quantity
    if (count === null || !Number.isInteger(count) || count < 1 || count > 999999) {
      ElMessage.warning(`商品「${row.name}」数量必须为1～999999的整数`)
      return null
    }
    result.push({ productId: row.id, quantity: count, expectedPrice: row.salePrice })
  }
  return result
}
function updateKnownState(value: SaleQuote) {
  for (const item of value.items) {
    const row = cart.value.find((row) => row.id === item.productId)
    if (row) {
      row.name = item.productName
      row.code = item.productCode
      row.unit = item.unit
      row.saleableQty = item.saleableQty
    }
  }
}
async function refreshQuote() {
  if (quoteBusy.value || busy.value) return
  const items = inputs()
  if (!items) return
  quoteBusy.value = true
  quote.value = null
  try {
    const result = await quoteSale(items)
    quote.value = result
    updateKnownState(result)
    return result
  } catch (reason) {
    showError(reason)
  } finally {
    quoteBusy.value = false
  }
}
async function checkout() {
  if (locked.value || contextLoading.value) return
  if (!(await loadContext())) return
  const result = await refreshQuote()
  if (result) paymentOpen.value = true
}
async function acceptPrices() {
  if (locked.value || !quote.value) return
  for (const item of quote.value.items) {
    const row = cart.value.find((row) => row.id === item.productId)
    if (row) row.salePrice = item.currentPrice
  }
  await refreshQuote()
}
async function confirm(payment: PaymentMethod, received: string) {
  if (locked.value || !quote.value?.canConfirm) return
  const items = inputs()
  if (!items) return
  try {
    await submit({
      requestKey: crypto.randomUUID(),
      paymentMethod: payment,
      receivedAmount: new Decimal(received).toFixed(2),
      items,
    })
  } catch (reason) {
    showError(reason)
    if (reason instanceof ApiError && reason.status === 409) await refreshQuote()
  } finally {
    if (pending.value) paymentOpen.value = false
  }
}
async function retryConfirmation() {
  try {
    await retry()
  } catch (reason) {
    showError(reason)
    if (!pending.value) await refreshQuote()
  }
}
onMounted(async () => {
  void loadContext()
  if (pending.value) {
    cart.value = pending.value.items.map((item) => ({
      id: item.productId,
      code: item.productId,
      barcode: null,
      name: `商品 ${item.productId}`,
      specification: null,
      unit: '件',
      imagePath: null,
      salePrice: item.expectedPrice,
      saleableQty: 0,
      frozen: false,
      quantity: item.quantity,
    }))
    await recover()
    if (pending.value) {
      try {
        updateKnownState(await quoteSale(pending.value.items))
      } catch (reason) {
        showError(reason)
      }
    }
  }
})
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">FRONT DESK CHECKOUT</div>
      <h1>前台开单</h1>
      <p>
        收银员：{{ user.realName }} · 北京业务日：{{ context?.today || '正在读取' }} ·
        模拟收款，明细完成后才扣库存。
      </p>
    </div>
    <router-link to="/sales"><el-button>销售记录</el-button></router-link>
  </div>
  <el-alert v-if="contextError" type="error" :title="contextError" :closable="false"
    ><el-button :loading="contextLoading" @click="loadContext">重试加载</el-button></el-alert
  >
  <template v-if="receipt"
    ><SaleReceiptPanel :detail="receipt" />
    <div class="receipt-actions">
      <router-link :to="`/sales/${receipt.order.id}`"
        ><el-button>查看销售详情</el-button></router-link
      ><el-button type="primary" @click="continueSelling">继续开单</el-button>
    </div></template
  >
  <template v-else
    ><section v-if="pending" class="panel recovery-panel">
      <h2>{{ uncertain ? '本次收款结果待核实' : '正在确认本次收款' }}</h2>
      <p>{{ problem || '请等待确认结果。查询会读取原单，继续确认会沿用本次请求。' }}</p>
      <div class="query-bar">
        <el-button :loading="busy" @click="recover">查询本次结果</el-button
        ><el-button v-if="uncertain" type="primary" :loading="busy" @click="retryConfirmation"
          >继续确认本次收款</el-button
        >
      </div>
    </section>
    <SaleProductSearch ref="productSearch" :disabled="locked" @selected="addProduct" /><SaleCart
      :items="cart"
      :disabled="locked || !context"
      @quantity="quantity"
      @remove="remove"
      @clear="clear"
      @checkout="checkout"
  /></template>
  <SalePaymentDialog
    v-model="paymentOpen"
    :quote="quote"
    :busy="busy"
    :loading="quoteBusy"
    :default-payment="context?.defaultPaymentMethod || 'CASH'"
    @confirm="confirm"
    @refresh="refreshQuote"
    @accept-prices="acceptPrices"
  />
</template>
<style scoped>
.recovery-panel {
  margin-bottom: 24px;
  border-color: #e3c78b;
  background: #fffdf6;
}
.recovery-panel p {
  color: #7c6843;
  line-height: 1.7;
}
.receipt-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>
