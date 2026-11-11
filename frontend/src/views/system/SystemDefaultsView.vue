<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuth } from '../../stores/auth'
import { confirmUnsaved, useDirtyGuard } from '../../composables/useDirtyGuard'
import { systemDefaults, saveSystemDefaults, type SystemDefaults } from '../../api/system'
const auth = useAuth(),
  original = ref<SystemDefaults | null>(null),
  form = reactive<SystemDefaults>({ stockLowerLimit: 0, nearExpiryDays: 0 }),
  loading = ref(false),
  saving = ref(false),
  error = ref(''),
  formRef = ref<FormInstance>()
const dirty = computed(
  () => !!original.value && JSON.stringify(form) !== JSON.stringify(original.value),
)
useDirtyGuard(dirty)
const rules: FormRules = {
  stockLowerLimit: [
    { required: true, message: '请输入默认库存下限', trigger: 'blur' },
    { type: 'integer', min: 0, max: 999999, message: '请输入0～999999之间的整数', trigger: 'blur' },
  ],
  nearExpiryDays: [
    { required: true, message: '请输入默认临期天数', trigger: 'blur' },
    { type: 'integer', min: 0, max: 3650, message: '请输入0～3650之间的整数', trigger: 'blur' },
  ],
}
async function load() {
  if (loading.value || saving.value) return
  if (dirty.value && !(await confirmUnsaved())) return
  loading.value = true
  error.value = ''
  try {
    const result = await systemDefaults()
    original.value = { ...result }
    Object.assign(form, result)
    formRef.value?.clearValidate()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '参数加载失败'
  } finally {
    loading.value = false
  }
}
function cancel() {
  if (original.value) Object.assign(form, original.value)
  formRef.value?.clearValidate()
  error.value = ''
}
async function save() {
  if (
    !original.value ||
    saving.value ||
    loading.value ||
    !(await formRef.value?.validate().catch(() => false))
  )
    return
  saving.value = true
  error.value = ''
  try {
    const result = await saveSystemDefaults({ ...form }, original.value)
    original.value = { ...result }
    Object.assign(form, result)
    ElMessage.success('默认参数已保存，既有商品设置保持各自值')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败，请刷新核对后重试'
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">BUSINESS DEFAULTS</div>
      <h1>业务参数</h1>
      <p>作为之后新建商品的初始值；既有商品保留各自的库存下限和临期设置。</p>
    </div>
    <el-button :loading="loading" :disabled="saving" @click="load">刷新参数</el-button>
  </div>
  <section v-loading="loading" class="panel defaults-panel">
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button :disabled="saving" @click="load">刷新并核对参数</el-button></el-alert
    >
    <el-form
      v-if="original"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      :disabled="loading || saving || !auth.can('system.config.save')"
      @submit.prevent="save"
    >
      <el-form-item label="默认库存下限" prop="stockLowerLimit"
        ><el-input-number
          v-model="form.stockLowerLimit"
          :min="0"
          :max="999999"
          :precision="0"
          :controls="false"
          aria-label="默认库存下限"
      /></el-form-item>
      <el-form-item label="默认临期天数" prop="nearExpiryDays"
        ><el-input-number
          v-model="form.nearExpiryDays"
          :min="0"
          :max="3650"
          :precision="0"
          :controls="false"
          aria-label="默认临期天数"
      /></el-form-item>
      <p class="muted">0 为有效值。临期天数适用于管理保质期的商品，到期当天仍按有效批次处理。</p>
      <div class="query-bar">
        <el-button
          v-if="auth.can('system.config.save')"
          native-type="submit"
          type="primary"
          :disabled="!dirty"
          :loading="saving"
          >保存默认参数</el-button
        ><el-button :disabled="!dirty" @click="cancel">取消修改</el-button>
      </div> </el-form
    ><el-empty v-else-if="!loading" description="默认参数尚未加载，请重试" />
  </section>
</template>
<style scoped>
.defaults-panel {
  max-width: 760px;
}
.defaults-panel .el-form {
  margin-top: 20px;
  max-width: 440px;
}
.defaults-panel .el-input-number {
  width: 100%;
}
.defaults-panel p {
  line-height: 1.8;
}
</style>
