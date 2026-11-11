<script setup lang="ts">
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { supplierOptions } from '../../api/purchases'
import { usePagedList } from '../../composables/usePagedList'
import { useAuth } from '../../stores/auth'
import type { PageQuery } from '../../types/api'
import type { SupplierOption } from '../../types/purchase'
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  selected: [supplier: SupplierOption]
}>()
const auth = useAuth()
const { query, rows, total, loading, error, load, search } = usePagedList<
  SupplierOption,
  PageQuery
>({ keyword: '', enabled: true, pageNum: 1, pageSize: 10 }, supplierOptions)
watch(
  () => props.modelValue,
  (open) => {
    if (open) void load()
  },
)
function refreshOnReturn() {
  if (props.modelValue) void load()
}
onMounted(() => window.addEventListener('focus', refreshOnReturn))
onBeforeUnmount(() => window.removeEventListener('focus', refreshOnReturn))
function select(supplier: SupplierOption) {
  emit('selected', supplier)
  emit('update:modelValue', false)
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    title="选择启用供应商"
    width="700px"
    top="5vh"
    class="archive-editor-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    ><form class="query-bar" @submit.prevent="search">
      <el-input
        v-model="query.keyword"
        placeholder="供应商编码 / 名称"
        aria-label="供应商关键词"
        clearable
      /><el-button type="primary" native-type="submit">搜索供应商</el-button
      ><el-button :loading="loading" @click="load">刷新选项</el-button>
    </form>
    <p class="muted">维护资料后返回此页，选项会自动刷新。</p>
    <router-link
      v-if="auth.can('base.supplier')"
      to="/catalog/suppliers"
      target="_blank"
      rel="opener"
      ><el-button>维护供应商（新窗口）</el-button></router-link
    ><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-table
      v-loading="loading"
      :data="rows"
      empty-text="没有启用供应商，请先维护供应商"
      ><el-table-column prop="code" label="编码" width="180" /><el-table-column
        prop="name"
        label="供应商名称"
        min-width="200"
      /><el-table-column label="操作" width="100"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="select(row)">选择供应商</el-button></template
        ></el-table-column
      ></el-table
    >
    <div class="table-footer">
      <span>仅显示启用档案</span
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
