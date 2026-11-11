import Decimal from 'decimal.js'
export const money = (value: string | number) => new Decimal(value).toFixed(2)
const beijingParts = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
})
export function businessTime(value: string | null) {
  if (!value) return '—'
  const date = new Date(/(?:Z|[+-]\d{2}:\d{2})$/.test(value) ? value : `${value}+08:00`)
  if (Number.isNaN(date.getTime())) return '—'
  const parts = Object.fromEntries(
    beijingParts.formatToParts(date).map((part) => [part.type, part.value]),
  )
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}
export const dateTime = (value: string) =>
  new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
export const resourceUrl = (path: string | null) =>
  path && /^[a-f0-9-]{36}\.(png|jpg)$/.test(path) ? `/media/${encodeURIComponent(path)}` : ''
