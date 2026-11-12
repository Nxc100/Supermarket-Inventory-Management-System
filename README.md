# 禾序 · 超市进销存管理系统

Spring Boot 单体后端 + Vue 桌面管理前端。[权威需求](doc/超市进销存管理系统功能需求文档.md)31组操作功能均已完成并标记✅；实现及验收遵循[开发规范](doc/基于SpringBoot与Vue的超市进销存管理系统开发规范文档.md)，实际证据和历史失败见[测试验收记录](doc/测试验收记录.md)。

已实现三岗位权限及账号管理、个人资料/安全图片、商品/分类/供应商、采购到货与审核/对账/撤销、销售开单/幂等恢复/当天整单纠错、库存批次/流水/盘点冻结/预警、真实统计图表、通义千问建议及独立降级、工作台、参数与审计。采购、销售和盘点通过同一事务库存内核核对数量。

2026-11-11后续已按用户明确要求切换正式库`supermarket_inventory`，完整备份后显式追加模型用量字段，通过Playwright MCP页面加载12个零售商品、3个供应商和正常采购库存，并验证31组功能及数量闭环。当前证据以[正式库用户视角验收记录](doc/正式库用户视角验收记录.md)为准。已修复路由资源加载失败恢复入口、模型库存余额/期间销量口径及滞销说明。后续通信专项修正Hikari生命周期及测试连接资源，新增真实断线回归，后端全量133项verify、前端21项单测/lint/build、正式库29项一致性通过。独立TCP回显与抓包证实跨端口回环握手故障；停止旧Npcap 0.9982后，三路各300次探测及另2000次回显/200次MySQL连接均成功，Chrome整站UI单批36/36通过。用户选择暂时保持Npcap停止，未升级驱动；重启或重新加载旧驱动可能恢复故障。此前131项中2项通信失败和历史UI失败均保留。后文早期“业务库未写入”及历史全绿结果仅属于对应阶段。

## 环境

本机验证环境：Windows 11、JDK 17（用户指定 `D:\xc_test\jdk-17`）、Node 24.16.0、npm 11.13.0、MySQL 8.0.34。Maven Wrapper 固定 3.9.9；已提供 Maven 3.10.0 可用于首次生成 Wrapper。依赖锁定在 `backend/pom.xml` 与 `frontend/package-lock.json`。

