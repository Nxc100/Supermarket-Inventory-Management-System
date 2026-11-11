<script setup lang="ts">
import { onMounted } from 'vue'
import { saleProducts } from '../../api/products'
import { usePagedList } from '../../composables/usePagedList'
import type { ProductQuery, SaleProduct } from '../../types/catalog'
import ArchiveImage from '../../components/ArchiveImage.vue'
const props = defineProps<{ disabled: boolean }>()
const emit = defineEmits<{ selected: [product: SaleProduct] }>()
const { query, rows, total, loading, error, load } = usePagedList<SaleProduct, ProductQuery>(
  { keyword: '', pageNum: 1, pageSize: 10 },
  saleProducts,
)
async function search() {
  if (loading.value) return
  query.pageNum = 1
  const keyword = query.keyword.trim()
  await load()
  if (props.disabled || query.keyword.trim() !== keyword) return
  const exact = rows.value.find((p) => p.barcode === keyword)
  if (exact && !exact.frozen && exact.saleableQty > 0) emit('selected', exact)
}
function select(product: SaleProduct) {
  if (!props.disabled && !product.frozen && product.saleableQty > 0) emit('selected', product)
}
function resizeResults() {
  query.pageNum = 1
  void load()
}
defineExpose({ refresh: load })
onMounted(load)
</script>
<template>
  <section class="panel">
    <h2>查找销售商品</h2>
    <form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        aria-label="销售商品关键词"
        placeholder="输入条码，或商品编码 / 名称"
        clearable
        :disabled="disabled"
      /><el-button type="primary" native-type="submit" :loading="loading" :disabled="disabled"
        >搜索商品</el-button
      ><el-button :loading="loading" :disabled="disabled" @click="load">刷新可售量</el-button>
    </form>
    <p class="muted">条码精确匹配后回车可直接加入。售价由系统提供；库存以确认收款时校验为准。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    ><el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      empty-text="未找到启用且可销售商品，请核实条码或商品状态"
      ><el-table-column label="商品" min-width="220"
        ><template #default="{ row }"
          ><div class="sale-product">
            <ArchiveImage :path="row.imagePath" :size="42" />
            <div>
              <strong>{{ row.name }}</strong>
              <div class="muted">{{ row.code }} / {{ row.barcode || '无条码' }}</div>
              <small class="muted">{{ row.specification || '无规格' }} · {{ row.unit }}</small>
            </div>
          </div></template
        ></el-table-column
      ><el-table-column label="售价（元）" width="105" align="right"
        ><template #default="{ row }">{{ row.salePrice }}</template></el-table-column
      ><el-table-column label="可售数量" width="105" align="right"
        ><template #default="{ row }"
          >{{ row.saleableQty }} {{ row.unit }}</template
        ></el-table-column
      ><el-table-column label="操作" width="155"
        ><template #default="{ row }"
          ><el-button
            :disabled="disabled || row.frozen || row.saleableQty < 1"
            link
            type="primary"
            @click="select(row)"
            >{{
              row.frozen ? '盘点中暂不可销售' : row.saleableQty < 1 ? '无可售库存' : '加入开单'
            }}</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>共 {{ total }} 种商品</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes,prev,pager,next"
        :disabled="disabled || loading"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="resizeResults"
      />
    </div>
  </section>
</template>
<style scoped>
.sale-product {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
