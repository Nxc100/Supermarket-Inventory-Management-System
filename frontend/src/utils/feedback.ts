import axios from 'axios'
import { ElMessage } from 'element-plus'
export function showError(error: unknown) {
  if (axios.isCancel(error) || error === 'cancel' || error === 'close') return
  ElMessage.error(error instanceof Error ? error.message : '操作失败，请稍后重试')
}
