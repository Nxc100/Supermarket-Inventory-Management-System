<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'
import * as api from '../../api/auth'
import { showError } from '../../utils/feedback'
import { useDirtyGuard } from '../../composables/useDirtyGuard'
import AvatarEditor from '../../components/AvatarEditor.vue'

const auth = useAuth()
const router = useRouter()
const editing = ref(false)
const saving = ref(false)
const changing = ref(false)
const profileForm = ref<FormInstance>()
const passwordForm = ref<FormInstance>()
const form = reactive({ realName: auth.user?.realName ?? '', phone: auth.user?.phone ?? '' })
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const dirty = computed(
  () =>
    (editing.value &&
      (form.realName !== auth.user?.realName || form.phone !== (auth.user?.phone ?? ''))) ||
    Object.values(password).some(Boolean),
)
useDirtyGuard(dirty)
const rules: FormRules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名为2～20个字符', trigger: 'blur' },
  ],
  phone: [{ pattern: /^$|^1[3-9][0-9]{9}$/, message: '请输入11位中国大陆手机号', trigger: 'blur' }],
}
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$/,
      message: '8～20位，至少含字母与数字',
      trigger: 'blur',
    },
    {
      validator: (_rule, value: string, callback) =>
        callback(value === password.oldPassword ? new Error('新密码不能与旧密码相同') : undefined),
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) =>
        callback(value !== password.newPassword ? new Error('两次新密码不一致') : undefined),
      trigger: 'blur',
    },
  ],
}
function cancelEdit() {
  form.realName = auth.user?.realName ?? ''
  form.phone = auth.user?.phone ?? ''
  editing.value = false
  profileForm.value?.clearValidate()
}
async function save() {
  if (!(await profileForm.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    auth.user = await api.updateProfile({
      realName: form.realName.trim(),
      phone: form.phone.trim() || null,
    })
    cancelEdit()
    ElMessage.success('个人资料已保存')
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}
async function changePassword() {
  if (!(await passwordForm.value?.validate().catch(() => false))) return
  changing.value = true
  try {
    await api.changePassword(password)
    Object.assign(password, { oldPassword: '', newPassword: '', confirmPassword: '' })
    auth.clear()
    ElMessage.success('密码已修改，请重新登录')
    await router.replace('/login')
  } catch (error) {
    showError(error)
  } finally {
    changing.value = false
  }
}
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <div class="eyebrow">MY ACCOUNT</div>
        <h1>个人中心</h1>
        <p>管理个人资料与账号安全。</p>
      </div>
    </div>
    <div class="profile-grid">
      <div class="panel identity">
        <AvatarEditor />
        <h2>{{ auth.user?.realName }}</h2>
        <p class="muted">{{ auth.user?.username }}</p>
        <el-tag effect="plain">{{ auth.user?.roleName }}</el-tag>
        <div class="identity-status"><span></span> 账号已启用</div>
      </div>
      <div class="profile-forms">
        <div class="panel">
          <div class="section-title">
            <h2>基本资料</h2>
            <el-button v-if="!editing" text type="primary" @click="editing = true"
              >编辑资料</el-button
            >
          </div>
          <el-form
            ref="profileForm"
            :model="form"
            :rules="rules"
            label-position="top"
            class="profile-form"
            @submit.prevent="save"
          >
            <el-form-item label="用户名"
              ><el-input :model-value="auth.user?.username" disabled
            /></el-form-item>
            <el-form-item label="姓名" prop="realName"
              ><el-input v-model="form.realName" :disabled="!editing || saving" maxlength="20"
            /></el-form-item>
            <el-form-item label="手机号" prop="phone"
              ><el-input
                v-model="form.phone"
                :disabled="!editing || saving"
                placeholder="选填"
                maxlength="11"
            /></el-form-item>
            <div v-if="editing" class="form-actions">
              <el-button :disabled="saving" @click="cancelEdit">取消</el-button
              ><el-button type="primary" native-type="submit" :loading="saving">保存资料</el-button>
            </div>
          </el-form>
        </div>
        <div v-if="auth.can('common.password')" class="panel">
          <h2>修改密码</h2>
          <p class="muted">修改成功后，原登录凭证会失效，需要重新登录。</p>
          <el-form
            ref="passwordForm"
            :model="password"
            :rules="passwordRules"
            label-position="top"
            class="profile-form"
            @submit.prevent="changePassword"
          >
            <el-form-item label="旧密码" prop="oldPassword"
              ><el-input
                v-model="password.oldPassword"
                type="password"
                autocomplete="current-password"
                maxlength="100"
            /></el-form-item>
            <el-form-item label="新密码" prop="newPassword"
              ><el-input
                v-model="password.newPassword"
                type="password"
                autocomplete="new-password"
                placeholder="8～20位，至少含字母与数字"
                maxlength="20"
            /></el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword"
              ><el-input
                v-model="password.confirmPassword"
                type="password"
                autocomplete="new-password"
                maxlength="20"
            /></el-form-item>
            <el-button type="primary" native-type="submit" :loading="changing">修改密码</el-button>
          </el-form>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
  align-items: start;
}
.identity {
  text-align: center;
  padding: 40px 24px;
}
.identity .el-avatar {
  background: #e8efdf;
  color: #607b50;
  font-size: 28px;
}
.identity h2 {
  margin: 24px 0 8px;
}
.identity-status {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
  font-size: var(--font-size-caption);
  color: #738773;
}
.identity-status span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #7ca26b;
  border-radius: 50%;
  margin-right: 6px;
}
.avatar-note {
  margin-top: 24px;
  font-size: var(--font-size-caption);
}
.profile-forms {
  display: grid;
  gap: 24px;
}
.section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.profile-form {
  max-width: 440px;
  margin-top: 24px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
}
@media (max-width: 1000px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
  .identity {
    padding: 24px;
  }
}
</style>
