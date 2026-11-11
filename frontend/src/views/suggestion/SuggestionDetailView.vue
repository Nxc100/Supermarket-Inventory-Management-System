<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { suggestion } from '../../api/suggestions'
import type { SuggestionDetail } from '../../types/suggestion'
import SuggestionResult from './SuggestionResult.vue'
const route = useRoute(),
  detail = ref<SuggestionDetail | null>(null),
  loading = ref(false),
  error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await suggestion(route.params.id as string)
  } catch (e) {
    detail.value = null
    error.value = e instanceof Error ? e.message : '历史记录加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">SUGGESTION HISTORY</div>
      <h1>建议历史详情</h1>
      <p>显示生成时数据和结果，不代表当前实时经营情况。</p>
    </div>
    <div class="query-bar">
      <router-link :to="{ path: '/suggestions', query: { tab: 'history' } }"
        ><el-button>历史记录</el-button></router-link
      ><el-button :loading="loading" @click="load">刷新详情</el-button>
    </div>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false"
    ><el-button @click="load">重试</el-button></el-alert
  ><el-skeleton v-if="loading && !detail" :rows="6" animated /><SuggestionResult
    v-if="detail"
    :detail="detail"
  />
</template>
