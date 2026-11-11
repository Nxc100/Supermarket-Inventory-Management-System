<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuth } from '../stores/auth'
import * as api from '../api/auth'
import { showError } from '../utils/feedback'
import UserAvatar from './UserAvatar.vue'
import { validateImageFile } from '../utils/images'

const auth = useAuth()
const input = ref<HTMLInputElement>()
const selected = ref<File | null>(null)
const preview = ref('')
const uploading = ref(false)
let selection = 0
function clear() {
  selection++
  selected.value = null
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
  if (input.value) input.value.value = ''
}
async function select(event: Event) {
  const target = event.target
  if (!(target instanceof HTMLInputElement)) return
  const file = target.files?.[0]
  clear()
  if (!file) return
  const current = selection
  try {
    await validateImageFile(file)
    if (current !== selection) return
    selected.value = file
    preview.value = URL.createObjectURL(file)
  } catch (error) {
    showError(error)
  }
}
async function upload() {
  if (!selected.value || uploading.value) return
  uploading.value = true
  try {
    auth.user = await api.uploadAvatar(selected.value)
    clear()
    ElMessage.success('头像已更新')
  } catch (error) {
    showError(error)
  } finally {
    uploading.value = false
  }
}
async function remove() {
  try {
    await ElMessageBox.confirm('确认使用默认头像？当前自定义头像将解除绑定。', '使用默认头像', {
      confirmButtonText: '使用默认头像',
      cancelButtonText: '取消',
      type: 'warning',
    })
    uploading.value = true
    auth.user = await api.removeAvatar()
    ElMessage.success('已恢复默认头像')
  } catch (error) {
    showError(error)
  } finally {
    uploading.value = false
  }
}
onBeforeUnmount(clear)
</script>
<template>
  <div class="avatar-editor">
    <UserAvatar
      :path="auth.user?.avatarPath ?? null"
      :name="auth.user?.realName ?? ''"
      :size="72"
    />
    <div v-if="auth.can('common.avatar')" class="avatar-actions">
      <input
        ref="input"
        type="file"
        accept="image/jpeg,image/png"
        aria-label="头像文件"
        class="file-input"
        :disabled="uploading"
        @change="select"
      /><el-button size="small" :disabled="uploading" @click="input?.click()">{{
        auth.user?.avatarPath ? '更换头像' : '上传头像'
      }}</el-button
      ><el-button
        v-if="auth.user?.avatarPath"
        link
        type="primary"
        size="small"
        :disabled="uploading"
        @click="remove"
        >使用默认头像</el-button
      >
    </div>
    <p class="muted">JPG / PNG · 2 MB以内<br />宽高64～4096像素</p>
    <el-dialog
      :model-value="selected !== null"
      title="头像预览"
      width="400px"
      :close-on-click-modal="false"
      :show-close="!uploading"
      :close-on-press-escape="!uploading"
      @update:model-value="clear"
      ><img v-if="preview" :src="preview" alt="待上传头像预览" class="avatar-preview" /><template
        #footer
        ><el-button :disabled="uploading" @click="clear">取消</el-button
        ><el-button type="primary" :loading="uploading" @click="upload"
          >确认上传</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<style scoped>
.avatar-actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.avatar-actions .el-button {
  margin: 0;
}
.file-input {
  display: none;
}
.avatar-editor p {
  margin: 12px 0 0;
  font-size: 11px;
  line-height: 1.8;
}
.avatar-preview {
  display: block;
  max-width: 100%;
  max-height: 300px;
  margin: auto;
  object-fit: contain;
}
</style>