后端 Spring Boot 3.5.16、MyBatis-Plus 3.5.18、Spring Security JWT/BCrypt；前端 Vue 3.5.25、Vite 7.2.2、TypeScript 5.9.3、Element Plus 2.11.8、Pinia、Axios。版本与 JDK 要求参考 [Spring Boot 官方文档](https://docs.spring.io/spring-boot/3.5/system-requirements.html)和 [MyBatis-Plus Boot 3 安装说明](https://baomidou.com/en/getting-started/install/)。

## 本地运行

1. 已导入的业务库为 `supermarket_inventory`。`mysql` 是系统库，应用不在其中建业务表。**应用启动不执行建表或重导**。新空库初始化仅使用 [supermarket_inventory.sql](doc/数据库/supermarket_inventory.sql)，禁止对已有业务库重复导入。
2. 复制 `backend/application-local.example.yml` 为 `backend/application-local.yml`，填写本地数据库密码及随机 JWT 密钥（至少32字节）。真实配置已被 Git 忽略。也可设置 `DB_PASSWORD`、`DB_USER`、`DB_URL`、`JWT_SECRET` 环境变量。
3. 从根目录启动后端：

   ```powershell
   .\scripts\start-backend.ps1 -JavaHome 'D:\xc_test\jdk-17'
   ```

4. 前端：

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

访问 `http://127.0.0.1:5173`；后端默认绑定127.0.0.1:8080（可用SERVER_ADDRESS/SERVER_PORT调整），通过Vite同源代理访问。`npm run build`后可用`npm run preview`在`http://127.0.0.1:4173`检查打包产物，代理与开发环境一致。头像存放在后端工作目录的 `.local/uploads`，可通过 `UPLOAD_DIR` 更改；数据库仅保存相对文件名，`/media` 提供经过解码校验的图片。接口文档 `http://127.0.0.1:8080/swagger-ui.html`。账号为 SQL 自带的 `admin`、`buyer01`、`cashier01`、`cashier02`，初始密码 `Test123456`；密码与数据库账号密码无关。

### 商品和头像图片初始化

项目已包含14张商品示意照片、8张头像照片及来源清单，运行时从本地`/media`读取，不依赖外链。新空库导入唯一SQL前，先运行`python -m pip install -r scripts/requirements-media.txt`和`python scripts/initialize-media.py --install-only`安装图片。SQL已包含对应的相对路径及真实图片元数据。

已有业务库补图时，设置`DB_PASSWORD`后运行`python scripts/initialize-media.py --database supermarket_inventory`预览，再增加`--apply`执行。它只补全已知演示商品及账号的空图、历史占位路径或丢失文件，保留有效上传图片，不更改库存、单据或权限；修改前的图片列备份保存于`.local/media-initialization/`。自定义`UPLOAD_DIR`时使用同一个绝对目录，或显式传`--upload-dir`。详见[图片素材与部署说明](doc/数据库/media/README.md)。不要向已有库重导包含删表语句的初始化SQL。

后端监听地址变化时，在启动Vite的终端设置`BACKEND_PROXY_TARGET`为同一个后端地址（默认`http://127.0.0.1:8080`）。例如后端`SERVER_ADDRESS=::1`、`SERVER_PORT=8082`时，前端设`BACKEND_PROXY_TARGET=http://[::1]:8082`。它只由开发/预览进程读取，不注入浏览器包；修改后重启相应Vite实例。普通与AI代理各有独立4连接池，空闲25秒，异常连接销毁，不自动重发业务写。IPv6对照也曾出现本机连接失败，不将切换地址当作已修复根因。

数据库连接采用标准Hikari：最多10条、最少2条空闲，连接寿命90秒，小于会话空闲超时120秒，空闲回收60秒、保活30秒；借连接5秒、验证2秒、JDBC连接5秒/读取15秒。测试每池最多4条、最少1条空闲，Spring测试上下文缓存最多4个，淘汰时关闭连接池。长事务不会被maxLifetime强行中断，业务仍须保持短事务。

连接排障可在根目录设置`DB_PASSWORD`后运行`python scripts/diagnose-connectivity.py --database supermarket_inventory --samples 120 --interval 0.5 --echo`。它仅执行只读SQL、无身份HTTP探测和临时独立TCP回显，报告写入已忽略的`.local/diagnostics/`；故障退出码为1。报告不含口令、Token、业务行或模型密钥。`--backend-url`可对照独立端口；新连接探测与连接池故障回归各自验证不同层次，不用重发采购/销售来测试网络。

刷新后的登录身份查询遇到网络错误时，会显示“登录身份暂未核实”，保留会话、隐藏业务数据并提供显式重试，连接恢复后返回原页面。真正登录失效仍须重新登录；网络错误不再误清会话。

### IntelliJ IDEA 2025.2.6.2

打开根项目并将 `backend/pom.xml` 添加为 Maven 项目，或直接打开该 pom。Project SDK、Maven importer 与 runner 的 JDK 均选择 `D:\xc_test\jdk-17`，语言级别17，Maven选择Wrapper。运行 `InventoryApplication.main()`，Working directory 必须为 `backend`（用于读取被忽略的本地配置）。根目录已提供 `.run/InventoryApplication.run.xml`；直接打开 backend 时可从主类生成 Application 配置。无需 Lombok、额外服务器或 IDE 专有插件。

### 通义千问本地配置

新增模型能力：建议页可选择管理员允许的模型，真实SSE逐段输出；正文与历史共用安全Markdown排版。管理员从左侧“模型配置与用量”设置API Key、默认及可选模型，按日期/模型/类型查看调用次数、输入/输出/总Token和缓存输入。只统计本系统服务商实际报告的数据，旧记录/断流未报告部分单独列出，不代表阿里云账户账单或余额。

已有数据库首次使用这一版本前，备份选定数据库，再从根目录显式执行一次非破坏升级：

```powershell
$env:DB_PASSWORD = '<本地MySQL密码>'
python scripts/upgrade-ai-usage.py --database '<明确选定的项目库名>'
```

脚本只追加5个可空用量字段、约束及索引，不重置库、不改历史正文/库存，不在启动时自动执行。可重复执行；中途中断后按已存在字段续做。开发验收先升级独立测试副本；后续按用户明确授权已备份并显式升级正式库`supermarket_inventory`，通过正常页面加载演示业务，详见正式库验收记录。新空库继续使用唯一SQL。

管理页保存的Key使用AES-256-GCM密文存`sys_config`。后端优先读取`AI_CONFIG_MASTER_KEY`（32随机字节的Base64），未设置时首次保存自动创建当前后端工作目录`.local/secrets/ai-master.key`并限制文件权限；可用`AI_CONFIG_MASTER_FILE`指定私有文件位置。**保留并安全备份该主密钥，数据库备份不能单独解密Key；遗失后须重新保存API Key。** 不随JWT密钥更换、不进入SQL/前端或Git。管理页Key为仅写入，保存后清空；“切回后端本地配置”移除密钥覆盖，恢复下述配置来源。

后端可设置 `DASHSCOPE_API_KEY`，或以 `DASHSCOPE_API_KEY_FILE` 指向仅含密钥的UTF-8文件。本机按用户授权使用已忽略的 `doc/key.txt`，后端本地配置设 `app.ai.key-file: ../doc/key.txt`（工作目录为backend）。不要在前端变量或聊天中填写密钥。默认模型 `qwen-plus`，可用 `DASHSCOPE_MODEL` 修改；`DASHSCOPE_TIMEOUT_SECONDS` 限制在1～15秒。未配置不影响启动、采购或销售，生成时保存明确失败记录。

建议页先采集并核对真实摘要，10分钟内确认发送同一快照。生成结果未知时保留原请求标识，刷新后查询或同键重试；仅明确重新采集才创建新请求。历史测试夹具单独标识，不代表真实调用。模型调用不持有数据库事务；建议接口Axios等待20秒、Vite代理等待20秒，基础接口仍使用原超时。

流式浏览器通过带Authorization的POST fetch读取SSE，独立等待22秒；正文逐段出现，持久化后才显示已生成。浏览器离开后后端仍在15秒模型总时限内收尾，返回页面查询原结果即可，不自动重复调用。默认与可选模型的管理变更只影响之后调用，已核对摘要固定所选模型。模型必须为已开通且与Key地域匹配的千问文本模型，支持手动输入模型ID。

## 检查与测试

后端测试连接独立 `supermarket_inventory_test`，不会初始化/清理业务库。首次准备需要 Python 3、PyMySQL 和 `DB_PASSWORD` 环境变量：

```powershell
python scripts/prepare-test-db.py
# 仅明确要重建这个测试副本时：python scripts/prepare-test-db.py --reset
cd backend
.\mvnw.cmd test
.\mvnw.cmd verify
# 格式修正：.\mvnw.cmd spotless:apply
```

测试数据库命名被脚本限制为 `supermarket_inventory_*test` / `*dbcheck`。普通接口用例通过事务回滚；并发/回滚边界用例仅提交自己创建的商品和采购夹具，并按唯一ID清理。真实 MySQL 验证 JWT、三角色边界、字段白名单、改密/退出失效、档案引用、采购/核对/库存一致性和审计。测试不会自动跳过。单独的测试可用 `-Dtest=类名` 选择。接口集成测试与UI测试顺序执行，避免共用种子账号的行锁干扰交互验证。

UI允许同样严格命名的独立`supermarket_inventory_*test`副本，`E2E_TEST_DATABASE`必须填后端实际使用的库名；业务库名称会被拒绝。盘点时间一致性修复后的验收使用新建的`supermarket_inventory_acceptance_test`，由唯一SQL初始化，原测试库保留历史问题证据，无重置。更换UI测试副本时须同时配置后端DB_URL与该环境变量，不能只改标记。

```powershell
cd frontend
npm ci
npm run lint
npm run typecheck
npm run test:unit
npm run build
# 前后端均已启动且后端连接测试副本后：
$env:E2E_TEST_DATABASE = 'supermarket_inventory_test'
npm run test:e2e
# 检查已打包产物时，另开终端先运行 npm run preview，再设置：
# $env:E2E_BASE_URL = 'http://127.0.0.1:4173'
# npm run test:e2e
```

测试后端可通过根目录 `scripts/start-backend.ps1 -JavaHome 'D:\xc_test\jdk-17' -TestDatabase` 启动；先设置 `DB_PASSWORD`。测试结束后，停止该实例，再按普通方式启动业务库实例。Playwright 默认使用本机 Chrome，可用 `E2E_BROWSER_CHANNEL=msedge` 改为 Edge，无需另下浏览器；在没有本机浏览器的环境安装 Playwright Chromium 后可调整channel配置。

需求12.3的规模验证使用另一独立库，脚本只通过真实HTTP命令建档、审核采购和完成销售，不直接编造余额或流水。先完成后端verify打包，再在根目录执行：

```powershell
$env:DB_PASSWORD = '<本地MySQL密码>'
python scripts/prepare-test-db.py --database supermarket_inventory_performance_test
python scripts/performance-check.py --java-home 'D:\xc_test\jdk-17'
# 已有完整数据时，仅重新测量（仍新增30张专属测试销售）：
# python scripts/performance-check.py --java-home 'D:\xc_test\jdk-17' --measure-only
```

脚本自行启动并停止127.0.0.1:8081的专属后端，拒绝占用现有服务端口；不重置/删除任何数据库。中断后可直接重跑，按专属编码、采购备注和固定销售请求键核实已提交状态，不自动重试未知写入。结果保存至`.local/performance-report.json`，包含环境、真实行数、30个样本的p50/p95/最大值及账面/批次/流水一致性。报告未产生前不能宣称达到2秒目标。页面回归可按本机条件设置`E2E_SLOW_MO_MS=100`以接近人工操作节奏；默认0，无自动重试。

CLI Playwright自动化套件只写入独立测试副本，其业务库名称保护继续保留。后续用户明确授权的正式库验收使用Playwright MCP逐步操作页面，不绕过CLI保护。已提交的业务记录按正常关闭/撤销保留历史，不以删除抹去审核轨迹。此前验收副本为`supermarket_inventory_acceptance_test`；原测试库保留30条旧盘点时间差证据，新代码已统一盘点完成与调整流水时间，新副本及本轮正式库检查均无该问题。

2026-11-11早期验收副本验证（后续正式库结果见文首链接）：

| 检查 | 实际结果 |
| --- | --- |
| JDK17 Maven Wrapper verify | 131/131；后续相关17项verify、Spotless及可运行JAR通过 |
| 前端lint、类型检查、构建 | 通过；21/21单元测试通过 |
| Chrome UI，无自动重试 | 本轮7项相关用例同批通过；整站两批35/36、33/36，历史34/34保留，详见验收记录 |
| 唯一SQL新库验证 | 16表8视图，59/59通过；验收库业务一致性29/29通过 |
| 完整数量闭环 | 20−12−1−2+10=15；净销量14、净金额70.00 |
| 规模采样 | 300专属商品、20供应商、1000采购、至少5000销售；240样本均低于2秒，结算p95 400.74ms/max 798.25ms，账面/批次/流水0不一致 |
| 真实通义千问 | MCP真实库存qwen-flash记录38/40，销售qwen-plus记录39；分别815/830/974 Token，真实增量及历史均验证；超时和未配置降级另有记录 |

上述早期批次后原管理员启用、岗位默认权限、参数5/7和6个测试账号密码均已复核还原，UI/MCP商品没有遗留盘点冻结。该阶段业务库未写入；后续正式库已获授权加载数据，未重置，唯一SQL与模型用量追加字段、约束/索引一致。当前正式库状态见专门验收记录。

IDEA版本已核实为2025.2.6.2（252.28539.54），JDK17下构建与主类服务实际启动通过；已提供导入步骤及运行配置。computer-use原生管道不可用，尚未在IDE图形界面点击运行。前端存在Element Plus主包体积提示；性能结论限于本机正常采样窗口。此前连接中断、默认UI首批33/34及性能SQL超时均保留在验收记录，不把定向复测拼成全量通过，也不宣称本机网络永不失败。
