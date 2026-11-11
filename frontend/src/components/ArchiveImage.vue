<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { resourceUrl } from '../utils/format'
const props = withDefaults(defineProps<{ path: string | null; size?: number }>(), { size: 40 })
const failed = ref(false)
const src = computed(() => resourceUrl(props.path))
watch(src, () => {
  failed.value = false
})
</script>
<template>
  <span class="archive-image" :style="{ width: `${size}px`, height: `${size}px` }"
    ><img v-if="src && !failed" :src="src" alt="商品图片" @error="failed = true" /><el-icon v-else
      ><Picture /></el-icon
  ></span>
</template>
<style scoped>
.archive-image {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  overflow: hidden;
  background: #f0f4eb;
  color: #a8b49b;
  flex-shrink: 0;
}
.archive-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
