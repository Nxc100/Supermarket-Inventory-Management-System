<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Decimal from 'decimal.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchase, purchaseContext, savePurchase } from '../../api/purchases'
import { ApiError } from '../../api/http'
import { useAuth } from '../../stores/auth'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import { showError } from '../../utils/feedback'
import { businessTime } from '../../utils/format'
import { purchaseProducts } from '../../api/products'
import { lineAmount, purchaseStatuses, validMoney } from '../../utils/purchase'
import type { PurchaseLineInput, SupplierOption } from '../../types/purchase'
import type { PurchaseProduct } from '../../types/catalog'
import PurchaseProductPicker from './PurchaseProductPicker.vue'
import PurchaseSupplierPicker from './PurchaseSupplierPicker.vue'
interface EditorLine extends Omit<PurchaseLineInput, 'quantity'> {
  quantity: number | null
  key: string
  name: string
  code: string
  barcode: string | null
  unit: string
  specification: string | null
  managesExpiry: boolean
  shelfLifeDays: number | null
}
const route = useRoute(),
  router = useRouter(),
  auth = useAuth()
const id = typeof route.params.id === 'string' ? route.params.id : undefined
const loading = ref(true),
  saving = ref(false),
  registeringArrival = ref(false),
  error = ref(''),
  unknownResult = ref(false),
  conflict = ref(false)
const resultQuery = ref('')
const supplierPicker = ref(false),
  productPicker = ref(false),
  baseline = ref(''),
  ready = ref(false)
const supplierName = ref(''),
  currentStatus = ref('新建草稿'),
  operatorName = ref(auth.user?.realName ?? ''),
  version = ref<number>()
