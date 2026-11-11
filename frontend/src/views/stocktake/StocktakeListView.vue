<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { stocktakes } from '../../api/stocktakes'
import { usePagedList } from '../../composables/usePagedList'
import { useAuth } from '../../stores/auth'
import { businessTime } from '../../utils/format'
import {
  stocktakeStatuses,
  type StocktakeQuery,
  type StocktakeSummary,
  type StocktakeStatus,
} from '../../types/stocktake'
const auth = useAuth(),
  route = useRoute()
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  StocktakeSummary,
  StocktakeQuery
>(
  {
    keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
    pageNum: 1,
    pageSize: 10,
    status:
      typeof route.query.status === 'string' && Object.hasOwn(stocktakeStatuses, route.query.status)
        ? (route.query.status as StocktakeStatus)
        : undefined,
  },
  stocktakes,
)
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">STOCKTAKE</div>
      <h1>库存盘点</h1>
      <p>
        {{ auth.user?.roleCode === 'ADMIN' ? '全店盘点任务' : '本人盘点任务' }} ·
        活动任务按商品整体冻结，完成或关闭后释放。
      </p>
    </div>
    <router-link v-if="auth.can('inventory.stocktake.start')" to="/stocktakes/new"
      ><el-button type="primary">新建盘点</el-button></router-link
    >
  </div>
  <section class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="盘点关键词"
        placeholder="盘点单号 / 商品编码 / 名称"
        clearable
      />
      <el-select v-model="query.status" aria-label="盘点状态" placeholder="全部状态" clearable
        ><el-option
          v-for="(label, value) in stocktakeStatuses"
          :key="value"
          :value="value"
          :label="label"
      /></el-select>
      <el-date-picker
        v-model="query.startDate"
        type="date"
        value-format="YYYY-MM-DD"
        aria-label="开始盘点日期"
        placeholder="开始日期"
      /><el-date-picker
        v-model="query.endDate"
        type="date"
        value-format="YYYY-MM-DD"
        aria-label="结束盘点日期"
        placeholder="结束日期"
      />
      <el-button native-type="submit" type="primary" :loading="loading">查询盘点</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button :loading="loading" @click="load">刷新</el-button>
    </form>
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    >
    <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的盘点任务"
      ><el-table-column prop="orderNo" label="盘点单号" min-width="265" /><el-table-column
        label="状态"
        width="95"
        ><template #default="{ row }"
          ><el-tag>{{
            stocktakeStatuses[row.status as keyof typeof stocktakeStatuses]
          }}</el-tag></template
        ></el-table-column
      ><el-table-column prop="operatorName" label="经办人" width="110" /><el-table-column
        label="清点进度"
        width="150"
        ><template #default="{ row }"
          >{{ row.productCount }} 种 · {{ row.countedCount }}/{{ row.batchCount }} 批次</template
        ></el-table-column
      ><el-table-column label="开始时间（北京）" width="185"
        ><template #default="{ row }">{{ businessTime(row.startedAt) }}</template></el-table-column
      ><el-table-column label="差异数量" width="110" align="right"
        ><template #default="{ row }">{{
          row.differenceQty === null ? '未填完' : row.differenceQty
        }}</template></el-table-column
      ><el-table-column label="操作" fixed="right" width="90"
        ><template #default="{ row }"
          ><router-link :to="`/stocktakes/${row.id}`"
            ><el-button link type="primary">查看盘点</el-button></router-link
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 张盘点</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="sizes,prev,pager,next"
        :disabled="loading"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </section>
</template>
