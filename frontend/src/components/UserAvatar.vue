<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resourceUrl } from '../utils/format'
const props = withDefaults(defineProps<{ path: string | null; name: string; size?: number }>(), {
  size: 32,
})
const failed = ref(false)
const src = computed(() => resourceUrl(props.path))
watch(src, () => {
  failed.value = false
})
</script>
<template>
  <el-avatar :size="size"
    ><img v-if="src && !failed" :src="src" alt="当前头像" @error="failed = true" /><span v-else>{{
      name.slice(0, 1)
    }}</span></el-avatar
  >
</template>