const form = reactive({
  supplierId: '',
  expectedArrivalDate: null as string | null,
  arrivedAt: null as string | null,
  remark: '',
  items: [] as EditorLine[],
})
const dirty = computed(() => ready.value && JSON.stringify(form) !== baseline.value)
useDirtyGuard(dirty)
const totalQty = computed(() =>
  form.items.reduce((sum, row) => sum + (Number.isInteger(row.quantity) ? row.quantity! : 0), 0),
)
const totalReceived = computed(() =>
  form.items.reduce(
    (sum, row) => sum + (Number.isInteger(row.receivedQty) ? row.receivedQty! : 0),
    0,
  ),
)
const totalAmount = computed(() =>
  form.items
    .reduce(
      (sum, row) =>
        sum.plus(
          lineAmount(row.quantity, row.unitCost) === '—'
            ? 0
            : lineAmount(row.quantity, row.unitCost),
        ),
      new Decimal(0),
    )
    .toFixed(2),
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    if (id) {
      const detail = await purchase(id),
        order = detail.order
      if (!['DRAFT', 'REJECTED'].includes(order.status)) {
        await router.replace(`/purchases/${id}`)
        return
      }
      version.value = order.rowVersion
      currentStatus.value = purchaseStatuses[order.status]
      operatorName.value = order.operatorName
      supplierName.value = order.supplierName
      Object.assign(form, {
        supplierId: order.supplierId,
        expectedArrivalDate: order.expectedArrivalDate,
        arrivedAt: order.arrivedAt ? businessTime(order.arrivedAt).replace(' ', 'T') : null,
        remark: order.remark ?? '',
        items: detail.items.map((item) => ({
          key: crypto.randomUUID(),
          productId: item.productId,
          quantity: item.quantity,
          receivedQty: item.receivedQty,
          unitCost: item.unitCost,
          supplierBatchNo: item.supplierBatchNo,
          productionDate: item.productionDate,
          expiryDate: item.expiryDate,
          name: item.productName,
          code: item.productCode,
          barcode: item.productBarcode,
          unit: item.unit,
          specification: item.specification,
          managesExpiry: item.managesExpiry,
          shelfLifeDays: null,
        })),
      })
    }
    if (!id && typeof route.query.productCode === 'string' && !form.items.length) {
      const code = route.query.productCode
      const result = await purchaseProducts({ keyword: code, pageNum: 1, pageSize: 10 })
      const selected = result.records.find((p) => p.code === code)
      if (!selected || selected.frozen)
        throw new Error('预选商品已停用、禁采或盘点中，请从商品选择器重新核实')
      addProduct(selected)
      form.items[0]!.quantity = null
      form.items[0]!.unitCost = ''
    }
    baseline.value = JSON.stringify(form)
    ready.value = true
    conflict.value = false
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '采购加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)
function selectSupplier(supplier: SupplierOption) {
  form.supplierId = supplier.id
  supplierName.value = supplier.name
}
function addProduct(product: PurchaseProduct) {
  if (form.items.length >= 100) {
    ElMessage.warning('每张采购单最多100行')
    return
  }
  form.items.push({
    key: crypto.randomUUID(),
    productId: product.id,
    name: product.name,
    code: product.code,
    barcode: product.barcode,
    specification: product.specification,
    unit: product.unit,
    managesExpiry: product.managesExpiry,
    shelfLifeDays: product.shelfLifeDays,
    quantity: 1,
    receivedQty: null,
    unitCost: product.referenceCost ?? '',
    supplierBatchNo: null,
    productionDate: null,
    expiryDate: null,
  })
}
async function clearLines() {
  try {
    await ElMessageBox.confirm('确认清空所有未入库明细？', '清空采购明细', { type: 'warning' })
    form.items = []
  } catch (reason) {
    showError(reason)
  }
}
async function registerArrival() {
  if (registeringArrival.value || saving.value) return
  registeringArrival.value = true
  try {
    const context = await purchaseContext()
    form.arrivedAt = businessTime(context.now).replace(' ', 'T')
    ElMessage.info('已填入北京时间，请按实物填写各行实收数量和批次日期')
  } catch (reason) {
    showError(reason)
  } finally {
    registeringArrival.value = false
  }
}
function proposeExpiry(row: EditorLine) {
  if (!row.productionDate || !row.shelfLifeDays) return
  const date = new Date(`${row.productionDate}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + row.shelfLifeDays)
  row.expiryDate = date.toISOString().slice(0, 10)
  ElMessage.info('到期日期已预填，请根据实物标签确认')
}
function validation() {
  if (!form.supplierId) return '请选择启用供应商'
  if (!form.items.length) return '请至少添加一行商品'
  for (const [index, row] of form.items.entries()) {
    const prefix = `第${index + 1}行：`
    if (
      row.quantity === null ||
      !Number.isInteger(row.quantity) ||
      row.quantity < 1 ||
      row.quantity > 999999
    )
      return `${prefix}采购数量为1～999999的整数`
    if (
      row.receivedQty != null &&
      (!Number.isInteger(row.receivedQty) || row.receivedQty < 0 || row.receivedQty > 999999)
    )
      return `${prefix}实收数量应为空或0～999999的整数`
    if (!validMoney(row.unitCost, true)) return `${prefix}请填写大于0、最多两位小数的单价`
    if (new Decimal(lineAmount(row.quantity, row.unitCost)).gt('99999999999999.99'))
      return `${prefix}行金额超出上限`
  }
  if (new Decimal(totalAmount.value).gt('99999999999999.99')) return '单据总金额超出上限'
  return ''
}
async function save() {
  if (saving.value || conflict.value || unknownResult.value || registeringArrival.value) return
  const message = validation()
  if (message) {
    ElMessage.warning(message)
    return
  }
  saving.value = true
  resultQuery.value = supplierName.value
  try {
    const detail = await savePurchase(id, {
      supplierId: form.supplierId,
      expectedVersion: version.value,
      expectedArrivalDate: form.expectedArrivalDate || null,
      arrivedAt: form.arrivedAt ? `${form.arrivedAt}+08:00` : null,
      remark: form.remark.trim() || null,
      items: form.items.map((row) => ({
        productId: row.productId,
        quantity: row.quantity!,
        receivedQty: row.receivedQty ?? null,
        unitCost: new Decimal(row.unitCost).toFixed(2),
        supplierBatchNo: row.supplierBatchNo?.trim() || null,
        productionDate: row.productionDate || null,
        expiryDate: row.expiryDate || null,
      })),
    })
    baseline.value = JSON.stringify(form)
    ElMessage.success('采购草稿已保存，库存未改变')
    await router.replace(`/purchases/${detail.order.id}`)
  } catch (reason) {
    if (reason instanceof ApiError && (reason.status === 0 || reason.status >= 500)) {
      unknownResult.value = true
      return
    }
    if (reason instanceof ApiError && reason.code === 'VERSION_CONFLICT' && id)
      conflict.value = true
    showError(reason)
  } finally {
    saving.value = false
  }
}
async function reload() {
  try {
    if (dirty.value)
      await ElMessageBox.confirm('重新加载会丢弃当前未保存内容，确认继续？', '重新加载采购', {
        type: 'warning',
      })
    await load()
  } catch (reason) {
    showError(reason)
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">PURCHASE DRAFT</div>
      <h1>{{ id ? '编辑采购 / 到货登记' : '新建采购单' }}</h1>
      <p>{{ currentStatus }} · 经办人：{{ operatorName }} · 保存草稿不增加库存</p>
    </div>
    <router-link :to="id ? `/purchases/${id}` : '/purchases'"
      ><el-button>返回</el-button></router-link
    >
  </div>
  <el-alert
    v-if="unknownResult"
    title="本次采购保存结果待核实"
    type="warning"
    :closable="false"
    show-icon
  >
    <p>
      输入已保留，请先核对采购单是否已保存，勿再次提交生成重复草稿。检查商品、经办人、创建时间及明细后再决定后续操作。
    </p>
    <router-link
      :to="id ? `/purchases/${id}` : { path: '/purchases', query: { keyword: resultQuery } }"
      ><el-button>去核实采购保存结果</el-button></router-link
    >
  </el-alert>
  <el-alert v-if="error" :title="error" type="error" :closable="false"
    ><el-button @click="load">重试</el-button></el-alert
  >
  <el-alert
    v-if="conflict"
    title="保存未成功，请核对提示。可保留当前输入查看资料，或重新加载单据后继续编辑。"
    type="warning"
    :closable="false"
    ><el-button @click="reload">重新加载单据</el-button></el-alert
  >
  <div v-if="ready && !error" v-loading="loading || saving" class="panel">
    <el-form label-position="top"
      ><div class="purchase-header-grid">
        <el-form-item label="供应商（必选）"
          ><div class="supplier-field">
            <el-input
              :model-value="supplierName"
              readonly
              placeholder="请选择供应商"
              aria-label="已选供应商"
            /><el-button @click="supplierPicker = true">选择供应商</el-button>
          </div></el-form-item
        ><el-form-item label="预计到货日期"
          ><el-date-picker
            v-model="form.expectedArrivalDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可留空" /></el-form-item
        ><el-form-item label="到货时间（北京时间，审核前必填）"
          ><div class="supplier-field">
            <el-date-picker
              v-model="form.arrivedAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="未到货可留空"
              aria-label="到货时间"
              :disabled="registeringArrival || saving"
            /><el-button :loading="registeringArrival" :disabled="saving" @click="registerArrival"
              >登记到货</el-button
            >
          </div></el-form-item
        >
      </div>
      <el-form-item label="备注"
        ><el-input
          v-model="form.remark"
          maxlength="500"
          show-word-limit
          placeholder="选填，最多500字" /></el-form-item
    ></el-form>
    <div class="query-bar">
      <el-button type="primary" :disabled="form.items.length >= 100" @click="productPicker = true"
        >添加商品</el-button
      ><el-button :disabled="!form.items.length" @click="clearLines">清空明细</el-button
      ><span class="muted">实收空白表示未登记；提交前须与采购数量一致。不同批次分行录入。</span>
    </div>
    <el-table :data="form.items" row-key="key" empty-text="请添加采购商品"
      ><el-table-column type="index" label="行" width="45" fixed /><el-table-column
        label="商品 / 编码"
        min-width="205"
        fixed
        ><template #default="{ row }"
          ><strong>{{ row.name }}</strong>
          <div class="muted">{{ row.code }} / {{ row.barcode || '无条码' }}</div>
          <small class="muted"
            >{{ row.specification || '无规格' }} · {{ row.unit }}</small
          ></template
        ></el-table-column
      ><el-table-column label="采购数量" width="140"
        ><template #default="{ row, $index }"
          ><el-input-number
            v-model="row.quantity"
            :min="1"
            :max="999999"
            :precision="0"
            controls-position="right"
            :aria-label="`第${$index + 1}行采购数量`"
            style="width: 120px" /></template></el-table-column
      ><el-table-column label="实际采购单价（元）" width="155"
        ><template #default="{ row, $index }"
          ><el-input
            v-model="row.unitCost"
            :aria-label="`第${$index + 1}行采购单价`"
            placeholder="必填"
            maxlength="17" /></template></el-table-column
      ><el-table-column label="实收数量" width="140"
        ><template #default="{ row, $index }"
          ><el-input-number
            v-model="row.receivedQty"
            :min="0"
            :max="999999"
            :precision="0"
            controls-position="right"
            :aria-label="`第${$index + 1}行实收数量`"
            placeholder="未登记"
            style="width: 120px" /></template></el-table-column
      ><el-table-column label="供应商批号" width="150"
        ><template #default="{ row, $index }"
          ><el-input
            v-model="row.supplierBatchNo"
            maxlength="64"
            placeholder="选填"
            :aria-label="`第${$index + 1}行供应商批号`" /></template></el-table-column
      ><el-table-column label="生产日期" width="175"
        ><template #default="{ row, $index }"
          ><el-date-picker
            v-if="row.managesExpiry"
            v-model="row.productionDate"
            type="date"
            value-format="YYYY-MM-DD"
            :aria-label="`第${$index + 1}行生产日期`"
            placeholder="待登记"
            style="width: 150px"
          /><span v-else class="muted">不管理保质期</span></template
        ></el-table-column
      ><el-table-column label="到期日期（按实物确认）" width="190"
        ><template #default="{ row, $index }"
          ><template v-if="row.managesExpiry"
            ><el-date-picker
              v-model="row.expiryDate"
              type="date"
              value-format="YYYY-MM-DD"
              :aria-label="`第${$index + 1}行到期日期`"
              placeholder="待登记"
              style="width: 160px"
            /><el-button
              v-if="row.shelfLifeDays"
              link
              type="primary"
              :disabled="!row.productionDate"
              @click="proposeExpiry(row)"
              >按默认天数预填</el-button
            ></template
          ><span v-else>—</span></template
        ></el-table-column
      ><el-table-column label="行金额（元）" width="140" align="right"
        ><template #default="{ row }">{{
          lineAmount(row.quantity, row.unitCost)
        }}</template></el-table-column
      ><el-table-column label="操作" width="80" fixed="right"
        ><template #default="{ $index }"
          ><el-button link type="danger" @click="form.items.splice($index, 1)"
            >移除行</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span
        >采购 {{ totalQty }} · 实收已登记 {{ totalReceived }} · 合计
        <strong>¥ {{ totalAmount }}</strong></span
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="conflict || unknownResult || registeringArrival"
        @click="save"
        >保存草稿</el-button
      >
    </div>
  </div>
  <PurchaseSupplierPicker
    v-model="supplierPicker"
    @selected="selectSupplier"
  /><PurchaseProductPicker v-model="productPicker" @selected="addProduct" />
</template>
<style scoped>
.purchase-header-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 2fr;
  gap: 20px;
}
.supplier-field {
  display: flex;
  gap: 8px;
  width: 100%;
}
@media (max-width: 1200px) {
  .purchase-header-grid {
    grid-template-columns: 1fr;
  }
}
</style>
