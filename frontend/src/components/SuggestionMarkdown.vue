<script setup lang="ts">
import { computed } from 'vue'
import { renderSuggestionMarkdown } from '../utils/markdown'
const props = defineProps<{ content: string }>()
const html = computed(() => renderSuggestionMarkdown(props.content))
</script>
<template>
  <!-- Only the shared HTML-disabled parser and strict DOMPurify allowlist reach this sink. -->
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div class="suggestion-markdown" v-html="html" />
</template>
<style scoped>
.suggestion-markdown {
  color: var(--text-primary, #243c35);
  font-size: 15px;
  line-height: 1.95;
  overflow-wrap: anywhere;
  max-width: 100%;
}
.suggestion-markdown :deep(h1),
.suggestion-markdown :deep(h2),
.suggestion-markdown :deep(h3),
.suggestion-markdown :deep(h4) {
  font-size: 17px;
  line-height: 1.5;
  margin: 26px 0 10px;
  padding-left: 12px;
  border-left: 3px solid #719c87;
  color: #245441;
}
.suggestion-markdown :deep(p) {
  margin: 10px 0 16px;
}
.suggestion-markdown :deep(ul),
.suggestion-markdown :deep(ol) {
  padding-left: 24px;
  margin: 10px 0 20px;
}
.suggestion-markdown :deep(li) {
  padding: 3px 0 3px 5px;
}
.suggestion-markdown :deep(li::marker) {
  color: #5e8671;
  font-weight: 600;
}
.suggestion-markdown :deep(strong) {
  color: #21513d;
  font-weight: 650;
}
.suggestion-markdown :deep(blockquote) {
  margin: 16px 0;
  border-left: 3px solid #d1dfd4;
  padding: 8px 18px;
  background: #f5f8f3;
}
.suggestion-markdown :deep(pre) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: #f3f6f1;
  border-radius: 8px;
  padding: 16px;
}
.suggestion-markdown :deep(code) {
  font-family: Consolas, monospace;
  font-size: 13px;
  background: #f3f6f1;
}
.suggestion-markdown :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
  margin: 18px 0;
}
.suggestion-markdown :deep(th),
.suggestion-markdown :deep(td) {
  border: 1px solid #dce6dc;
  padding: 9px 14px;
  min-width: 90px;
}
.suggestion-markdown :deep(th) {
  background: #f2f6ee;
  text-align: left;
}
</style>
