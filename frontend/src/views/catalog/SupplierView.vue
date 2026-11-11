<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { useAuth } from '../../stores/auth'
import { usePagedList } from '../../composables/usePagedList'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import * as api from '../../api/suppliers'
import type { Supplier, SupplierInput, SupplierQuery } from '../../types/catalog'
import { showError } from '../../utils/feedback'
import { dateTime } from '../../utils/format'
const auth = useAuth()
const { query, rows, total, loading, error, load, search, reset } = usePagedList<
  Supplier,
  SupplierQuery
>({ keyword: '', contactName: '', enabled: undefined, pageNum: 1, pageSize: 10 }, api.suppliers)
const dialog = ref(false),
  saving = ref(false)
const editing = ref<Supplier | null>(null),
  detail = ref<Supplier | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<SupplierInput>({
  name: '',
  contactName: '',
  contactPhone: '',
  address: null,
  remark: null,
})
const baseline = ref('')
const dirty = computed(() => dialog.value && JSON.stringify(form) !== baseline.value)
useDirtyGuard(dirty)
const rules: FormRules = {
  name: [
    { required: true, message: '请输入供应商名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称为2～100个字符', trigger: 'blur' },
  ],
  contactName: [
    { required: true, message: '请输入联系人', trigger: 'blur' },
    { min: 2, max: 30, message: '联系人为2～30个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    {
      pattern: /^[0-9][0-9 -]{4,18}[0-9]$/,
      message: '电话为6～20位数字、空格或短横线',
      trigger: 'blur',
    },
  ],
}
async function open(row?: Supplier) {
  try {
    editing.value = row ? await api.supplier(row.id) : null
    const s = editing.value
    Object.assign(form, {
      name: s?.name ?? '',
      contactName: s?.contactName ?? '',
      contactPhone: s?.contactPhone ?? '',
      address: s?.address ?? null,
      remark: s?.remark ?? null,
    })
    baseline.value = JSON.stringify(form)
    dialog.value = true
    formRef.value?.clearValidate()
  } catch (error) {
    showError(error)
  }
}
async function close(done?: () => void) {
  if (saving.value) return
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('供应商资料尚未保存，确认放弃修改？', '未保存的修改', {
        type: 'warning',
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
      })
    } catch {
      return
    }
  }
  dialog.value = false
  done?.()
}
async function save() {
  if (saving.value) return
  saving.value = true
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return
    const input = {
      ...form,
      name: form.name.trim(),
      contactName: form.contactName.trim(),
      contactPhone: form.contactPhone.trim(),
    }
    const value = editing.value
      ? await api.updateSupplier(editing.value.id, input)
      : await api.createSupplier(input)
    dialog.value = false
    ElMessage.success(`供应商 ${value.code} 已保存`)
    await load()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}
