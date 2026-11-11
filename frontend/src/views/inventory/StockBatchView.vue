<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { stockBatches } from '../../api/inventory'
import type { BatchQuery, StockBatch } from '../../types/stock'
import { businessTime as purchaseTime } from '../../utils/format'
const route = useRoute(),
  auth = useAuth(),
  id = route.params.id as string
const name = typeof route.query.productName === 'string' ? route.query.productName : '商品批次'
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  StockBatch,
  BatchQuery
>({ keyword: '', includeEmpty: true, pageNum: 1, pageSize: 10 }, (q) => stockBatches(id, q))
onMounted(load)
function status(row: StockBatch) {
  if (row.sourceStatus === 'REVERSED') return '入库已撤销'
  if (row.stockQty === 0) return '无库存'
  if (row.remainingDays !== null && row.remainingDays < 0) return '已过期，禁止销售'
  if (!row.enabled) return '商品已停用'
  if (!row.canSell) return '商品禁止销售'
  return row.saleable ? '可售' : '不可售'
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">BATCH TRACEABILITY</div>
      <h1>{{ name }} · 批次详情</h1>
      <p>展示实际采购成本和来源；来源采购详情仍受单据归属权限限制。</p>
    </div>
    <router-link to="/inventory/products"><el-button>当前库存</el-button></router-link>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="系统批次 / 供应商批号 / 采购单号"
        clearable
        aria-label="批次关键词"
      /><el-checkbox v-model="query.includeEmpty" @change="search">包含零库存批次</el-checkbox
      ><el-button type="primary" native-type="submit">查询批次</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button :loading="loading" @click="load">刷新</el-button>
    </form>
    <el-alert v-if="error" type="error" :title="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    ><el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      empty-text="暂无有效入库批次；未入库草稿不建立批次"
      ><el-table-column label="系统批次 / 供应商批号" min-width="275" fixed
        ><template #default="{ row }"
          >{{ row.batchNo }}
          <div class="muted">{{ row.supplierBatchNo || '无供应商批号' }}</div></template
        ></el-table-column
      ><el-table-column label="来源采购单" min-width="265"
        ><template #default="{ row }"
          ><router-link v-if="row.sourceReadable" :to="`/purchases/${row.purchaseId}`">{{
            row.purchaseNo
          }}</router-link
          ><span v-else
            >{{ row.purchaseNo }}<small class="muted">（仅追溯单号）</small></span
          ></template
        ></el-table-column
      ><el-table-column label="生产 / 到期" min-width="150"
        ><template #default="{ row }"
          >{{ row.productionDate || '不管理保质期' }}
          <div>{{ row.expiryDate || '—' }}</div></template
        ></el-table-column
      ><el-table-column label="剩余天数" width="100"
        ><template #default="{ row }">{{
          row.remainingDays ?? '不适用'
        }}</template></el-table-column
      ><el-table-column prop="stockQty" label="当前数量" width="100" /><el-table-column
        prop="unitCost"
        label="采购单价（元）"
        width="130"
        align="right"
      /><el-table-column label="可售状态" min-width="180"
        ><template #default="{ row }"
          ><el-tag :type="row.saleable ? 'success' : 'info'">{{ status(row) }}</el-tag>
          <div v-if="row.frozen" class="muted">盘点冻结，暂不能交易</div></template
        ></el-table-column
      ><el-table-column label="入库时间（北京）" min-width="180"
        ><template #default="{ row }">{{ purchaseTime(row.stockedAt) }}</template></el-table-column
      ><el-table-column v-if="auth.can('inventory.movement')" label="操作" width="100" fixed="right"
        ><template #default="{ row }"
          ><router-link
            :to="{
              path: '/inventory/movements',
              query: { productId: id, batchId: row.id, productName: name },
            }"
            >批次流水</router-link
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 个批次 · 到期当天仍可售，冻结单独显示</span
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
