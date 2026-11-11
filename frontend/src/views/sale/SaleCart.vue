<script setup lang="ts">
import { computed } from 'vue'
import Decimal from 'decimal.js'
import type { SaleCartLine } from '../../types/sale'
import { lineAmount } from '../../utils/purchase'
const props = defineProps<{ items: SaleCartLine[]; disabled: boolean }>()
const emit = defineEmits<{
  quantity: [id: string, quantity: number | null]
  remove: [id: string]
  clear: []
  checkout: []
}>()
const totalQty = computed(() =>
  props.items.reduce(
    (sum, item) => sum + (Number.isInteger(item.quantity) ? (item.quantity ?? 0) : 0),
    0,
  ),
)
const totalAmount = computed(() =>
  props.items
    .reduce((sum, item) => {
      const amount = lineAmount(item.quantity, item.salePrice)
      return sum.plus(amount === '—' ? 0 : amount)
    }, new Decimal(0))
    .toFixed(2),
)
</script>
<template>
  <section class="panel">
    <div class="page-heading">
      <div>
        <h2>当前开单</h2>
        <p>未完成收款前不占库存、不计销售。</p>
      </div>
      <el-button :disabled="disabled || !items.length" @click="emit('clear')">清空开单</el-button>
    </div>
    <el-table :data="items" row-key="id" empty-text="搜索商品并加入本次开单"
      ><el-table-column label="商品 / 单位" min-width="220"
        ><template #default="{ row }"
          ><strong>{{ row.name }}</strong>
          <div class="muted">
            {{ row.code }} · {{ row.specification || '无规格' }} / {{ row.unit }}
          </div>
          <div v-if="row.quantity !== null && row.quantity > row.saleableQty" class="stock-warning">
            数量超过页面已知可售量 {{ row.saleableQty }}，请刷新核实。
          </div></template
        ></el-table-column
      ><el-table-column
        prop="salePrice"
        label="开单售价"
        width="100"
        align="right"
      /><el-table-column label="数量" width="155"
        ><template #default="{ row }"
          ><el-input-number
            :model-value="row.quantity"
            :disabled="disabled"
            :min="1"
            :max="999999"
            :precision="0"
            controls-position="right"
            :aria-label="`${row.name}销售数量`"
            style="width: 130px"
            @update:model-value="
              emit('quantity', row.id, $event ?? null)
            " /></template></el-table-column
      ><el-table-column label="行金额" width="110" align="right"
        ><template #default="{ row }">{{
          lineAmount(row.quantity, row.salePrice)
        }}</template></el-table-column
      ><el-table-column label="操作" width="80"
        ><template #default="{ row }"
          ><el-button :disabled="disabled" link type="danger" @click="emit('remove', row.id)"
            >移除</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span
        >{{ items.length }} 种 · {{ totalQty }} 件基本单位 · 合计
        <strong>¥ {{ totalAmount }}</strong></span
      ><el-button
        type="primary"
        size="large"
        :disabled="disabled || !items.length"
        @click="emit('checkout')"
        >结算</el-button
      >
    </div>
  </section>
</template>
<style scoped>
.stock-warning {
  color: #b65232;
  font-size: 12px;
}
.panel {
  margin-top: 24px;
}
</style>
