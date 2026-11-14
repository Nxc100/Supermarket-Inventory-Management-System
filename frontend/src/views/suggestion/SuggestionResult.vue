<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { SuggestionDetail } from '../../types/suggestion'
import { suggestionTypes, suggestionStatuses } from '../../types/suggestion'
import { businessTime } from '../../utils/format'
import SuggestionMarkdown from '../../components/SuggestionMarkdown.vue'
const props = defineProps<{ detail: SuggestionDetail }>()
const copyError = ref('')
async function copy() {
  copyError.value = ''
  try {
    await navigator.clipboard.writeText(props.detail.content ?? '')
    ElMessage.success('建议正文已复制')
  } catch {
    copyError.value = '无法写入剪贴板，请手动选中正文复制'
  }
}
</script>
<template>
  <section class="panel suggestion-result">
    <div class="page-heading">
      <div>
        <h2>{{ suggestionTypes[detail.record.type] }}</h2>
        <p>
          {{ suggestionStatuses[detail.record.status] }} · {{ detail.record.provider }} /
          {{ detail.record.modelName }}
        </p>
      </div>
      <el-button v-if="detail.content" @click="copy">复制建议正文</el-button>
    </div>
    <el-alert
      v-if="detail.record.fixture"
      title="初始或测试夹具，非真实模型调用；不得作为真实生成证据"
      type="warning"
      :closable="false"
    />
    <p class="muted">
      生成时快照：{{ businessTime(detail.record.snapshotAt) }}（北京） · 分析区间
      {{ detail.record.periodStart }} 至 {{ detail.record.periodEnd }} · 操作者
      {{ detail.record.operatorName }}
    </p>
    <p class="muted">
      完成时间：{{ businessTime(detail.record.finishedAt) }}（北京） ·
      仅供人工参考，不会自动采购、调价或修改库存。
    </p>
    <el-alert
      v-if="detail.record.status === 'FAILURE'"
      title="智能建议暂不可用，进销存功能可正常使用"
      type="warning"
      :closable="false"
      ><p>{{ detail.record.errorMessage }}</p></el-alert
    ><el-alert
      v-if="detail.record.status === 'PENDING'"
      title="本次结果尚待完成；请查询原结果，不要另建请求重复生成。超过等待时间后会显示中断原因。"
      type="info"
      :closable="false"
    />
    <p v-if="copyError" role="alert">{{ copyError }}</p>
    <SuggestionMarkdown
      v-if="detail.record.status === 'SUCCESS' && detail.content"
      class="suggestion-text"
      :content="detail.content"
    />
    <p v-if="detail.usage" class="muted usage-note">
      Token 用量：<template v-if="detail.usage.totalTokens !== null"
        >输入 {{ detail.usage.promptTokens }} · 输出 {{ detail.usage.completionTokens }} · 合计
        {{ detail.usage.totalTokens
        }}<span v-if="detail.usage.cachedTokens !== null">
          · 缓存输入 {{ detail.usage.cachedTokens }}</span
        ></template
      ><template v-else>服务商未报告，本记录未计入已知 Token 合计</template>
    </p>
    <el-collapse
      ><el-collapse-item title="生成时输入摘要（历史快照）" name="summary">
        <pre class="summary-text">{{ JSON.stringify(detail.summary, null, 2) }}</pre>
      </el-collapse-item></el-collapse
    >
  </section>
</template>
<style scoped>
.suggestion-result {
  margin-top: 24px;
}
.summary-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.8;
  font-family: inherit;
}
.summary-text {
  max-height: 450px;
  overflow: auto;
  font-size: var(--font-size-caption);
  background: #f6f8f5;
  padding: 16px;
}
</style>
