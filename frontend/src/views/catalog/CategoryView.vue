<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import * as api from '../../api/catalog'
import type { Category, CategoryInput, Id, PageQuery } from '../../types/api'
import { useAuth } from '../../stores/auth'
import { showError } from '../../utils/feedback'
import { dateTime } from '../../utils/format'
import { useDirtyGuard } from '../../composables/useDirtyGuard'

const auth = useAuth()
const query = reactive<PageQuery>({ keyword: '', enabled: undefined, pageNum: 1, pageSize: 10 })
const rows = ref<Category[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
let sequence = 0
const dialog = ref(false)
const editingId = ref<Id | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<CategoryInput>({ code: '', name: '', sortOrder: 0 })
const baseline = ref('')
const detail = ref<Category | null>(null)
const dirty = computed(() => dialog.value && JSON.stringify(form) !== baseline.value)
useDirtyGuard(dirty)
const rules: FormRules = {
  code: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{2,20}$/, message: '2～20位字母数字或下划线', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 30, message: '名称为2～30个字符', trigger: 'blur' },
  ],
  sortOrder: [
    { required: true, type: 'integer', min: 0, message: '请输入非负整数', trigger: 'change' },
  ],
}
async function load() {
  const current = ++sequence
  loading.value = true
  loadError.value = ''
  rows.value = []
  try {
    const result = await api.categories({ ...query })
    if (current !== sequence) return
    rows.value = result.records
    total.value = result.total
    if (!rows.value.length && query.pageNum > 1 && total.value > 0) {
      query.pageNum = Math.ceil(total.value / query.pageSize)
      await load()
    }
  } catch (error) {
    if (current === sequence) loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    if (current === sequence) loading.value = false
  }
}
function search() {
  query.pageNum = 1
  void load()
}
function reset() {
  Object.assign(query, { keyword: '', enabled: undefined, pageNum: 1, pageSize: 10 })
  void load()
}
function open(row?: Category) {
  editingId.value = row?.id ?? null
  Object.assign(form, {
    code: row?.code ?? '',
    name: row?.name ?? '',
    sortOrder: row?.sortOrder ?? 0,
  })
  baseline.value = JSON.stringify(form)
  dialog.value = true
  formRef.value?.clearValidate()
}
async function close(done?: () => void) {
  if (saving.value) return
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('分类资料尚未保存，确认放弃修改？', '未保存的修改', {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  dialog.value = false
  done?.()
}
async function save() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    const input = { ...form, code: form.code.trim(), name: form.name.trim() }
    const saved = editingId.value
      ? await api.updateCategory(editingId.value, input)
      : await api.createCategory(input)
    dialog.value = false
    ElMessage.success(`分类 ${saved.code} 已保存`)
    await load()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}
async function status(row: Category) {
  try {
    await ElMessageBox.confirm(
      `${row.enabled ? '停用' : '启用'}分类「${row.name}」（${row.code}）？停用不影响已有商品。`,
      '确认分类状态',
      {
        confirmButtonText: row.enabled ? '停用分类' : '启用分类',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await api.setCategoryStatus(row.id, !row.enabled)
    ElMessage.success('分类状态已更新')
    await load()
  } catch (error) {
    showError(error)
  }
}
async function remove(row: Category) {
  try {
    await ElMessageBox.confirm(
      `删除分类「${row.name}」（${row.code}）？删除后无法恢复。`,
      '删除分类',
      { confirmButtonText: '删除分类', cancelButtonText: '取消', type: 'warning' },
    )
    await api.deleteCategory(row.id)
    ElMessage.success('分类已删除')
    await load()
  } catch (error) {
    showError(error)
  }
}
async function view(row: Category) {
  try {
    detail.value = await api.category(row.id)
  } catch (error) {
    showError(error)
  }
}
onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <div class="eyebrow">CATALOG / CATEGORIES</div>
        <h1>商品分类</h1>
        <p>统一商品归属，让采购、库存与统计保持清晰。</p>
      </div>
      <el-button v-if="auth.can('base.category.create')" type="primary" :icon="Plus" @click="open()"
        >新增分类</el-button
      >
    </div>
    <div class="panel">
      <el-form class="query-bar" @submit.prevent="search"
        ><el-input
          v-model="query.keyword"
          placeholder="搜索分类编码或名称"
          aria-label="分类关键词"
          clearable
          :prefix-icon="Search"
          maxlength="100"
        /><el-select v-model="query.enabled" placeholder="全部状态" aria-label="分类状态" clearable
          ><el-option label="已启用" :value="true" /><el-option
            label="已停用"
            :value="false" /></el-select
        ><el-button type="primary" native-type="submit">查询</el-button
        ><el-button :icon="Refresh" @click="reset">重置</el-button></el-form
      >
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon
        ><el-button text type="primary" @click="load">重试</el-button></el-alert
      >
      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的分类">
        <el-table-column prop="code" label="分类编码" min-width="130" /><el-table-column
          prop="name"
          label="分类名称"
          min-width="150"
        />
        <el-table-column prop="sortOrder" label="排序值" width="90" /><el-table-column
          prop="productCount"
          label="商品数"
          width="90"
        />
        <el-table-column label="启用状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{
              row.enabled ? '已启用' : '已停用'
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="创建时间" min-width="170"
          ><template #default="{ row }">{{ dateTime(row.createdAt) }}</template></el-table-column
        >
        <el-table-column label="操作" width="240" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="view(row)">详情</el-button
            ><el-button v-if="auth.can('base.category.edit')" link type="primary" @click="open(row)"
              >编辑</el-button
            ><el-button
              v-if="auth.can('base.category.enable')"
              link
              type="primary"
              @click="status(row)"
              >{{ row.enabled ? '停用' : '启用' }}</el-button
            ><el-tooltip :disabled="row.productCount === 0" content="已有商品引用，请使用停用"
              ><span
                ><el-button
                  v-if="auth.can('base.category.delete')"
                  link
                  type="danger"
                  :disabled="row.productCount > 0"
                  @click="remove(row)"
                  >删除</el-button
                ></span
              ></el-tooltip
            ></template
          ></el-table-column
        >
      </el-table>
      <div class="table-footer">
        <span>共 {{ total }} 个分类 · 已引用的分类保留历史关联</span
        ><el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          :disabled="loading"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
    <el-dialog
      v-model="dialog"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="480px"
      :before-close="close"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="save"
        ><el-form-item label="分类编码" prop="code"
          ><el-input
            v-model="form.code"
            placeholder="如 BEVERAGES"
            maxlength="20"
            :disabled="saving" /></el-form-item
        ><el-form-item label="分类名称" prop="name"
          ><el-input
            v-model="form.name"
            placeholder="如 饮料乳品"
            maxlength="30"
            :disabled="saving" /></el-form-item
        ><el-form-item label="排序值" prop="sortOrder"
          ><el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="2147483647"
            :precision="0"
            :disabled="saving"
        /></el-form-item>
        <p class="muted">新分类默认启用；分类启停不会改变已有商品状态。</p></el-form
      >
      <template #footer
        ><el-button :disabled="saving" @click="close()">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存分类</el-button></template
      >
    </el-dialog>
    <el-dialog
      :model-value="detail !== null"
      title="分类详情"
      width="480px"
      @update:model-value="detail = null"
      ><el-descriptions v-if="detail" :column="1" border
        ><el-descriptions-item label="分类编码">{{ detail.code }}</el-descriptions-item
        ><el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item
        ><el-descriptions-item label="排序值">{{ detail.sortOrder }}</el-descriptions-item
        ><el-descriptions-item label="商品数">{{ detail.productCount }}</el-descriptions-item
        ><el-descriptions-item label="状态">{{
          detail.enabled ? '已启用' : '已停用'
        }}</el-descriptions-item
        ><el-descriptions-item label="创建时间">{{
          dateTime(detail.createdAt)
        }}</el-descriptions-item></el-descriptions
      ></el-dialog
    >
  </section>
</template>
