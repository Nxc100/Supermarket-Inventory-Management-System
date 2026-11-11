<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { purchases } from '../../api/purchases'
import type {
  PurchaseQuery,
  PurchaseStatus,
  PurchaseSummary,
  ReconcileStatus,
} from '../../types/purchase'
import { purchaseStatuses, reconcileStatuses, purchaseTime } from '../../utils/purchase'
const props = defineProps<{ review?: boolean }>()
const auth = useAuth(),
  route = useRoute()
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  PurchaseSummary,
  PurchaseQuery
>(
  {
    keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
    pageNum: 1,
    pageSize: 10,
    status: props.review
      ? 'PENDING'
      : typeof route.query.status === 'string' &&
          Object.hasOwn(purchaseStatuses, route.query.status)
        ? (route.query.status as PurchaseStatus)
        : undefined,
    reconcileStatus:
      typeof route.query.reconcileStatus === 'string' &&
      Object.hasOwn(reconcileStatuses, route.query.reconcileStatus)
        ? (route.query.reconcileStatus as ReconcileStatus)
        : undefined,
    dateBasis: route.query.dateBasis === 'EVENT' ? 'EVENT' : 'CREATED',
    supplierId: typeof route.query.supplierId === 'string' ? route.query.supplierId : undefined,
    startDate: typeof route.query.startDate === 'string' ? route.query.startDate : undefined,
    endDate: typeof route.query.endDate === 'string' ? route.query.endDate : undefined,
  },
  purchases,
)
onMounted(load)
function tagType(status: PurchaseStatus) {
  return status === 'IN_STOCK'
    ? 'success'
    : status === 'PENDING'
      ? 'warning'
      : status === 'REJECTED'
        ? 'danger'
        : 'info'
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">PURCHASING</div>
      <h1>{{ review ? '入库审核' : '采购单管理' }}</h1>
      <p>
        {{ auth.user?.roleCode === 'ADMIN' ? '查看全店采购' : '仅显示本人经办采购' }} ·
        草稿和提交均不增加库存，审核通过后入库。
      </p>
    </div>
    <div class="query-bar">
      <router-link v-if="auth.can('purchase.order.reconcile')" to="/purchases/summary"
        ><el-button>供应商核对汇总</el-button></router-link
      ><router-link v-if="auth.can('purchase.order.create')" to="/purchases/new"
        ><el-button type="primary">新建采购</el-button></router-link
      >
    </div>
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="单号 / 供应商名称"
        clearable
        aria-label="采购查询关键词"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable aria-label="采购状态"
        ><el-option
          v-for="(name, code) in purchaseStatuses"
          :key="code"
          :label="name"
          :value="code"
      /></el-select>
      <el-select
        v-model="query.reconcileStatus"
        placeholder="全部核对状态"
        clearable
        aria-label="核对状态"
        ><el-option
          v-for="(name, code) in reconcileStatuses"
          :key="code"
          :label="name"
          :value="code"
      /></el-select>
      <el-select v-model="query.dateBasis" aria-label="日期口径"
        ><el-option label="创建日期" value="CREATED" /><el-option
          label="入库日期"
          value="STOCKED" /><el-option label="入库/撤销事件日期" value="EVENT"
      /></el-select>
      <el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="开始日期"
        aria-label="开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="结束日期"
        aria-label="结束日期"
      />
      <el-button type="primary" native-type="submit">查询</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button :loading="loading" @click="load">刷新</el-button>
    </form>
    <el-alert
      v-if="query.supplierId"
      title="已按供应商汇总下钻，日期和权限范围与汇总保持一致。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"
      ><el-button text @click="load">重试</el-button></el-alert
    >
    <el-table v-loading="loading" :data="rows" empty-text="暂无符合条件的采购单" row-key="id">
      <el-table-column label="采购单号 / 供应商" min-width="280" fixed
        ><template #default="{ row }"
          ><router-link :to="`/purchases/${row.id}`">{{ row.orderNo }}</router-link>
          <div class="muted">{{ row.supplierName }}</div></template
        ></el-table-column
      >
      <el-table-column prop="operatorName" label="经办人" width="100" />
      <el-table-column label="数量 / 实收" width="115"
        ><template #default="{ row }"
          >{{ row.totalQty }} / {{ row.totalReceivedQty }}</template
        ></el-table-column
      >
      <el-table-column prop="totalAmount" label="金额（元）" width="130" align="right" />
      <el-table-column label="状态" width="120"
        ><template #default="{ row }"
          ><el-tag :type="tagType(row.status)">{{
            purchaseStatuses[row.status as PurchaseStatus]
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="核对状态" width="110"
        ><template #default="{ row }">{{
          reconcileStatuses[row.reconcileStatus as keyof typeof reconcileStatuses]
        }}</template></el-table-column
      >
      <el-table-column :label="review ? '提交时间（北京）' : '创建时间（北京）'" min-width="175"
        ><template #default="{ row }">{{
          purchaseTime(review ? row.submittedAt : row.createdAt)
        }}</template></el-table-column
      >
      <el-table-column label="操作" fixed="right" width="100"
        ><template #default="{ row }"
          ><router-link :to="`/purchases/${row.id}`">{{
            review && row.status === 'PENDING' ? '查看并审核' : '查看详情'
          }}</router-link></template
        ></el-table-column
      >
    </el-table>
    <div class="table-footer">
      <span>共 {{ total }} 张采购单</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </section>
</template>
