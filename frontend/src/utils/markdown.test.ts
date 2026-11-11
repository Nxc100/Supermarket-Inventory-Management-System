import { describe, it, expect } from 'vitest'
import { renderSuggestionMarkdown } from './markdown'
describe('safe suggestion formatting', () => {
  it('renders headings, emphasis, lists and horizontally scrollable table markup', () => {
    const html = renderSuggestionMarkdown(
      '### 数据现象\n\n库存 **4** 件。\n\n1. 人工核查\n2. 核对补货\n\n| 商品 | 数量 |\n| --- | --- |\n| 饮料 | 4 |',
    )
    expect(html).toContain('<h3>数据现象</h3>')
    expect(html).toContain('<strong>4</strong>')
    expect(html).toContain('<ol>')
    expect(html).toContain('<table>')
  })
  it('never creates executable HTML, external images or links from model content', () => {
    const host = document.createElement('div')
    host.innerHTML = renderSuggestionMarkdown(
      '<script>alert(1)</script>\n<img src=x onerror=alert(1)>\n[点击](javascript:alert(1))\n![跟踪](https://example.com/a.png)\n<iframe src="https://example.com"></iframe>',
    )
    expect(host.querySelector('script,img,a,iframe,svg')).toBeNull()
    expect(host.textContent).toContain('script')
  })
})
