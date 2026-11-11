import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox } from 'element-plus'
let activeDirty: Ref<boolean> | null = null
export async function confirmUnsaved(): Promise<boolean> {
  if (!activeDirty?.value) return true
  try {
    await ElMessageBox.confirm('未保存的内容将丢失，确认离开？', '离开页面', {
      type: 'warning',
      confirmButtonText: '确认离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
}
export function useDirtyGuard(dirty: Ref<boolean>) {
  activeDirty = dirty
  const beforeUnload = (event: BeforeUnloadEvent) => {
    if (dirty.value) {
      event.preventDefault()
      event.returnValue = ''
    }
  }
  onMounted(() => window.addEventListener('beforeunload', beforeUnload))
  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', beforeUnload)
    if (activeDirty === dirty) activeDirty = null
  })
  onBeforeRouteLeave(async () => {
    if (!sessionStorage.getItem('inventory-token')) return true
    return confirmUnsaved()
  })
}
