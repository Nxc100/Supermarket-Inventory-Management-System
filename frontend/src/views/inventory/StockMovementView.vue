<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { usePagedList } from '../../composables/usePagedList'
import { stockMovements } from '../../api/inventory'
import { movementTypes, type MovementQuery, type StockMovement } from '../../types/stock'
import { businessTime as purchaseTime } from '../../utils/format'
const route = useRoute()
const productName = typeof route.query.productName === 'string' ? route.query.productName : ''
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  StockMovement,
  MovementQuery
>(
  {
    keyword: '',
    productId: typeof route.query.productId === 'string' ? route.query.productId : undefined,
    batchId: typeof route.query.batchId === 'string' ? route.query.batchId : undefined,
    pageNum: 1,
    pageSize: 10,
  },
  stockMovements,
)
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">INVENTORY LEDGER</div>
      <h1>库存流水</h1>
      <p>
        {{ productName ? `${productName} · ` : ''
        }}{{
          query.batchId ? '限定所选批次 · ' : ''
        }}按库存事件发生时间查询，正数增加、负数减少，保留纠错轨迹。
      </p>
    </div>
    <router-link to="/inventory/products"><el-button>当前库存</el-button></router-link>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="业务单号 / 批次 / 商品编码、名称"
        clearable
        aria-label="流水关键词"
      /><el-select
        v-model="query.eventType"
        placeholder="全部业务类型"
        clearable
        aria-label="流水业务类型"
        ><el-option
          v-for="(label, value) in movementTypes"
          :key="value"
          :value="value"
          :label="label" /></el-select
      ><el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="开始日期"
        aria-label="流水开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="结束日期"
        aria-label="流水结束日期"
      /><el-button type="primary" native-type="submit">查询流水</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button :loading="loading" @click="load">刷新</el-button>
    </form>
    <el-alert v-if="error" type="error" :title="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    ><el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的库存流水"
      ><el-table-column label="时间（北京）/ 商品" min-width="220" fixed
        ><template #default="{ row }"
          >{{ purchaseTime(row.occurredAt) }}
          <div>{{ row.productName }} · {{ row.productCode }}</div>
          <small class="muted">{{ row.unit }}</small></template
        ></el-table-column
      ><el-table-column label="类型 / 业务单号" min-width="270"
        ><template #default="{ row }"
          >{{ movementTypes[row.eventType as keyof typeof movementTypes] }}
          <div>
            <router-link
              v-if="row.sourceReadable && row.purchaseId"
              :to="`/purchases/${row.purchaseId}`"
              >{{ row.businessNo }}</router-link
            ><span v-else>{{ row.businessNo }}</span>
          </div></template
        ></el-table-column
      ><el-table-column prop="batchNo" label="批次" min-width="260" /><el-table-column
        label="批次变动（前 → 后）"
        min-width="160"
        ><template #default="{ row }"
          >{{ row.batchBefore }} → {{ row.batchAfter }}</template
        ></el-table-column
      ><el-table-column label="增减量" width="100" align="right"
        ><template #default="{ row }"
          ><strong :class="row.deltaQty > 0 ? 'increase' : 'decrease'"
            >{{ row.deltaQty > 0 ? '+' : '' }}{{ row.deltaQty }}</strong
          ></template
        ></el-table-column
      ><el-table-column label="商品变动（前 → 后）" min-width="170"
        ><template #default="{ row }"
          >{{ row.productBefore }} → {{ row.productAfter }}</template
        ></el-table-column
      ><el-table-column prop="actorName" label="操作者" width="115" /><el-table-column
        prop="reason"
        label="原因"
        min-width="180"
    /></el-table>
    <div class="table-footer">
      <span>共 {{ total }} 条流水 · 按时间及记录顺序倒序排列</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes,prev,pager,next"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </section>
</template>
<style scoped>
.increase {
  color: #166c56;
}
.decrease {
  color: #b65232;
}
</style>
