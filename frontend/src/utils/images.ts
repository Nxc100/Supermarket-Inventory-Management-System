export async function validateImageFile(file: File) {
  if (!/\.(jpe?g|png)$/i.test(file.name) || !['image/jpeg', 'image/png'].includes(file.type))
    throw new Error('仅支持 JPG、JPEG 或 PNG 图片')
  if (!file.size || file.size > 2 * 1024 * 1024) throw new Error('图片不能为空，且不得超过2 MB')
  const bitmap = await createImageBitmap(file).catch(() => {
    throw new Error('图片无法正常解码')
  })
  const valid =
    bitmap.width >= 64 && bitmap.height >= 64 && bitmap.width <= 4096 && bitmap.height <= 4096
  bitmap.close()
  if (!valid) throw new Error('图片宽高必须为64～4096像素')
}
