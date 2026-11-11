<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stockInventory } from '../../api/inventory'
import { startStocktake } from '../../api/stocktakes'
import { usePagedList } from '../../composables/usePagedList'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import { showError } from '../../utils/feedback'
import { ApiError } from '../../api/http'
import type { StockProduct, StockQuery } from '../../types/stock'
const router = useRouter(),
  route = useRoute()
const selected = ref<StockProduct[]>([]),
  remark = ref(''),
  busy = ref(false),
  unknownResult = ref(false),
  completed = ref(false)
const resultQuery = ref('')
const { query, rows, total, loading, error, load, search } = usePagedList<StockProduct, StockQuery>(
  {
    keyword:
      typeof route.query.productCode === 'string'
        ? route.query.productCode
        : typeof route.query.keyword === 'string'
          ? route.query.keyword
          : '',
    pageNum: 1,
    pageSize: 10,
  },
  stockInventory,
)
useDirtyGuard(computed(() => !completed.value && (selected.value.length > 0 || !!remark.value)))
function add(row: StockProduct) {
  if (!selected.value.some((p) => p.id === row.id) && selected.value.length < 100)
    selected.value.push(row)
}
async function start() {
  if (busy.value || unknownResult.value || !selected.value.length) return
  busy.value = true
  try {
    await ElMessageBox.confirm(
      `确认开始 ${selected.value.length} 种商品的盘点？相关商品将整体冻结，暂时不能采购入库、销售、纠错或撤销；完成或关闭后释放。`,
      '开始盘点',
      { confirmButtonText: '开始盘点', cancelButtonText: '取消', type: 'warning' },
    )
    resultQuery.value = selected.value[0]?.code ?? ''
    const result = await startStocktake(
      selected.value.map((p) => p.id),
      remark.value,
    )
    completed.value = true
    ElMessage.success('盘点已开始，商品已冻结')
    await router.replace(`/stocktakes/${result.order.id}`)
  } catch (e) {
    if (e instanceof ApiError && (e.status === 0 || e.status >= 500)) unknownResult.value = true
    else showError(e)
  } finally {
    busy.value = false
  }
}
onMounted(async () => {
  await load()
  if (typeof route.query.productCode === 'string') {
    const product = rows.value.find((p) => p.code === route.query.productCode)
    if (product && !product.frozen) add(product)
  }
})
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">START STOCKTAKE</div>
      <h1>新建盘点</h1>
      <p>
        按商品整体选取有效来源批次，包括零库存批次。停用或过期商品仍可盘点；没有来源的商品须先采购。
      </p>
    </div>
    <router-link to="/stocktakes"><el-button>盘点列表</el-button></router-link>
  </div>
  <el-alert
    v-if="unknownResult"
    title="本次盘点发起结果待核实"
    type="warning"
    :closable="false"
    show-icon
  >
    <p>
      请求可能已经创建任务并冻结商品。当前范围已保留，请先到盘点列表按商品、经办人和开始时间核对，勿重复发起。
    </p>
    <router-link :to="{ path: '/stocktakes', query: { keyword: resultQuery } }"
      ><el-button>去列表核实盘点结果</el-button></router-link
    >
  </el-alert>
  <section v-loading="busy" class="panel">
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="盘点选品关键词"
        placeholder="商品编码 / 条码 / 名称"
        clearable
      /><el-button native-type="submit" type="primary" :loading="loading">搜索盘点商品</el-button>
    </form>
    <el-alert v-if="error" type="error" :title="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    >
    <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无匹配商品"
      ><el-table-column prop="code" label="商品编码" width="180" /><el-table-column
        prop="name"
        label="商品名称"
        min-width="180"
      /><el-table-column label="账面 / 单位" width="130"
        ><template #default="{ row }">{{ row.bookQty }} {{ row.unit }}</template></el-table-column
      ><el-table-column label="状态" width="160"
        ><template #default="{ row }">{{
          row.frozen ? '已有活动盘点' : row.enabled ? '启用' : '停用，仍可盘点'
        }}</template></el-table-column
      ><el-table-column label="操作" width="110"
        ><template #default="{ row }"
          ><el-button
            link
            type="primary"
            :disabled="
              row.frozen || selected.some((p) => p.id === row.id) || selected.length >= 100
            "
            @click="add(row)"
            >{{
              row.frozen
                ? '盘点冻结'
                : selected.some((p) => p.id === row.id)
                  ? '已选取'
                  : '选取商品'
            }}</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>最多100种商品 / 2000个批次</span
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
    <h2>本次范围 · {{ selected.length }} 种</h2>
    <div class="selection-tags">
      <el-tag
        v-for="p in selected"
        :key="p.id"
        closable
        @close="selected = selected.filter((s) => s.id !== p.id)"
        >{{ p.name }} · {{ p.code }}</el-tag
      >
    </div>
    <p v-if="!selected.length" class="muted">先选取需要核实实物的商品。</p>
    <el-input
      v-model="remark"
      type="textarea"
      aria-label="盘点备注"
      maxlength="500"
      show-word-limit
      placeholder="盘点范围及说明（可空）"
    />
    <div class="table-footer">
      <span>开始后实盘数均为空白，须逐批清点。</span
      ><el-button
        type="primary"
        :loading="busy"
        :disabled="!selected.length || unknownResult"
        @click="start"
        >开始盘点</el-button
      >
    </div>
  </section>
</template>
<style scoped>
.selection-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}
</style>
