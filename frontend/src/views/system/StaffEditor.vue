<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import { ApiError } from '../../api/http'
import * as api from '../../api/staff'
import { staffRoles, type Staff, type StaffRole } from '../../api/staff'
export type EditorMode = 'CREATE' | 'EDIT' | 'RESET'
const props = defineProps<{ modelValue: boolean; mode: EditorMode; account: Staff | null }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [account: Staff, invalidate: boolean]
}>()
const form = reactive({
    username: '',
    realName: '',
    phone: '',
    role: 'CASHIER' as StaffRole,
    password: '',
    confirmPassword: '',
  }),
  formRef = ref<FormInstance>(),
  saving = ref(false),
  error = ref(''),
  baseline = ref('')
const title = computed(() =>
  props.mode === 'CREATE' ? '新增用户' : props.mode === 'EDIT' ? '编辑用户' : '重置用户密码',
)
const dirty = computed(() => props.modelValue && JSON.stringify(form) !== baseline.value)
const pending = computed(
  () => !!props.account && props.account.unfinishedPurchases + props.account.activeStocktakes > 0,
)
useDirtyGuard(dirty)
const rules = computed<FormRules>(() => ({
  username:
    props.mode === 'CREATE'
      ? [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          {
            pattern: /^[A-Za-z0-9_]{4,20}$/,
            message: '用户名须为4～20位字母、数字或下划线',
            trigger: 'blur',
          },
        ]
      : [],
  realName:
    props.mode !== 'RESET'
      ? [
          { required: true, message: '请输入姓名', trigger: 'blur' },
          { min: 2, max: 20, message: '姓名为2～20个字符', trigger: 'blur' },
        ]
      : [],
  phone:
    props.mode !== 'RESET'
      ? [
          {
            validator: (_r, value: string, cb) =>
              cb(
                value && !/^1[3-9][0-9]{9}$/.test(value)
                  ? new Error('请输入11位中国大陆手机号')
                  : undefined,
              ),
            trigger: 'blur',
          },
        ]
      : [],
  role:
    props.mode !== 'RESET' ? [{ required: true, message: '请选择岗位', trigger: 'change' }] : [],
  password:
    props.mode !== 'EDIT'
      ? [
          { required: true, message: '请输入密码', trigger: 'blur' },
          {
            pattern: /^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$/,
            message: '密码须为8～20位且包含字母与数字',
            trigger: 'blur',
          },
        ]
      : [],
  confirmPassword:
    props.mode !== 'EDIT'
      ? [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          {
            validator: (_r, value: string, cb) =>
              cb(value !== form.password ? new Error('两次密码须一致') : undefined),
            trigger: 'blur',
          },
        ]
      : [],
}))
watch(
  () => props.modelValue,
  async (opened) => {
    if (!opened) return
    const row = props.account
    Object.assign(form, {
      username: row?.username ?? '',
      realName: row?.realName ?? '',
      phone: row?.phone ?? '',
      role: row?.roleCode ?? 'CASHIER',
      password: '',
      confirmPassword: '',
    })
    baseline.value = JSON.stringify(form)
    error.value = ''
    await nextTick()
    formRef.value?.clearValidate()
  },
)
function clear() {
  form.password = ''
  form.confirmPassword = ''
  emit('update:modelValue', false)
}
async function close(done?: () => void) {
  if (saving.value) return
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('未保存的账号修改将丢失，确认放弃？', '未保存的修改', {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  clear()
  done?.()
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false))) return
  if (props.mode === 'RESET') {
    try {
      await ElMessageBox.confirm(
        `确认重置账号 ${props.account?.username} 的密码？成功后其原登录凭证失效，请线下告知使用者。`,
        '确认重置密码',
        { confirmButtonText: '确认重置密码', cancelButtonText: '取消', type: 'warning' },
      )
    } catch {
      return
    }
  }
  saving.value = true
  error.value = ''
  try {
    const account = props.account
    let result: Staff
    if (props.mode === 'CREATE')
      result = await api.createStaff({
        username: form.username.trim(),
        realName: form.realName.trim(),
        phone: form.phone.trim() || null,
        role: form.role,
        password: form.password,
        confirmPassword: form.confirmPassword,
      })
    else if (account && props.mode === 'EDIT')
      result = await api.editStaff(account.id, {
        realName: form.realName.trim(),
        phone: form.phone.trim() || null,
        role: form.role,
        expectedUpdatedAt: account.updatedAt,
      })
    else if (account)
      result = await api.resetStaffPassword(account, form.password, form.confirmPassword)
    else throw new Error('目标账号未加载，请重新打开编辑窗口')
    const invalidate =
      props.mode === 'RESET' || (props.mode === 'EDIT' && account?.roleCode !== result.roleCode)
    clear()
    ElMessage.success(
      props.mode === 'RESET'
        ? `账号 ${result.username} 密码已重置，请线下告知使用者`
        : `账号 ${result.username} 已保存`,
    )
    emit('saved', result, invalidate)
  } catch (e) {
    error.value =
      e instanceof ApiError && (e.status === 0 || e.status >= 500)
        ? '本次保存结果尚未核实，请先取消并刷新用户列表，核对目标账号后再决定是否重试'
        : e instanceof Error
          ? e.message
          : '保存失败，请刷新核对后重试'
  } finally {
    saving.value = false
  }
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="620px"
    :before-close="close"
    :close-on-click-modal="false"
    class="archive-editor-dialog"
  >
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="account" class="muted">
      目标账号：{{ account.username }} · {{ account.roleName }} ·
      {{ account.enabled ? '已启用' : '已停用' }}
    </p>
    <p v-if="mode === 'RESET'" class="muted">
      重置成功会立即使该账号的原登录失效。此处不发送短信或邮件。
    </p>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      :disabled="saving"
      @submit.prevent="save"
    >
      <template v-if="mode !== 'RESET'">
        <el-form-item label="用户名" prop="username"
          ><el-input
            v-model="form.username"
            :disabled="mode === 'EDIT'"
            autocomplete="off"
            maxlength="20"
            aria-label="账号用户名"
        /></el-form-item>
        <el-form-item label="姓名" prop="realName"
          ><el-input v-model="form.realName" maxlength="20" aria-label="账号姓名"
        /></el-form-item>
        <el-form-item label="手机号" prop="phone"
          ><el-input v-model="form.phone" maxlength="11" aria-label="账号手机号"
        /></el-form-item>
        <el-form-item label="岗位" prop="role"
          ><el-select
            v-model="form.role"
            aria-label="账号岗位"
            :disabled="mode === 'EDIT' && pending"
            ><el-option
              v-for="(label, value) in staffRoles"
              :key="value"
              :value="value"
              :label="label" /></el-select
        ></el-form-item>
        <p v-if="mode === 'EDIT' && pending" class="muted">
          存在未完成采购或活动盘点，暂不能变更岗位；请先从用户列表进入未完成任务处理。
        </p>
        <p v-else-if="mode === 'EDIT'" class="muted">
          变更岗位将使原登录失效；历史业务保留原经办人快照。
        </p>
      </template>
      <template v-if="mode !== 'EDIT'"
        ><el-form-item :label="mode === 'CREATE' ? '初始密码' : '新密码'" prop="password"
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="new-password"
            maxlength="20"
            aria-label="账号密码" /></el-form-item
        ><el-form-item label="确认密码" prop="confirmPassword"
          ><el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            maxlength="20"
            aria-label="确认账号密码" /></el-form-item
      ></template>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="close()">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">{{
        mode === 'RESET' ? '重置密码' : '保存用户'
      }}</el-button></template
    >
  </el-dialog>
</template>
<style scoped>
.el-alert {
  margin-bottom: 20px;
}
.el-select {
  width: 100%;
}
.muted {
  line-height: 1.7;
}
</style>
