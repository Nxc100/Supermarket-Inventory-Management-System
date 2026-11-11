<script setup lang="ts">
import type { SaleDetail } from '../../types/sale'
import { paymentMethods, saleStatuses } from '../../types/sale'
import { businessTime } from '../../utils/format'
import { useAuth } from '../../stores/auth'
defineProps<{ detail: SaleDetail }>()
const auth = useAuth()
</script>
<template>
  <section class="panel">
    <div class="page-heading">
      <div>
        <h2>{{ detail.order.orderNo }}</h2>
        <p>
          {{ saleStatuses[detail.order.status] }} ·
          {{ businessTime(detail.order.completedAt) }}（北京）
        </p>
      </div>
      <el-tag :type="detail.order.status === 'COMPLETED' ? 'success' : 'warning'">{{
        saleStatuses[detail.order.status]
      }}</el-tag>
    </div>
    <el-descriptions :column="3" border
      ><el-descriptions-item label="原应收金额"
        >¥ {{ detail.order.totalAmount }}</el-descriptions-item
      ><el-descriptions-item label="实收金额"
        >¥ {{ detail.order.receivedAmount }}</el-descriptions-item
      ><el-descriptions-item label="找零">¥ {{ detail.order.changeAmount }}</el-descriptions-item
      ><el-descriptions-item label="付款方式">{{
        paymentMethods[detail.order.paymentMethod]
      }}</el-descriptions-item
      ><el-descriptions-item label="收银员">{{ detail.order.cashierName }}</el-descriptions-item
      ><el-descriptions-item label="商品 / 数量"
        >{{ detail.order.itemCount }} 种 /
        {{ detail.order.totalQty }} 件基本单位</el-descriptions-item
      ></el-descriptions
    >
    <p v-if="detail.order.paymentMethod === 'MOCK_EPAY'" class="muted payment-note">
      模拟收款记录，不发生真实扣款。
    </p>
    <el-alert
      v-if="detail.order.status === 'CORRECTED'"
      :title="`已整单纠错：${detail.correction.reason} · ${detail.correction.actorName} · ${businessTime(detail.correction.occurredAt)}（北京）`"
      type="warning"
      :closable="false"
    /><el-alert
      v-if="detail.expiredSourceStock"
      title="原销售来源批次当前存在过期库存，恢复的过期部分不计入可售，请核实实物并处理。"
      type="warning"
      :closable="false"
      ><router-link v-if="auth.can('inventory.batch')" to="/inventory/products"
        >进入库存查询</router-link
      ><span v-else>请联系管理员处理。</span></el-alert
    ><el-table :data="detail.items" row-key="id"
      ><el-table-column prop="lineNo" label="行" width="50" /><el-table-column
        label="商品快照"
        min-width="220"
        ><template #default="{ row }"
          ><strong>{{ row.productName }}</strong>
          <div class="muted">{{ row.productCode }} / {{ row.productBarcode || '无条码' }}</div>
          <small class="muted"
            >{{ row.categoryName }} · {{ row.specification || '无规格' }} / {{ row.unit }}</small
          ></template
        ></el-table-column
      ><el-table-column
        prop="unitPrice"
        label="销售单价（元）"
        width="150"
        align="right" /><el-table-column prop="quantity" label="数量" width="100" /><el-table-column
        prop="lineAmount"
        label="行金额（元）"
        width="150"
        align="right"
    /></el-table>
  </section>
</template>
<style scoped>
.payment-note {
  margin-top: 16px;
}
.el-alert {
  margin-top: 16px;
}
</style>
