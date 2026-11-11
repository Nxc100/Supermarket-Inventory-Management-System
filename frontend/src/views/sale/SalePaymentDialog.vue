<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Decimal from 'decimal.js'
import { ElMessage } from 'element-plus'
import type { PaymentMethod, SaleQuote } from '../../types/sale'
import { paymentMethods } from '../../types/sale'
import { businessTime } from '../../utils/format'
import { validMoney } from '../../utils/purchase'
const props = defineProps<{
  modelValue: boolean
  quote: SaleQuote | null
  busy: boolean
  loading: boolean
  defaultPayment: PaymentMethod
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [payment: PaymentMethod, received: string]
  refresh: []
  acceptPrices: []
}>()
const payment = ref<PaymentMethod>('CASH'),
  received = ref('')
watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      payment.value = props.defaultPayment
      received.value = ''
    }
  },
)
const change = computed(() =>
  props.quote && validMoney(received.value)
    ? new Decimal(received.value).minus(props.quote.totalAmount).toFixed(2)
    : '—',
)
const pricesChanged = computed(() => props.quote?.items.some((item) => item.priceChanged) ?? false)
function close(done?: () => void) {
  if (props.busy) return
  emit('update:modelValue', false)
  done?.()
}
function confirm() {
  if (!props.quote?.canConfirm || props.busy || props.loading) return
  const amount = payment.value === 'MOCK_EPAY' ? props.quote.totalAmount : received.value
  if (!validMoney(amount) || new Decimal(amount).lt(props.quote.totalAmount)) {
    ElMessage.warning('实收金额必须不少于应收金额，且最多两位小数')
    return
  }
  emit('confirm', payment.value, new Decimal(amount).toFixed(2))
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    title="确认收款"
    width="900px"
    top="5vh"
    class="archive-editor-dialog"
    :close-on-click-modal="false"
    :before-close="close"
    @update:model-value="emit('update:modelValue', $event)"
    ><div v-loading="loading">
      <template v-if="quote"
        ><p class="muted">
          预校验时间（北京）：{{
            businessTime(quote.checkedAt)
          }}。确认收款时将再次核实售价、库存和冻结。
        </p>
        <el-alert
          v-if="!quote.canConfirm"
          title="请先处理以下明细问题，再确认收款。"
          type="warning"
          :closable="false" /><el-table :data="quote.items" row-key="productId"
          ><el-table-column prop="productName" label="商品" min-width="160" /><el-table-column
            prop="quantity"
            label="数量"
            width="75"
          /><el-table-column prop="expectedPrice" label="开单售价" width="105" /><el-table-column
            prop="currentPrice"
            label="当前售价"
            width="105"
          /><el-table-column prop="lineAmount" label="当前行金额" width="115" /><el-table-column
            label="校验结果"
            min-width="200"
            ><template #default="{ row }"
              ><span :class="{ problem: row.problem }">{{
                row.problem || '可确认'
              }}</span></template
            ></el-table-column
          ></el-table
        >
        <div class="query-bar payment-summary">
          <strong
            >{{ quote.itemCount }} 种商品 · {{ quote.totalQty }} 件基本单位 · 应收 ¥
            {{ quote.totalAmount }}</strong
          ><el-button :disabled="busy || loading" @click="emit('refresh')">重新校验</el-button
          ><el-button
            v-if="pricesChanged"
            type="warning"
            :disabled="busy || loading"
            @click="emit('acceptPrices')"
            >已核对，采用当前售价并重新校验</el-button
          >
        </div>
        <el-form label-position="top"
          ><el-form-item label="收款方式"
            ><el-radio-group v-model="payment" :disabled="busy"
              ><el-radio-button
                v-for="(label, value) in paymentMethods"
                :key="value"
                :value="value"
                >{{ label }}</el-radio-button
              ></el-radio-group
            ></el-form-item
          ><el-form-item v-if="payment === 'CASH'" label="现金实收（元）"
            ><el-input
              v-model="received"
              aria-label="现金实收金额"
              placeholder="请输入实际收到的现金"
              maxlength="17"
              :disabled="busy"
            />
            <div class="cash-change">找零：¥ {{ change }}</div></el-form-item
          ><el-alert
            v-else
            title="模拟确认，不发生真实扣款。实收按应收金额记录，找零为0。"
            type="info"
            :closable="false" /></el-form
      ></template>
    </div>
    <template #footer
      ><el-button :disabled="busy" @click="close()">返回调整</el-button
      ><el-button
        type="primary"
        :loading="busy"
        :disabled="loading || !quote?.canConfirm"
        @click="confirm"
        >确认收款并完成销售</el-button
      ></template
    ></el-dialog
  >
</template>
<style scoped>
.payment-summary {
  margin-top: 20px;
}
.cash-change {
  margin-top: 12px;
  color: #166c56;
}
.problem {
  color: #b65232;
}
</style>
