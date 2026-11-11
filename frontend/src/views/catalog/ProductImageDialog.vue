<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Product } from '../../types/catalog'
import * as api from '../../api/products'
import { showError } from '../../utils/feedback'
import { validateImageFile } from '../../utils/images'
import ArchiveImage from '../../components/ArchiveImage.vue'
const props = defineProps<{ modelValue: boolean; product: Product | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; saved: [product: Product] }>()
const input = ref<HTMLInputElement>()
const selected = ref<File | null>(null)
const preview = ref('')
const busy = ref(false)
let sequence = 0
function clear() {
  sequence++
  selected.value = null
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
  if (input.value) input.value.value = ''
}
watch(() => props.modelValue, clear)
async function choose(event: Event) {
  const target = event.target
  if (!(target instanceof HTMLInputElement)) return
  const file = target.files?.[0]
  clear()
  if (!file) return
  const current = sequence
  try {
    await validateImageFile(file)
    if (current !== sequence) return
    selected.value = file
    preview.value = URL.createObjectURL(file)
  } catch (error) {
    showError(error)
  }
}
async function upload() {
  if (!props.product || !selected.value || busy.value) return
  busy.value = true
  try {
    const product = await api.uploadProductImage(props.product.id, selected.value)
    emit('saved', product)
    clear()
    ElMessage.success('商品图片已更新')
  } catch (error) {
    showError(error)
  } finally {
    busy.value = false
  }
}
async function remove() {
  if (!props.product) return
  try {
    await ElMessageBox.confirm(
      `移除「${props.product.name}」的图片？商品档案和库存保持不变。`,
      '移除商品图片',
      { confirmButtonText: '移除图片', cancelButtonText: '取消', type: 'warning' },
    )
    busy.value = true
    const product = await api.removeProductImage(props.product.id)
    emit('saved', product)
    clear()
    ElMessage.success('商品图片已移除')
  } catch (error) {
    showError(error)
  } finally {
    busy.value = false
  }
}
function close(done?: () => void) {
  if (busy.value) return
  clear()
  emit('update:modelValue', false)
  done?.()
}
onBeforeUnmount(clear)
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    title="商品图片"
    width="440px"
    :close-on-click-modal="false"
    :before-close="close"
    @update:model-value="emit('update:modelValue', $event)"
    ><div v-if="product" class="image-body">
      <p>{{ product.name }} · {{ product.code }}</p>
      <img
        v-if="preview"
        :src="preview"
        class="selected-image"
        alt="待上传商品图片预览"
      /><ArchiveImage v-else :path="product.imagePath" :size="160" />
      <p class="muted">JPG / PNG · 不超过2 MB · 宽高64～4096像素</p>
      <input
        ref="input"
        type="file"
        accept="image/jpeg,image/png"
        aria-label="商品图片文件"
        class="file-input"
        :disabled="busy"
        @change="choose"
      />
      <div class="image-actions">
        <el-button :disabled="busy" @click="input?.click()">选择图片</el-button
        ><el-button v-if="selected" :disabled="busy" @click="clear">取消选择</el-button
        ><el-button v-if="product.imagePath" type="danger" plain :disabled="busy" @click="remove"
          >移除图片</el-button
        >
      </div>
    </div>
    <template #footer
      ><el-button :disabled="busy" @click="close()">关闭</el-button
      ><el-button type="primary" :loading="busy" :disabled="!selected" @click="upload"
        >确认上传</el-button
      ></template
    ></el-dialog
  >
</template>
<style scoped>
.image-body {
  text-align: center;
}
.selected-image {
  display: block;
  max-width: 100%;
  max-height: 240px;
  margin: auto;
}
.file-input {
  display: none;
}
.image-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.image-actions .el-button {
  margin: 0;
}
.muted {
  font-size: 12px;
  margin: 20px 0;
}
</style>