async function view(row: Supplier) {
  try {
    detail.value = await api.supplier(row.id)
  } catch (error) {
    showError(error)
  }
}
async function status(row: Supplier) {
  try {
    await ElMessageBox.confirm(
      `${row.enabled ? '停用' : '启用'}供应商「${row.name}」（${row.code}）？停用后不可用于新采购，历史单据保留。`,
      '供应商状态确认',
      {
        type: 'warning',
        confirmButtonText: row.enabled ? '停用供应商' : '启用供应商',
        cancelButtonText: '取消',
      },
    )
    await api.setSupplierStatus(row.id, !row.enabled)
    ElMessage.success('供应商状态已更新')
    await load()
  } catch (error) {
    showError(error)
  }
}
async function remove(row: Supplier) {
  try {
    await ElMessageBox.confirm(
      `删除无采购引用的供应商「${row.name}」（${row.code}）？`,
      '删除供应商',
      { type: 'warning', confirmButtonText: '删除供应商', cancelButtonText: '取消' },
    )
    await api.deleteSupplier(row.id)
    ElMessage.success('供应商已删除')
    await load()
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
        <div class="eyebrow">CATALOG / SUPPLIERS</div>
        <h1>供应商管理</h1>
        <p>管理员与采购共享档案，历史采购保留当时的供应商信息。</p>
      </div>
      <el-button v-if="auth.can('base.supplier.create')" type="primary" :icon="Plus" @click="open()"
        >新增供应商</el-button
      >
    </div>
    <div class="panel">
      <el-form class="query-bar" @submit.prevent="search"
        ><el-input
          v-model="query.keyword"
          placeholder="搜索供应商编码或名称"
          aria-label="供应商关键词"
          :prefix-icon="Search"
          clearable
          maxlength="100"
        /><el-input
          v-model="query.contactName"
          placeholder="联系人"
          aria-label="联系人筛选"
          clearable
          maxlength="30"
        /><el-select
          v-model="query.enabled"
          aria-label="供应商状态筛选"
          placeholder="全部状态"
          clearable
          @change="search"
          ><el-option label="已启用" :value="true" /><el-option
            label="已停用"
            :value="false" /></el-select
        ><el-button type="primary" native-type="submit">查询</el-button
        ><el-button :icon="Refresh" @click="reset">重置</el-button></el-form
      ><el-alert v-if="error" :title="error" type="error" :closable="false" show-icon
        ><el-button text type="primary" @click="load">重试</el-button></el-alert
      ><el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的供应商"
        ><el-table-column
          prop="name"
          label="供应商名称"
          min-width="200"
          fixed="left"
        /><el-table-column prop="code" label="供应商编码" min-width="220" /><el-table-column
          prop="contactName"
          label="联系人"
          min-width="110"
        /><el-table-column prop="contactPhone" label="联系电话" min-width="150" /><el-table-column
          label="状态"
          width="90"
          ><template #default="{ row }"
            ><el-tag :type="row.enabled ? 'success' : 'info'">{{
              row.enabled ? '已启用' : '已停用'
            }}</el-tag></template
          ></el-table-column
        ><el-table-column label="操作" width="230" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="view(row)">详情</el-button
            ><el-button v-if="auth.can('base.supplier.edit')" link type="primary" @click="open(row)"
              >编辑</el-button
            ><el-button
              v-if="auth.can('base.supplier.enable')"
              link
              type="primary"
              @click="status(row)"
              >{{ row.enabled ? '停用' : '启用' }}</el-button
            ><el-tooltip
              v-if="auth.can('base.supplier.delete')"
              content="已有采购引用，请使用停用"
              :disabled="row.purchaseCount === 0"
              ><span
                ><el-button
                  link
                  type="danger"
                  :disabled="row.purchaseCount > 0"
                  @click="remove(row)"
                  >删除</el-button
                ></span
              ></el-tooltip
            ></template
          ></el-table-column
        ></el-table
      >
      <div class="table-footer">
        <span>共 {{ total }} 家供应商 · 有采购引用的档案不可删除</span
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
      :title="editing ? '编辑供应商' : '新增供应商'"
      class="archive-editor-dialog"
      top="5vh"
      width="560px"
      :before-close="close"
      :close-on-click-modal="false"
      destroy-on-close
      ><el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :disabled="saving"
        label-position="top"
        @submit.prevent="save"
        ><el-form-item label="供应商编码"
          ><el-input :model-value="editing?.code ?? '保存后由系统生成'" disabled /></el-form-item
        ><el-form-item label="供应商名称" prop="name"
          ><el-input v-model="form.name" maxlength="100" /></el-form-item
        ><el-form-item label="联系人" prop="contactName"
          ><el-input v-model="form.contactName" maxlength="30" /></el-form-item
        ><el-form-item label="联系电话" prop="contactPhone"
          ><el-input
            v-model="form.contactPhone"
            maxlength="20"
            placeholder="手机号或含区号的固定电话" /></el-form-item
        ><el-form-item label="地址" prop="address"
          ><el-input v-model="form.address" maxlength="200" /></el-form-item
        ><el-form-item label="备注" prop="remark"
          ><el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit /></el-form-item></el-form
      ><template #footer
        ><el-button :disabled="saving" @click="close()">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存供应商</el-button></template
      ></el-dialog
    >
    <el-dialog
      :model-value="detail !== null"
      title="供应商详情"
      width="560px"
      @update:model-value="detail = null"
      ><el-descriptions v-if="detail" :column="1" border
        ><el-descriptions-item label="编码">{{ detail.code }}</el-descriptions-item
        ><el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item
        ><el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item
        ><el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item
        ><el-descriptions-item label="地址">{{ detail.address || '未填写' }}</el-descriptions-item
        ><el-descriptions-item label="启用状态">{{
          detail.enabled ? '已启用' : '已停用'
        }}</el-descriptions-item
        ><el-descriptions-item label="采购引用数">{{ detail.purchaseCount }}</el-descriptions-item
        ><el-descriptions-item label="备注">{{ detail.remark || '未填写' }}</el-descriptions-item
        ><el-descriptions-item label="创建时间">{{
          dateTime(detail.createdAt)
        }}</el-descriptions-item></el-descriptions
      ></el-dialog
    >
  </section>
</template>
