<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { init, use, type EChartsType } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'
use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer,
])
const props = defineProps<{ option: EChartsOption; empty: boolean; label: string }>()
const emit = defineEmits<{ datum: [index: number] }>()
const container = ref<HTMLDivElement>()
let chart: EChartsType | undefined, observer: ResizeObserver | undefined
function render() {
  if (!container.value) return
  if (props.empty) {
    chart?.clear()
    return
  }
  if (!chart) {
    if (!container.value.clientWidth) return
    // Canvas labels do not inherit CSS. Share the page's typography tokens with ECharts.
    const style = getComputedStyle(container.value)
    const fontSize = Number.parseFloat(style.fontSize)
    const captionSize = Number.parseFloat(style.getPropertyValue('--font-size-caption')) || fontSize
    const axis = {
      axisLabel: { fontSize: captionSize },
      nameTextStyle: { fontSize: captionSize },
    }
    chart = init(container.value, {
      textStyle: { fontFamily: style.fontFamily, fontSize },
      categoryAxis: axis,
      valueAxis: axis,
      legend: { textStyle: { fontSize: captionSize } },
      tooltip: { textStyle: { fontSize } },
      pie: { label: { fontSize: captionSize, lineHeight: 22 } },
    })
    chart.on('click', (event: { dataIndex?: number }) => {
      if (typeof event.dataIndex === 'number') emit('datum', event.dataIndex)
    })
  }
  chart.setOption(props.option, true)
  chart.resize()
}
watch(
  () => [props.option, props.empty],
  async () => {
    await nextTick()
    render()
  },
  { deep: true },
)
onMounted(() => {
  render()
  observer = new ResizeObserver(() => {
    if (!chart) render()
    else chart.resize()
  })
  if (container.value) observer.observe(container.value)
})
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = undefined
})
</script>
<template>
  <div class="report-chart">
    <el-empty v-if="empty" description="暂无图表数据，请调整筛选或先完成业务" />
    <div v-show="!empty" ref="container" class="chart-canvas" role="img" :aria-label="label" />
  </div>
</template>
<style scoped>
.chart-canvas {
  width: 100%;
  height: 360px;
}
.report-chart {
  min-height: 240px;
}
</style>
