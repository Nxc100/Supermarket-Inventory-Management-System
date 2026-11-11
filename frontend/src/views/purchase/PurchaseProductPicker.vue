<script setup lang="ts">
import { watch } from 'vue'
import { purchaseProducts } from '../../api/products'
import { usePagedList } from '../../composables/usePagedList'
import type { ProductQuery, PurchaseProduct } from '../../types/catalog'
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  selected: [product: PurchaseProduct]
}>()
const { query, rows, total, loading, error, load, search } = usePagedList<
  PurchaseProduct,
  ProductQuery
>({ keyword: '', pageNum: 1, pageSize: 10 }, purchaseProducts)
watch(
  () => props.modelValue,
  (open) => {
    if (open) void load()
  },
)
function select(product: PurchaseProduct) {
  emit('selected', product)
  emit('update:modelValue', false)
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    title="添加采购商品"
    width="900px"
    top="5vh"
    class="archive-editor-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    ><form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="商品编码 / 条码 / 名称"
        aria-label="选品关键词"
        clearable
      /><el-button type="primary" native-type="submit">搜索商品</el-button>
    </form>
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button @click="load">重试</el-button></el-alert
    ><el-table v-loading="loading" :data="rows" empty-text="暂无启用且可采购的商品"
      ><el-table-column label="商品" min-width="200"
        ><template #default="{ row }"
          >{{ row.name }}
          <div class="muted">{{ row.code }} · {{ row.barcode || '无条码' }}</div></template
        ></el-table-column
      ><el-table-column prop="specification" label="规格" /><el-table-column
        prop="unit"
        label="单位"
        width="65"
      /><el-table-column label="参考进价" width="100"
        ><template #default="{ row }">{{
          row.referenceCost ?? '未设置'
        }}</template></el-table-column
      ><el-table-column label="保质期" width="90"
        ><template #default="{ row }">{{
          row.managesExpiry ? '需要登记' : '不管理'
        }}</template></el-table-column
      ><el-table-column label="操作" width="110"
        ><template #default="{ row }"
          ><el-button :disabled="row.frozen" link type="primary" @click="select(row)">{{
            row.frozen ? '盘点冻结' : '添加此商品'
          }}</el-button></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>不同批次请分别添加，最多100行</span
      ><el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :disabled="loading"
        @current-change="load"
        @size-change="search"
      /></div
  ></el-dialog>
</template>
