# 演示图片素材与初始化

本目录包含14张商品类别示意照片与8张头像照片，来自Pexels。商品照片宽1200像素，头像800×800，全部JPG且单图小于2MB；总计约2.84MB。照片用于演示，不表示商品实际品牌、包装规格或账号本人身份。

素材沿用[Pexels许可](https://www.pexels.com/license/)，不将其作为独立图库出售。来源页、实际下载地址、文件SHA-256、MIME、尺寸和字节数保存在[manifest.json](manifest.json)，可逐图追溯。

## 新环境安装

从项目根目录执行：

```powershell
python -m pip install -r scripts/requirements-media.txt
python scripts/initialize-media.py --install-only
```

然后只在新空项目库导入`doc/数据库/supermarket_inventory.sql`。SQL末尾的图片种子由manifest生成，覆盖36个已知商品编码、8个标准演示账号，其他导出种子账号按用户名稳定分配头像；所引用文件均随项目提供。视图按依赖顺序创建，不需要先建临时占位表。

后端从`backend`工作目录启动时，默认图片目录为`backend/.local/uploads`。如果配置了`UPLOAD_DIR`，安装与后端必须使用同一个目录；建议使用绝对路径，例如`--upload-dir D:/data/supermarket/uploads`。初始化工具不修改后端配置。

## 已有业务库增量补图

通过环境变量提供`DB_PASSWORD`，可选`DB_HOST`、`DB_PORT`、`DB_USER`。先预览，再执行：

```powershell
python scripts/initialize-media.py --database supermarket_inventory
python scripts/initialize-media.py --database supermarket_inventory --apply
```

增量模式只补空绑定、历史“测试图片.jpg”占位或已丢失的UUID图片。已有可用上传图片保留；只更新`product.image_path/image_meta`及`sys_user.avatar_path/avatar_meta`。不写库存、价格、单据、权限和口令；数据库自身的更新时间按原表规则处理。未知商品编码不会随意套用照片，报告中列为unmapped；其他缺图账号按用户名稳定分配头像。

所有素材先检查哈希、实际编码、字节数与尺寸，再复制文件。文件同名但内容冲突时停止；重复执行不覆写相同文件。数据库更新前将原图片列与计划写入`.local/media-initialization`，更新使用原绑定的比较条件，遇到同时上传的新图片会跳过该行；数据库异常回滚本批更新。备份只包含图片列，不包含密码或模型密钥。

不要在已有业务库执行初始化SQL：它包含重建表的语句。已有库必须使用`--apply`，不能把SQL种子当作增量迁移。

## 维护

新增或更换素材时，更新manifest及对应图片文件，按商品编码维护映射，再运行`python scripts/initialize-media.py --sync-baseline`将绑定同步到唯一SQL；该命令不连接数据库。素材依然通过既有`/media/{filename}`读取，上传、替换及共享文件回收机制沿用现有后端。

回归：`python -m unittest discover -s tests/scripts -v`。

## 素材清单

| 用途 | 像素 | 本地文件 | 来源 |
| --- | --- | --- | --- |
| 瓶装橙汁 | 1200×1800 | [dfac9770-c5c4-5b5b-a19b-e26c9372888f.jpg](dfac9770-c5c4-5b5b-a19b-e26c9372888f.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/5946790/) |
| 纯牛奶 | 1200×1800 | [43c5bb41-a9db-5686-b072-daa7c1cd2c69.jpg](43c5bb41-a9db-5686-b072-daa7c1cd2c69.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/5946733/) |
| 酸奶 | 1200×1797 | [e1b1a95b-32b7-5509-8b84-62cd1925886a.jpg](e1b1a95b-32b7-5509-8b84-62cd1925886a.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/10809259/) |
| 面包 | 1200×1600 | [58ca9274-94cc-5f49-beb3-53d6c3e6dfe2.jpg](58ca9274-94cc-5f49-beb3-53d6c3e6dfe2.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/11214715/) |
| 大米 | 1200×800 | [54737c4a-31a9-5d49-9610-60adfcd5cc1a.jpg](54737c4a-31a9-5d49-9610-60adfcd5cc1a.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/7779908/) |
| 食用油 | 1200×1800 | [121f0b1f-c972-526a-afb8-ee04faf69790.jpg](121f0b1f-c972-526a-afb8-ee04faf69790.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/6213754/) |
| 抽纸 | 1200×900 | [7d26380d-2202-573f-a2e5-8cb39ab31b46.jpg](7d26380d-2202-573f-a2e5-8cb39ab31b46.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/35539861/) |
| 香皂 | 1200×1800 | [2bcab468-83a1-547e-bbba-4063b06f604e.jpg](2bcab468-83a1-547e-bbba-4063b06f604e.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/8285564/) |
| 饮用水 | 1200×1800 | [30e32958-e18d-5ff4-9e85-61064db5893d.jpg](30e32958-e18d-5ff4-9e85-61064db5893d.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/11860563/) |
| 文具笔 | 1200×800 | [e46c82e2-40b0-5d4f-96bf-4d579932ea94.jpg](e46c82e2-40b0-5d4f-96bf-4d579932ea94.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/5952015/) |
| 方便面 | 1200×1800 | [bdc4ba25-9153-5941-878a-7aacbc209d8e.jpg](bdc4ba25-9153-5941-878a-7aacbc209d8e.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/6940984/) |
| 饼干 | 1200×1461 | [2ecc72fc-ea47-51c5-b013-55673745e638.jpg](2ecc72fc-ea47-51c5-b013-55673745e638.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/31617910/) |
| 笔记本 | 1200×1800 | [5d727db2-e392-5df2-b4a3-1bd43477b7d2.jpg](5d727db2-e392-5df2-b4a3-1bd43477b7d2.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/5594320/) |
| 洗衣液 | 1200×1800 | [2d02cd03-1295-5d5e-bee7-c653e4d1aa80.jpg](2d02cd03-1295-5d5e-bee7-c653e4d1aa80.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/5218021/) |
| 演示头像 1 | 800×800 | [482894fd-768d-58c0-af15-cbcf6676dd81.jpg](482894fd-768d-58c0-af15-cbcf6676dd81.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/220453/) |
| 演示头像 2 | 800×800 | [9987e67f-ed37-538b-9575-9f7830c923d1.jpg](9987e67f-ed37-538b-9575-9f7830c923d1.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/415829/) |
| 演示头像 3 | 800×800 | [5c4d411a-5905-50b5-98b2-736200d32076.jpg](5c4d411a-5905-50b5-98b2-736200d32076.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/614810/) |
| 演示头像 4 | 800×800 | [e3ab0093-bc9f-53ac-8d0c-777d3c390294.jpg](e3ab0093-bc9f-53ac-8d0c-777d3c390294.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/774909/) |
| 演示头像 5 | 800×800 | [23ca5159-b7b8-59cf-a79b-8fb2ab134412.jpg](23ca5159-b7b8-59cf-a79b-8fb2ab134412.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/1239291/) |
| 演示头像 6 | 800×800 | [6c7b50d6-0830-5dd7-b90b-8373bb1ac6f0.jpg](6c7b50d6-0830-5dd7-b90b-8373bb1ac6f0.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/91227/) |
| 演示头像 7 | 800×800 | [83392ae9-2d11-5d76-88bd-db8522fdb22c.jpg](83392ae9-2d11-5d76-88bd-db8522fdb22c.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/733872/) |
| 演示头像 8 | 800×800 | [50b137a5-ecd5-50b3-a98b-73575d24a5f2.jpg](50b137a5-ecd5-50b3-a98b-73575d24a5f2.jpg) | [Pexels 原图页面](https://www.pexels.com/photo/1222271/) |
