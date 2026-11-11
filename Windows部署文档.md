# 禾序超市进销存管理系统 Windows 部署文档

> 适用对象：第一次在新 Windows 电脑部署本项目的开发、演示和维护人员。依据当前仓库代码编写，核对日期：2026-11-11。所有命令均以 **Windows PowerShell 5.1 / PowerShell 7** 为目标，数据库命令另外标注。

## 阅读路线

- **新电脑快速运行、开发或答辩**：按第1～8章操作，浏览器访问 `http://127.0.0.1:5173`。
- **使用打包产物持续运行**：先完成数据库和配置，再按第9章部署 JAR、静态页面和 Nginx；第10章可选配置登录后自动启动。
- **从原电脑搬迁业务数据**：先阅读第12章，恢复备份；不要再导入演示初始化SQL。
- **启动失败**：先查第13章，再按第14章逐项验收。

本文的安装和系统配置命令供部署者在新电脑执行。本次交付只编写并检查文档，没有在当前电脑重新安装软件、重建数据库或更改服务。

<a id="overview"></a>
## 1 部署对象、版本与目录约定

### 1.1 项目实际运行方式

```text
本机快速运行：浏览器 → Vite :5173 → Spring Boot :8080 → MySQL :3306
打包运行：    浏览器 → Nginx :8088 → /api、/media → Spring Boot :8080
                                  └→ frontend/dist 静态页面
后端额外访问：阿里云 DashScope HTTPS（只有生成经营建议时需要）
```

项目不需要安装 Tomcat、Redis、消息队列、Docker 或独立 Maven。后端自带嵌入式服务器，仓库 Maven Wrapper 会下载固定版本 Maven。Python 只用于额外测试、连接诊断和旧库字段升级，**不是基础启动必需项**。

### 1.2 环境版本

| 软件 | 本文要求 | 如何确认 |
| --- | --- | --- |
| Windows | Windows 11 x64；Windows 10 x64需满足软件自身支持条件 | `winver`；本文以64位Intel/AMD电脑为目标 |
| JDK | **17，完整JDK，不是只有JRE** | `java -version`、`javac -version` |
| Node.js | **24.x，必须小于25** | `node --version`；来自`frontend/package.json`引擎约束 |
| npm | 使用Node 24自带版本 | `npm.cmd --version` |
| MySQL | 本项目实测基准为 **8.0.34** | `SELECT VERSION();` |
| Maven | Wrapper固定3.9.9，自动获取 | `backend/mvnw.cmd -v` |
| 浏览器 | 新版Edge或Chrome | 页面及UI测试使用桌面浏览器 |
| Git | 使用Git获取代码时才需要 | `git --version` |

**MySQL版本说明**：本文主流程以已验证的8.0系列复现项目，官方Installer当前仍提供8.0.46；8.0.46属于同系列较新补丁，但本仓库已有全量验收证据的服务器版本是8.0.34。MySQL官方已公告8.0在2026年4月结束生命周期，长期正式使用应规划8.4 LTS并在隔离库运行本项目完整验收；本文不把8.4兼容性写成已验证结论。[MySQL官方版本说明](https://dev.mysql.com/doc/relnotes/mysql/8.0/en/)

不要替换为MySQL 5.7、MariaDB或SQL Server；基准使用MySQL 8的CHECK、JSON、窗口函数、生成列及`utf8mb4_0900_ai_ci`。不要直接安装“最新Java”和“最新Node”代替项目要求。

建议为本地构建准备4核CPU、8GB以上内存、10GB以上空闲磁盘。这是部署建议，不是项目做过的最低硬件性能承诺。

### 1.3 统一目录和端口

本文用以下路径，便于直接复制命令；如果更换路径，必须同步修改后文配置和启动脚本。

```text
C:\Apps\SupermarketInventory\                 项目根目录
  backend\                                  后端启动工作目录
    application-local.yml                   本机私有配置，需要新建
    target\inventory-1.0.0-SNAPSHOT.jar       后端构建产物
    .local\uploads\                         用户上传的头像和商品图
    .local\secrets\ai-master.key             管理Key加密主密钥，首次保存时生成
  frontend\dist\                            前端构建产物
  .local\deploy\                            本文使用的部署辅助文件
C:\Apps\nginx\                              可选Nginx目录
C:\Backups\SupermarketInventory\             自选的私有备份目录
```

| 端口 | 用途 | 默认访问范围 |
| --- | --- | --- |
| 3306 | MySQL | 本机，通常不向局域网开放 |
| 8080 | 后端API和媒体 | 默认只监听`127.0.0.1` |
| 5173 | 前端开发服务 | 默认只监听`127.0.0.1` |
| 4173 | 构建产物本机预览 | 默认只监听`127.0.0.1` |
| 8088 | 本文示例Nginx入口 | 先配置只监听本机，局域网见9.7 |
| 8081 / 5174 | 可选独立UI验收服务 | 不与正式运行服务混用 |

## 2 准备 Windows 终端

### 2.1 区分管理员终端与普通终端

安装JDK/Node/MySQL、管理MySQL服务或配置防火墙时，以管理员身份打开PowerShell。项目构建、启动和配置文件创建使用实际运行项目的普通Windows账号，后续也使用同一账号运行，尤其不要随意把AI主密钥改由其他账号读取。

在每个新PowerShell窗口，可先执行：

```powershell
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
```

本文使用`npm.cmd`、`mvnw.cmd`，避免误执行被PowerShell策略拦截的`npm.ps1`。执行项目`.ps1`时可使用单次命令的`-ExecutionPolicy Bypass`，不需要全局关闭执行策略。

### 2.2 检查占用和网络

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in 3306,8080,5173,4173,8088 } |
    Select-Object LocalAddress,LocalPort,OwningProcess

Test-NetConnection repo.maven.apache.org -Port 443
Test-NetConnection registry.npmjs.org -Port 443
```

已有3306服务不等于需要再装MySQL；先核实其版本、服务名和数据库。8080/5173等被其他项目占用时，不要直接结束所有Java或Node进程，按第13章识别后选择换端口或停止对应实例。

## 3 安装依赖

### 3.1 安装 JDK 17

在**管理员PowerShell**中：

```powershell
winget --version
winget install --id EclipseAdoptium.Temurin.17.JDK --exact --source winget --accept-source-agreements --accept-package-agreements
if ($LASTEXITCODE -ne 0) { throw 'JDK installation failed; inspect the installer output.' }
```

若没有winget，使用[Adoptium官方Windows安装说明](https://adoptium.net/installation/windows)，下载 **Temurin 17 / Windows / x64 / JDK / MSI**，在安装选项中启用设置`JAVA_HOME`和加入PATH。不是选择官网默认的其他Java大版本。

安装后关闭终端，新开**普通PowerShell**检查：

```powershell
java -version
javac -version
$env:JAVA_HOME
Get-Command java.exe,javac.exe | Select-Object Name,Source
```

期望两个版本都以17开头。若环境变量未设置，按实际安装位置选择：

```powershell
$jdkCandidates = @(Get-ChildItem -LiteralPath 'C:\Program Files\Eclipse Adoptium' -Directory |
    Where-Object { $_.Name -like 'jdk-17*' } |
    Sort-Object LastWriteTime -Descending)
if ($jdkCandidates.Count -eq 0) { throw 'Locate the installed JDK 17 directory first.' }
$JavaHome = $jdkCandidates[0].FullName
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) { throw 'A full JDK is required.' }
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
[Environment]::SetEnvironmentVariable('JAVA_HOME', $JavaHome, 'User')
& "$JavaHome\bin\java.exe" -version
```

后续启动使用明确的`JAVA_HOME\bin\java.exe`，即使系统同时安装其他Java，也可避免选错版本。更换安装目录后要更新该值。

### 3.2 安装 Node.js 24

简单方式：打开[Node.js官方24系列下载目录](https://nodejs.org/download/release/latest-v24.x/)，下载文件名为`node-v24.*-x64.msi`的安装包并安装，保留npm和PATH选项。不要下载arm64版本，也不要把官网当前最新大版本直接用于本项目。

也可在**管理员PowerShell**执行下面的下载、校验和安装命令。脚本从官方SHA256清单确定24系列具体版本，再按该固定版本下载，避免“latest”目录更新时文件与校验值不一致。

```powershell
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$DownloadDir = Join-Path $env:USERPROFILE 'Downloads\SupermarketSetup'
New-Item -ItemType Directory -Path $DownloadDir -Force | Out-Null
$sumUrl = 'https://nodejs.org/download/release/latest-v24.x/SHASUMS256.txt'
$sumText = (Invoke-WebRequest -Uri $sumUrl -UseBasicParsing).Content
$sumLines = @($sumText -split '\r?\n' | Where-Object {
    $_ -match '^[a-fA-F0-9]{64}\s+node-v24\.[0-9]+\.[0-9]+-x64\.msi$'
})
if ($sumLines.Count -ne 1) { throw 'Cannot uniquely identify the official Node 24 x64 MSI.' }
$parts = $sumLines[0].Trim() -split '\s+', 2
$expectedHash = $parts[0]
$msiName = $parts[1]
$nodeVersion = [regex]::Match($msiName, '^node-v(24\.[0-9]+\.[0-9]+)-x64\.msi$').Groups[1].Value
$nodeMsi = Join-Path $DownloadDir $msiName
Invoke-WebRequest -Uri "https://nodejs.org/download/release/v$nodeVersion/$msiName" -OutFile $nodeMsi -UseBasicParsing
if ((Get-FileHash -LiteralPath $nodeMsi -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Node installer checksum mismatch.'
}
if ((Get-AuthenticodeSignature -LiteralPath $nodeMsi).Status -ne 'Valid') {
    throw 'Node installer signature is not valid. Resolve certificate/network problems before installation.'
}
$installer = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/i', "`"$nodeMsi`"", '/passive', '/norestart') -Wait -PassThru
if ($installer.ExitCode -notin 0,3010) { throw "Node installation failed: $($installer.ExitCode)" }
```

新开普通PowerShell：

```powershell
node --version
npm.cmd --version
if ((& node -p 'process.versions.node.split(".")[0]') -ne '24') { throw 'This project requires Node.js 24.x.' }
```

SHA256清单来自[Node.js官方发布目录](https://nodejs.org/download/release/latest-v24.x/SHASUMS256.txt)，具体补丁版本随官方更新；项目依赖仍由仓库锁文件固定。

### 3.3 安装和配置 MySQL 8.0

新电脑可从[MySQL官方Installer](https://dev.mysql.com/downloads/installer/)下载安装程序。若需要严格复现本项目原实测版本，从官方Archives选择8.0.34；若选择8.0.46，完成第14章新机验收后再投入使用。

Installer建议选项：

1. 选择包含 **MySQL Server 8.0 x64** 的安装方式；“Server only”或自定义只装Server即可，Workbench不是必需项。
2. 安装路径可保持`C:\Program Files\MySQL\MySQL Server 8.0`。
3. 配置类型选择开发电脑或按实际用途选择；启用TCP/IP，端口3306。
4. 本机使用不需要勾选向外网开放3306防火墙。
5. 使用默认强密码认证方式，设置并妥善保存数据库root密码。
6. Windows服务名本文假定为`MySQL80`，启用开机自动启动。
7. 执行配置，确认初始化和启动服务成功。

如果提示VC运行库缺失，按MySQL安装程序的依赖检查安装微软官方对应x64运行库，再重试。[MySQL Windows安装说明](https://dev.mysql.com/doc/refman/8.0/en/windows-installation.html)

在管理员PowerShell确认服务：

```powershell
Get-Service -Name '*mysql*'
Get-Service -Name MySQL80
# 仅当该服务未运行时执行：
Start-Service -Name MySQL80
```

若安装时采用不同服务名，后文`MySQL80`全部替换为真实名称。不要为已有服务器再初始化数据目录，也不要用`--initialize`去“修复”一个有数据的实例。

普通PowerShell设置客户端路径：

```powershell
$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$MySql = Join-Path $MySqlBin 'mysql.exe'
if (-not (Test-Path -LiteralPath $MySql)) { throw 'Set MySqlBin to the actual MySQL bin directory.' }
& $MySql --version
Test-NetConnection 127.0.0.1 -Port 3306
```

### 3.4 可选：Git

已有完整交付ZIP时跳过。需要Git时在管理员PowerShell：

```powershell
winget install --id Git.Git --exact --source winget --accept-source-agreements --accept-package-agreements
```

新终端执行`git --version`确认可用。

## 4 获取同一版本的完整代码

### 4.1 使用交付包

将交付包解压到`C:\Apps\SupermarketInventory`，目录下应直接有`backend`、`frontend`、`doc`、`scripts`，不要多套一层同名目录。包内须包含`backend/.mvn/wrapper/maven-wrapper.properties`及`frontend/package-lock.json`。

不要把旧电脑的`node_modules`和`backend/target`当作新机依赖。若仅获取源码，私有`application-local.yml`和AI密钥不会随Git提供，后文会重新创建；如果是业务迁移，按第12章单独迁移私有运行数据。

### 4.2 使用Git获取代码

仅当维护者已经把**当前交付版本**推送到远程时使用：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
if (Test-Path -LiteralPath $ProjectRoot) { throw 'Choose an empty destination or use the existing verified delivery directory.' }
New-Item -ItemType Directory -Path 'C:\Apps' -Force | Out-Null
git clone https://github.com/Nxc100/Supermarket-Inventory-Management-System.git $ProjectRoot
if ($LASTEXITCODE -ne 0) { throw 'Clone failed. Check repository access or use the delivery ZIP.' }
Set-Location -LiteralPath $ProjectRoot
git rev-parse HEAD
```

本地未提交、未推送的修改不包含在`git clone`或`git archive HEAD`里。部署时以维护者交付的版本为准，不要假定远程默认分支一定等于此前验收工作区。

### 4.3 检查目录和Wrapper

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$requiredFiles = @(
    'backend\pom.xml',
    'backend\mvnw.cmd',
    'backend\.mvn\wrapper\maven-wrapper.properties',
    'frontend\package.json',
    'frontend\package-lock.json',
    'doc\数据库\supermarket_inventory.sql',
    'scripts\start-backend.ps1'
)
foreach ($relativePath in $requiredFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $ProjectRoot $relativePath))) {
        throw "Missing delivery file: $relativePath"
    }
}
Set-Location -LiteralPath (Join-Path $ProjectRoot 'backend')
.\mvnw.cmd -v
if ($LASTEXITCODE -ne 0) { throw 'Maven Wrapper failed. Verify JAVA_HOME and HTTPS connectivity.' }
```

期望看到Maven 3.9.9、Java 17。首次执行需要下载Maven；首次构建还需下载Java依赖。

## 5 新空库初始化与应用数据库账号

> **本章只适用于新空库。** 唯一基准SQL包含`DROP TABLE`，重复导入会覆盖已有业务。已有业务迁移请跳到第12章；应用启动不会自动建库、建表或填种子。

### 5.1 复制SQL到不含中文的辅助路径

为减少Windows客户端路径和控制台编码问题，复制文件字节，不通过PowerShell文本管道重写SQL：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$DeployDir = Join-Path $ProjectRoot '.local\deploy'
New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'doc\数据库\supermarket_inventory.sql') -Destination (Join-Path $DeployDir 'supermarket_inventory.sql')
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'doc\数据库\03_verify.sql') -Destination (Join-Path $DeployDir '03_verify.sql')
```

### 5.2 登录并确认目标库不存在

```powershell
$MySql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
& $MySql --host=127.0.0.1 --port=3306 --protocol=TCP --user=root --password --default-character-set=utf8mb4
```

密码在提示符输入，不在命令行的`--password`后填写明文。进入`mysql>`后执行 **SQL**：

```sql
SELECT VERSION(), CURRENT_USER();
SELECT SCHEMA_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'supermarket_inventory';
```

第二条必须返回空结果。如果已存在该库，先确认来源，停止本章初始化；不要继续复制下一段。

### 5.3 创建并导入

继续在同一个`mysql>`中执行，一步成功后再执行下一步：

```sql
CREATE DATABASE supermarket_inventory
CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE supermarket_inventory;
SELECT DATABASE();
SET SESSION time_zone = '+08:00';
SOURCE C:/Apps/SupermarketInventory/.local/deploy/supermarket_inventory.sql;
```

不能出现`ERROR`。若中途报错，先保留错误并排查，DDL不是整份文件可回滚事务；不要继续启动半初始化数据库，也不要用`--force`忽略错误。只有确认是本次新建的无业务库时，才由部署者决定重建。

数据库基准文件只用这一份，不再执行历史`01_schema.sql`、`02_seed.sql`。

### 5.4 检查结构和初始数据

在`mysql>`执行：

```sql
SELECT TABLE_TYPE, COUNT(*) AS object_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'supermarket_inventory'
GROUP BY TABLE_TYPE;

SELECT COUNT(*) AS users FROM sys_user;
SELECT COUNT(*) AS products FROM product;
SELECT COUNT(*) AS suppliers FROM supplier;
SELECT COUNT(*) AS usage_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'supermarket_inventory'
  AND TABLE_NAME = 'ai_suggestion'
  AND COLUMN_NAME IN ('prompt_tokens','completion_tokens','total_tokens','cached_tokens','provider_request_id');

SOURCE C:/Apps/SupermarketInventory/.local/deploy/03_verify.sql;
```

当前文件已更新为**2026-11-11 17:04:31的业务库导出快照**，不是最早的演示种子。导入期望为**16张基础表、8个视图、9个账号、36个商品、11个供应商、18张采购、15张销售、12张盘点、13条建议和5个AI用量字段**；后续重新导出时行数还会变化。29项一致性检查的`violations`应均为0；后续状态、库存等结果表不是错误。不要为了符合旧文档的演示数量删除当前业务。

这份快照还包含管理员保存的模型配置密文。若要保留原API Key使用能力，必须单独迁移导出机器的AI主密钥；数据库导入不会生成可解密原密文的新主密钥。图片也需要单独复制，见第12章。

### 5.5 创建后端运行账号

在root的`mysql>`执行以下SQL，先把示例密码替换为本机自行设置的强密码；后续配置文件必须填写同一密码。建议使用不含单引号和反斜线的随机密码，避免手工SQL转义错误。

```sql
CREATE USER 'inventory_app'@'127.0.0.1'
IDENTIFIED BY 'Replace_With_Your_Own_Db_Password_2026!';
GRANT SELECT, INSERT, UPDATE, DELETE
ON supermarket_inventory.* TO 'inventory_app'@'127.0.0.1';
SHOW GRANTS FOR 'inventory_app'@'127.0.0.1';
EXIT;
```

此账号用于运行现有业务，不授予建库、删表或全局管理权限。初始化、备份、测试建库及旧库升级由有相应权限的管理账号执行；不要为解决升级权限错误给运行账号全局root权限。

测试该账号的TCP连接及视图读取：

```powershell
$MySql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
& $MySql --host=127.0.0.1 --port=3306 --protocol=TCP --user=inventory_app --password --default-character-set=utf8mb4 --database=supermarket_inventory --execute="SELECT CURRENT_USER(),DATABASE(); SELECT COUNT(*) AS products FROM product; SELECT COUNT(*) AS sales_events FROM v_sale_event;"
if ($LASTEXITCODE -ne 0) { throw 'Application database account verification failed.' }
```

若使用远程数据库，账号Host、JDBC地址、TLS及网络授权需要按实际部署设计，本文默认数据库与应用在同一台Windows电脑。

## 6 创建后端本地配置

### 6.1 一次性生成配置，交互输入数据库密码

在运行项目的**普通PowerShell**执行。脚本生成随机JWT密钥，以UTF-8无BOM写入后端工作目录；使用JSON字符串编码生成合法YAML标量，避免密码中的冒号、井号、引号等破坏配置。

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$configFile = Join-Path $ProjectRoot 'backend\application-local.yml'
if (Test-Path -LiteralPath $configFile) { throw 'Configuration already exists. Review and edit it; do not overwrite it with a new JWT secret.' }
$dbPasswordSecure = Read-Host 'Enter the inventory_app database password' -AsSecureString
$dbPasswordPlain = [System.Net.NetworkCredential]::new('', $dbPasswordSecure).Password
if ([string]::IsNullOrEmpty($dbPasswordPlain)) { throw 'Database password must not be empty.' }
$passwordYaml = ConvertTo-Json -InputObject $dbPasswordPlain -Compress
$jwtBytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($jwtBytes) } finally { $rng.Dispose() }
$jwtSecret = [Convert]::ToBase64String($jwtBytes)
$localYaml = @"
spring:
  datasource:
    url: "jdbc:mysql://127.0.0.1:3306/supermarket_inventory?connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8"
    username: inventory_app
    password: $passwordYaml
server:
  address: 127.0.0.1
  port: 8080
app:
  jwt-secret: "$jwtSecret"
  upload-directory: .local/uploads
  ai:
    config-master-file: .local/secrets/ai-master.key
    model: qwen-plus
    timeout-seconds: 15
"@
[IO.File]::WriteAllText($configFile, $localYaml, [Text.UTF8Encoding]::new($false))
$runAccount = [Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $configFile /inheritance:r /grant:r "${runAccount}:(F)" '*S-1-5-18:(F)'
if ($LASTEXITCODE -ne 0) { throw 'Review permissions on the private configuration file.' }
Remove-Variable dbPasswordPlain,passwordYaml,jwtSecret,localYaml,dbPasswordSecure,jwtBytes
Write-Host 'Private configuration created. Do not commit or share its contents.'
```

这里只生成后端私有配置，没有把数据库密码写入前端。后端`src/main/resources/application-local.yml`是导入指针，不要把私密值填到这个受版本控制的资源文件中。新机连接以自己设置的应用账号密码为准，不依赖共享`application.yml`中的默认回退值。

### 6.2 必须理解的配置规则

| 设置 | 规则 |
| --- | --- |
| 工作目录 | **必须为`C:\Apps\SupermarketInventory\backend`**，不能在任意目录直接`java -jar` |
| 配置文件 | 后端工作目录下`application-local.yml`，不随源码Git传输 |
| Profile | 默认`local`；若外部设置了其他`SPRING_PROFILES_ACTIVE`，应核实配置加载方式 |
| JWT | 至少32字节，本文生成48随机字节；日常重启不重新生成 |
| 图片 | 默认保存在`backend/.local/uploads`，更新程序时保留 |
| AI主密钥 | 默认保存在`backend/.local/secrets/ai-master.key`，与数据库密文配套备份 |
| 数据库时间 | JDBC会话固定`+08:00`，后端业务时区为北京时间 |

检查系统时间是否准确即可，不必强行更改新电脑的系统时区：

```powershell
Get-Date
Get-TimeZone
```

若从同一终端先运行过测试，确认没有遗留外部覆盖变量。可以只列名称，不输出值：

```powershell
Get-ChildItem Env: | Where-Object {
    $_.Name -match '^(DB_|TEST_DB_|JWT_SECRET|SPRING_|SERVER_|DASHSCOPE_|AI_CONFIG_|UPLOAD_DIR)'
} | Select-Object Name
```

显式本地YAML值通常优先于资源文件中环境占位符的默认配置，例如本地已直接写URL时，单改`DB_URL`不能保证切库；而`SPRING_DATASOURCE_URL`等直接映射属性的环境变量又可能覆盖YAML。不要同时保留多套冲突设置；本文以生成的本地文件为准，后续测试通过显式profile排除默认local导入。

## 7 首次构建与本机启动

### 7.1 构建后端

在普通PowerShell：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath (Join-Path $ProjectRoot 'backend')
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to the installed JDK 17 directory.' }
& "$env:JAVA_HOME\bin\java.exe" -version
.\mvnw.cmd '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw 'Backend package failed. Fix the first Maven error before proceeding.' }
Get-Item -LiteralPath '.\target\inventory-1.0.0-SNAPSHOT.jar' | Select-Object Name,Length,LastWriteTime
```

此处跳过测试是因为还没有准备独立测试库，只用于得到可运行JAR；不代表全量验收通过。需要完整检查时按第11章执行`verify`。

### 7.2 终端A：启动后端

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath $ProjectRoot
powershell.exe -NoProfile -ExecutionPolicy Bypass -File '.\scripts\start-backend.ps1' -JavaHome $env:JAVA_HOME
```

保持此窗口打开。期望日志包含`Started InventoryApplication`及8080启动信息，没有`APPLICATION FAILED TO START`。后端启动本身不检查云端Key是否可用。

也可直接运行刚打好的JAR，二选一，不要两个都启动：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath (Join-Path $ProjectRoot 'backend')
& "$env:JAVA_HOME\bin\java.exe" -jar '.\target\inventory-1.0.0-SNAPSHOT.jar'
```

### 7.3 终端B：验证后端

```powershell
Test-NetConnection 127.0.0.1 -Port 8080
try {
    $response = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/v1/auth/me' -UseBasicParsing -TimeoutSec 10
    throw "Unexpected unauthenticated status: $($response.StatusCode)"
} catch {
    if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) { throw }
    Write-Host 'Backend responds with expected HTTP 401 before login.'
}
```

未登录的`/auth/me`返回401表示安全入口正常响应，不是部署失败。可选打开接口文档：`http://127.0.0.1:8080/swagger-ui.html`。

### 7.4 终端B：安装和启动前端

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath (Join-Path $ProjectRoot 'frontend')
node --version
npm.cmd ci
if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
$env:BACKEND_PROXY_TARGET = 'http://127.0.0.1:8080'
npm.cmd run dev
```

保持此窗口打开。出现`http://127.0.0.1:5173`后，用Edge/Chrome访问该地址。端口5173采用严格占用检查，不会自动换成另一个端口。

### 7.5 首次登录和演示数据

| 角色 | 原演示账号 | 原始演示密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Test123456` |
| 采购员 | `buyer01` | `Test123456` |
| 收银员 | `cashier01` | `Test123456` |

以上密码只适用于未改过密码的原始演示账号。**当前SQL导出保留导出时的密码散列、账号状态和权限，新机应使用导出前实际有效的业务密码**，不能保证所有账号仍使用原始密码。这些业务账号和MySQL的root、`inventory_app`完全不同。数据库还包含其他账号，管理员可在用户管理中查看；通过个人中心和用户管理维护口令及启用状态，不直接改数据库散列。

当前唯一SQL包含原演示资料及后续业务单据、历史日期、活动盘点、AI夹具和真实调用记录，不是一个纯空业务系统。旧批次可能已经过期，部分商品可能被原盘点冻结；测试新采购/销售时应新建专用商品、登记当前真实日期并审核入库。数据库只保存图片引用，迁移后缺少实际文件时页面使用占位图，应恢复原上传目录或重新上传。

### 7.6 构建前端并预览

停止前端开发服务后，或另开终端只执行构建：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath (Join-Path $ProjectRoot 'frontend')
npm.cmd run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
Test-Path -LiteralPath '.\dist\index.html'
$env:BACKEND_PROXY_TARGET = 'http://127.0.0.1:8080'
npm.cmd run preview
```

访问`http://127.0.0.1:4173`。开发服务和预览服务都提供当前仓库定义的代理；**Vite preview用于本地检查构建产物，不是长期生产服务器**，持续运行采用第9章静态托管方案。[Vite部署说明](https://vite.dev/guide/static-deploy.html)

## 8 配置并验证通义千问

### 8.1 推荐：通过管理员界面配置

1. 登录管理员，进入左侧“模型配置与用量”。
2. 在阿里云百炼开通相应模型服务，取得适用地域/工作空间的API Key。
3. 选择设置新API Key，输入密钥；填写默认和允许模型，例如`qwen-plus`、`qwen-flash`、`qwen-max`。只填写该云账号实际可用、支持当前文本流式聊天参数的模型。
4. 默认模型必须在允许列表中；保存成功后旧Key不会回显。
5. 全新管理配置首次保存Key会生成`backend/.local/secrets/ai-master.key`。当前导出SQL已经包含管理密文时，应先恢复原机对应主密钥；若无法取得原密钥，由管理员重新输入API Key并选择替换保存，建立新机密文配置。把实际使用的主密钥纳入私有备份，不要手动改成文本或随机覆盖。
6. 进入“智能经营建议”，选择允许的模型，采集真实摘要，核对后确认生成。
7. 看到逐段正文并最终显示成功，再去历史和用量页核对模型、时间和Token；夹具记录不算真实云调用。

Key、默认模型及允许列表由管理员保存；已保存管理Key优先于本地Key来源。API Key本身必须与调用地域/工作空间匹配，管理页面不自动改变API端点。

### 8.2 端点和网络

仓库默认完整聊天端点：

```text
https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
```

该地址在本项目已有真实调用证据。新云账号如使用不同地域或工作空间专属端点，应以控制台提供的**兼容Chat Completions完整地址**为准，在本地配置`app.ai.endpoint`中填写；不能只填`/v1`基础地址，也不能误填Responses接口。阿里云产品和新工作空间接入方式可能变化，部署时核对[百炼官方接入文档](https://help.aliyun.com/zh/model-studio/what-is-model-studio)。

可在当前配置`app.ai`下补充，例如默认地域场景：

```yaml
    endpoint: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
```

修改本地文件后重启后端。网络检查只验证HTTPS端口可达，不代表Key或模型授权成功：

```powershell
Test-NetConnection dashscope.aliyuncs.com -Port 443
```

### 8.3 可选：使用本地Key文件

若不通过管理界面保存Key，可将仅含Key的UTF-8文件置于`backend/.local/secrets/dashscope.key`，在当前`app.ai`配置下增加：

```yaml
    key-file: .local/secrets/dashscope.key
```

使用文本编辑器在本机填写文件，并限制其权限；不要将密钥写入前端`.env`、Git、SQL、部署日志或命令参数。此前机器的`doc/key.txt`不会随源码自动存在，新机不能沿用一个不存在的路径。若库中已有管理密文，先在管理页选择切回本地配置才能使用该文件来源。

未配置千问不影响采购、销售等核心业务；生成时会明确失败。用量未知显示未知，不按0伪造；总Token不是阿里云账户账单，本文也不提供价格估算。

## 9 打包运行：JAR + Nginx

本章用于已经完成第5～8章后，希望使用构建产物持续运行的单机或小规模局域网环境。它是新增的部署配置示例，不是仓库原本自带的一键安装器。Windows版Nginx官方仍有平台和扩展性限制，大规模正式服务应另做容量和可用性设计。[Nginx Windows说明](https://nginx.org/en/docs/windows.html)

### 9.1 准备固定运行JAR

先停止终端A原后端，避免两个实例占用8080；前端开发服务也可停止。构建完成后，将JAR复制到固定运行位置，避免以后构建覆盖运行文件。

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$BackendRoot = Join-Path $ProjectRoot 'backend'
Set-Location -LiteralPath $BackendRoot
.\mvnw.cmd '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
New-Item -ItemType Directory -Path '.\.local\runtime' -Force | Out-Null
New-Item -ItemType Directory -Path '.\.local\logs' -Force | Out-Null
$RuntimeJar = Join-Path $BackendRoot '.local\runtime\inventory.jar'
if (Test-Path -LiteralPath $RuntimeJar) { throw 'A runtime JAR already exists. Use the controlled update procedure in chapter 12.' }
Copy-Item -LiteralPath '.\target\inventory-1.0.0-SNAPSHOT.jar' -Destination $RuntimeJar
& "$env:JAVA_HOME\bin\java.exe" -jar $RuntimeJar '--logging.file.name=.local/logs/backend.log'
```

此窗口继续运行后端。日志保存在`backend/.local/logs/backend.log`；启动目录仍然必须为`backend`。

### 9.2 构建静态页面

另开普通PowerShell：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath (Join-Path $ProjectRoot 'frontend')
npm.cmd ci
if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
npm.cmd run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
Get-Item -LiteralPath '.\dist\index.html'
```

Nginx读取`frontend/dist`。浏览器包使用同源`/api`和`/media`，不需要在构建时硬编码服务器IP，也不需要新增`VITE_API_URL`；当前源码没有通过这个变量配置API基础地址。

### 9.3 下载并解压 Nginx

从[Nginx官方页面](https://nginx.org/en/download.html)下载Windows ZIP。官方Windows说明建议选择包含修复的mainline分支；根据部署当天页面选取具体版本，将ZIP放在下载目录。不要下载Linux的`.tar.gz`。

在普通PowerShell中把下面第一行改为实际ZIP路径：

```powershell
$NginxZip = 'C:\Users\你的Windows用户名\Downloads\nginx-具体版本.zip'
$NginxRoot = 'C:\Apps\nginx'
if (-not (Test-Path -LiteralPath $NginxZip)) { throw 'Set NginxZip to the downloaded Windows ZIP.' }
if (Test-Path -LiteralPath $NginxRoot) { throw 'Nginx destination already exists. Do not overwrite another installation.' }
$NginxStage = Join-Path 'C:\Apps' ('nginx-unpack-' + [Guid]::NewGuid().ToString('N'))
Expand-Archive -LiteralPath $NginxZip -DestinationPath $NginxStage
$NginxFolders = @(Get-ChildItem -LiteralPath $NginxStage -Directory | Where-Object {
    Test-Path -LiteralPath (Join-Path $_.FullName 'nginx.exe')
})
if ($NginxFolders.Count -ne 1) { throw 'Cannot identify the unpacked Windows Nginx directory.' }
Copy-Item -LiteralPath $NginxFolders[0].FullName -Destination $NginxRoot -Recurse
if (-not (Test-Path -LiteralPath (Join-Path $NginxRoot 'nginx.exe'))) { throw 'Nginx extraction failed.' }
```

上面只创建新目录并复制文件，不覆盖已有安装。临时解压目录可在确认安装正常后手工清理。

### 9.4 生成完整 Nginx 配置

此配置先只开放本机8088，包含SPA刷新回退、图片上传限制、普通API代理及SSE专用代理。`proxy_pass`保留原路径，没有去掉`/api/v1`。

```powershell
$NginxRoot = 'C:\Apps\nginx'
$nginxConf = Join-Path $NginxRoot 'conf\nginx.conf'
Copy-Item -LiteralPath $nginxConf -Destination (Join-Path $NginxRoot 'conf\nginx.conf.original')
$nginxText = @'
worker_processes 1;
error_log logs/error.log warn;
pid logs/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include mime.types;
    default_type application/octet-stream;
    access_log logs/access.log;
    sendfile on;
    keepalive_timeout 30;
    server_tokens off;

    server {
        listen 127.0.0.1:8088;
        server_name localhost;
        root C:/Apps/SupermarketInventory/frontend/dist;
        index index.html;
        client_max_body_size 3m;

        location /api/v1/suggestions/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Connection "";
            proxy_connect_timeout 5s;
            proxy_send_timeout 30s;
            proxy_read_timeout 60s;
            proxy_buffering off;
            proxy_request_buffering off;
            proxy_cache off;
            gzip off;
            proxy_next_upstream off;
        }

        location /api/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Connection "";
            proxy_connect_timeout 5s;
            proxy_send_timeout 20s;
            proxy_read_timeout 20s;
            proxy_next_upstream off;
        }

        location /media/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_set_header Host $host;
            proxy_connect_timeout 5s;
            proxy_read_timeout 20s;
        }

        location /assets/ {
            try_files $uri =404;
            add_header Cache-Control "public, max-age=604800, immutable";
        }

        location = /index.html {
            add_header Cache-Control "no-store";
        }

        location / {
            try_files $uri $uri/ /index.html;
        }
    }
}
'@
[IO.File]::WriteAllText($nginxConf, $nginxText, [Text.UTF8Encoding]::new($false))
```

使用单引号here-string是为了保留Nginx的`$uri`、`$host`等变量，不被PowerShell提前展开。示例不启用代理写请求自动重试；销售、采购和盘点遇到未知结果仍走项目自己的核实流程。

### 9.5 检查、启动和停止 Nginx

```powershell
$NginxRoot = 'C:\Apps\nginx'
Set-Location -LiteralPath $NginxRoot
.\nginx.exe -p 'C:/Apps/nginx/' -c 'conf/nginx.conf' -t
if ($LASTEXITCODE -ne 0) { throw 'Nginx configuration test failed.' }
Start-Process -FilePath (Join-Path $NginxRoot 'nginx.exe') -ArgumentList @('-p','C:/Apps/nginx/','-c','conf/nginx.conf') -WorkingDirectory $NginxRoot -WindowStyle Hidden
```

启动一次即可。改配置后的检查与重载：

```powershell
Set-Location -LiteralPath 'C:\Apps\nginx'
.\nginx.exe -p 'C:/Apps/nginx/' -c 'conf/nginx.conf' -t
if ($LASTEXITCODE -ne 0) { throw 'Do not reload invalid configuration.' }
.\nginx.exe -p 'C:/Apps/nginx/' -c 'conf/nginx.conf' -s reload
```

平稳停止：

```powershell
Set-Location -LiteralPath 'C:\Apps\nginx'
.\nginx.exe -p 'C:/Apps/nginx/' -c 'conf/nginx.conf' -s quit
```

### 9.6 验证打包入口

```powershell
(Invoke-WebRequest -Uri 'http://127.0.0.1:8088/' -UseBasicParsing -TimeoutSec 10).StatusCode
(Invoke-WebRequest -Uri 'http://127.0.0.1:8088/login' -UseBasicParsing -TimeoutSec 10).StatusCode
```

两次应为200。打开`http://127.0.0.1:8088`登录，然后直接刷新采购详情或销售列表等内部路由，页面应正常。图片应通过同源`/media`出现；真实模型生成应逐段显示，若一次性等到最后才出现，检查SSE代理缓冲及上层代理。

此方案不再需要运行Vite。Nginx和后端必须同时运行；仅有前端首页200不能证明API正常。

### 9.7 可选：供同一局域网其他电脑访问

把Nginx的监听行改成`listen 0.0.0.0:8088;`，检查配置后重载。后端和MySQL继续只对本机提供服务，不必开放8080和3306。

管理员PowerShell仅允许私有网络的本地子网访问入口：

```powershell
New-NetFirewallRule -DisplayName 'Supermarket Inventory HTTP 8088' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8088 -Profile Private -RemoteAddress LocalSubnet
Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
    $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*'
} | Select-Object InterfaceAlias,IPAddress
```

其他电脑访问`http://服务器局域网IP:8088`，例如`http://192.168.1.20:8088`。客户端的`127.0.0.1`永远指向客户端自己。只有实际可信局域网才设为Private配置；不为排障关闭整机防火墙。

跨公网或不可信网络使用时应为入口配置可信域名和HTTPS，保护登录口令及Bearer Token。本文8088 HTTP示例用于本机或受控局域网，没有假设已经配置公网证书。停止共享后可移除本条防火墙规则：

```powershell
Remove-NetFirewallRule -DisplayName 'Supermarket Inventory HTTP 8088'
```

## 10 可选：Windows 登录后自动启动

本章使用Windows计划任务，在**同一个运行账号登录后**启动固定JAR和Nginx，避免依赖手工保留终端。它不是无人登录也运行的完整Windows服务方案。若要求开机前台无人登录即长期运行，应另行配置专用服务托管、服务账号、文件ACL及恢复策略；不能直接把`java -jar`当作原生Windows服务注册。

### 10.1 先停止原手工实例

后端手工窗口按`Ctrl+C`，Nginx使用9.5的`-s quit`；确认8080和8088均已释放。不要让计划任务和手工启动竞争同一端口。

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in 8080,8088 } |
    Select-Object LocalAddress,LocalPort,OwningProcess
```

### 10.2 生成两个启动文件

在当前运行账号的PowerShell执行，脚本写成UTF-8带BOM以兼容Windows PowerShell 5.1：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$DeployDir = Join-Path $ProjectRoot '.local\deploy'
New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
$backendLauncher = @'
$ErrorActionPreference = 'Stop'
try {
    $backendRoot = 'C:\Apps\SupermarketInventory\backend'
    $runtimeJar = Join-Path $backendRoot '.local\runtime\inventory.jar'
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
    if (-not $javaHome) { $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
    if (-not $javaHome) { throw 'JAVA_HOME is not configured.' }
    $javaExe = Join-Path $javaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe)) { throw 'Java executable not found.' }
    if (-not (Test-Path -LiteralPath $runtimeJar)) { throw 'Runtime JAR not found.' }
    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
        throw 'Port 8080 is already occupied. Do not start another backend.'
    }
    $logDir = Join-Path $backendRoot '.local\logs'
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    $process = Start-Process -FilePath $javaExe -ArgumentList @('-jar', "`"$runtimeJar`"", '--logging.file.name=.local/logs/backend.log') -WorkingDirectory $backendRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'backend-console.out.log') -RedirectStandardError (Join-Path $logDir 'backend-console.err.log') -Wait -PassThru
    exit $process.ExitCode
} catch {
    Write-Error $_
    exit 1
}
'@
$webLauncher = @'
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath 'C:\Apps\nginx'
if (Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 8088 is already occupied. Do not start another web server.'
}
& '.\nginx.exe' -p 'C:/Apps/nginx/' -c 'conf/nginx.conf' -t
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Start-Process -FilePath 'C:\Apps\nginx\nginx.exe' -ArgumentList @('-p','C:/Apps/nginx/','-c','conf/nginx.conf') -WorkingDirectory 'C:\Apps\nginx' -WindowStyle Hidden
'@
[IO.File]::WriteAllText((Join-Path $DeployDir 'Start-Backend.ps1'), $backendLauncher, [Text.UTF8Encoding]::new($true))
[IO.File]::WriteAllText((Join-Path $DeployDir 'Start-Web.ps1'), $webLauncher, [Text.UTF8Encoding]::new($true))
```

这些是按本文生成的本机辅助文件，原仓库没有预置；部署路径或端口变化时要同步编辑。不要切换到SYSTEM或另一个Windows账号直接运行，否则自动生成的仅所有者可读AI主密钥可能无法解密。

### 10.3 注册并启动任务

使用同一Windows账号执行；如果注册被Windows拒绝，可用该账号的管理员PowerShell完成注册，任务本身仍按以下Limited身份运行。

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$DeployDir = Join-Path $ProjectRoot '.local\deploy'
$runAccount = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$principal = New-ScheduledTaskPrincipal -UserId $runAccount -LogonType Interactive -RunLevel Limited
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $runAccount
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
$backendFile = Join-Path $DeployDir 'Start-Backend.ps1'
$webFile = Join-Path $DeployDir 'Start-Web.ps1'
$backendAction = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$backendFile`"" -WorkingDirectory $ProjectRoot
$webAction = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$webFile`"" -WorkingDirectory $ProjectRoot
Register-ScheduledTask -TaskName 'SupermarketInventory-Backend' -Action $backendAction -Trigger $trigger -Principal $principal -Settings $settings
Register-ScheduledTask -TaskName 'SupermarketInventory-Web' -Action $webAction -Trigger $trigger -Principal $principal -Settings $settings
Start-ScheduledTask -TaskName 'SupermarketInventory-Backend'
Start-ScheduledTask -TaskName 'SupermarketInventory-Web'
```

确认任务未同名存在后再注册；不使用`-Force`覆盖其他已有任务。MySQL本身由`MySQL80`系统服务自启。

### 10.4 查看状态和关闭自启

```powershell
Get-ScheduledTask -TaskName 'SupermarketInventory-*' | Select-Object TaskName,State
Get-ScheduledTaskInfo -TaskName 'SupermarketInventory-Backend'
Get-Content -LiteralPath 'C:\Apps\SupermarketInventory\backend\.local\logs\backend.log' -Tail 60
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection 127.0.0.1 -Port 8088
```

后端任务正常时可能一直是Running；Web启动任务完成后可以是Ready，但Nginx进程仍在运行，因此须以监听端口和页面检查共同判断。计划任务重试只负责进程启动失败，不能代替完整健康监测。

停用登录自启：

```powershell
Disable-ScheduledTask -TaskName 'SupermarketInventory-Backend'
Disable-ScheduledTask -TaskName 'SupermarketInventory-Web'
Stop-ScheduledTask -TaskName 'SupermarketInventory-Backend'
Set-Location -LiteralPath 'C:\Apps\nginx'
.\nginx.exe -p 'C:/Apps/nginx/' -c 'conf/nginx.conf' -s quit
```

停止计划任务后核对8080；如果其Java子进程仍在，按第13章识别完整命令行后只结束本项目的PID。停用任务不删除数据库、上传目录或密钥。

## 11 可选：在新机执行质量检查和独立验收

基础部署不要求先装Python或运行所有测试，但首次换数据库补丁、调整部署配置或准备正式交付时，应完成相应验收。所有写测试使用独立副本，不指向`supermarket_inventory`。

### 11.1 准备 Python 工具环境

可从[Python官方Windows下载页](https://www.python.org/downloads/windows/)安装Python 3.12 x64，保留Python Launcher。下列环境位于项目忽略目录，不污染系统包：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath $ProjectRoot
py -3.12 --version
py -3.12 -m venv '.\.local\tools-venv'
$ToolsPython = Join-Path $ProjectRoot '.local\tools-venv\Scripts\python.exe'
& $ToolsPython -m pip install PyMySQL requests bcrypt
if ($LASTEXITCODE -ne 0) { throw 'Python tool dependencies failed to install.' }
```

### 11.2 后端完整验证

打开**新的专用测试PowerShell**，不复用正式启动窗口。下面使用有权创建测试库的数据库管理账号，密码交互输入。使用`test` profile使私有正式`application-local.yml`不会作为默认local配置载入。

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$ToolsPython = Join-Path $ProjectRoot '.local\tools-venv\Scripts\python.exe'
Set-Location -LiteralPath $ProjectRoot
if ($env:SPRING_CONFIG_LOCATION -or $env:SPRING_CONFIG_ADDITIONAL_LOCATION -or $env:SPRING_CONFIG_IMPORT -or $env:SPRING_APPLICATION_JSON -or (Get-ChildItem Env: | Where-Object { $_.Name -like 'SPRING_DATASOURCE_*' })) {
    throw 'Use a clean test terminal without external Spring configuration overrides.'
}
$testPasswordSecure = Read-Host 'Enter the MySQL test administrator password' -AsSecureString
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', $testPasswordSecure).Password
$env:DB_USER = 'root'
$env:SPRING_PROFILES_ACTIVE = 'test'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/supermarket_inventory_test?connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8'
$env:TEST_DB_URL = $env:DB_URL
& $ToolsPython '.\scripts\prepare-test-db.py' --database supermarket_inventory_test
if ($LASTEXITCODE -ne 0) { throw 'Test database preparation failed. Do not reset an existing database without checking its purpose.' }
Set-Location -LiteralPath (Join-Path $ProjectRoot 'backend')
.\mvnw.cmd verify
if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed. Inspect target/surefire-reports and the first error.' }
```

`verify`运行测试、打包及Spotless检查，既有最新记录为133项，但新机必须依据本次实际输出判断。测试脚本拒绝非空库，`--reset`只在明确要重建该测试副本时单独使用，不作为日常启动步骤。

测试完成关闭这个终端，避免root密码和测试profile被后续正式启动继承。不要在这里调用普通正式启动脚本。

### 11.3 前端检查

另一个普通PowerShell：

```powershell
Set-Location -LiteralPath 'C:\Apps\SupermarketInventory\frontend'
npm.cmd run lint
if ($LASTEXITCODE -ne 0) { throw 'Frontend lint failed.' }
npm.cmd run test:unit
if ($LASTEXITCODE -ne 0) { throw 'Frontend unit tests failed.' }
npm.cmd run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend type check or production build failed.' }
```

`build`包含类型检查；既有前端单测记录为21项。若Nginx正在读取这个`dist`，先按更新维护流程暂停入口，避免构建过程替换线上文件。

### 11.4 可选：36项浏览器全站回归

需要完整源码和`tests/e2e`目录。为避免回归修改演示账号影响人工使用，另建`supermarket_inventory_deploy_ui_test`，服务端口8081、前端5174，与正式运行隔离。

**UI测试终端A：准备新的UI副本并启动专属后端。**

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$ToolsPython = Join-Path $ProjectRoot '.local\tools-venv\Scripts\python.exe'
Set-Location -LiteralPath $ProjectRoot
if ($env:SPRING_CONFIG_LOCATION -or $env:SPRING_CONFIG_ADDITIONAL_LOCATION -or $env:SPRING_CONFIG_IMPORT -or $env:SPRING_APPLICATION_JSON -or (Get-ChildItem Env: | Where-Object { $_.Name -like 'SPRING_DATASOURCE_*' })) {
    throw 'Use a clean UI test terminal without external Spring overrides.'
}
$testPasswordSecure = Read-Host 'Enter the MySQL test administrator password' -AsSecureString
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', $testPasswordSecure).Password
$env:DB_USER = 'root'
& $ToolsPython '.\scripts\prepare-test-db.py' --database supermarket_inventory_deploy_ui_test
if ($LASTEXITCODE -ne 0) { throw 'UI test database preparation failed.' }
$env:SPRING_PROFILES_ACTIVE = 'uiqa'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/supermarket_inventory_deploy_ui_test?connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '8081'
$env:UPLOAD_DIR = '.local/uiqa/uploads'
$env:AI_CONFIG_MASTER_FILE = '.local/uiqa/secrets/ai-master.key'
$env:DASHSCOPE_API_KEY = ''
$env:DASHSCOPE_API_KEY_FILE = ''
$qaJwtBytes = New-Object byte[] 48
$qaRng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $qaRng.GetBytes($qaJwtBytes) } finally { $qaRng.Dispose() }
$env:JWT_SECRET = [Convert]::ToBase64String($qaJwtBytes)
Set-Location -LiteralPath (Join-Path $ProjectRoot 'backend')
& "$env:JAVA_HOME\bin\java.exe" -jar '.\target\inventory-1.0.0-SNAPSHOT.jar'
```

`uiqa`是本章为隔离默认local导入而显式指定的profile，没有依赖一个未提供的`application-uiqa.yml`；基础配置由`application.yml`加上述环境变量提供。

**UI测试终端B：启动代理到8081的专属前端。**

```powershell
Set-Location -LiteralPath 'C:\Apps\SupermarketInventory\frontend'
$env:BACKEND_PROXY_TARGET = 'http://127.0.0.1:8081'
npm.cmd run dev -- --port 5174
```

**UI测试终端C：执行。** 使用Windows自带Edge，无需另下载Playwright浏览器：

```powershell
Set-Location -LiteralPath 'C:\Apps\SupermarketInventory\frontend'
$env:E2E_TEST_DATABASE = 'supermarket_inventory_deploy_ui_test'
$env:E2E_BASE_URL = 'http://127.0.0.1:5174'
$env:E2E_BROWSER_CHANNEL = 'msedge'
npm.cmd run test:e2e
if ($LASTEXITCODE -ne 0) { throw 'UI regression failed. Inspect test-results and retained traces.' }
```

必须同时核对后端实际数据库、前端代理目标和`E2E_TEST_DATABASE`，只改标记不能保证隔离。浏览器单worker、无自动重试；故障注入及部分模型流是明确测试夹具，不能把UI全绿当作真实阿里云调用成功。

结束后在专属A/B窗口按`Ctrl+C`并关闭三个窗口；正式8080、8088实例不受此步骤控制。

## 12 备份、迁移、更新与恢复

### 12.1 必须保存的内容

| 内容 | 默认位置 | 遗失影响 |
| --- | --- | --- |
| 业务数据库 | `supermarket_inventory` | 单据、权限、余额、历史及AI管理密文等丢失 |
| 用户图片 | `backend/.local/uploads` | 头像和商品图引用存在但文件丢失 |
| AI加密主密钥 | `backend/.local/secrets/ai-master.key` | 无法解密管理页已保存的Key |
| 私有配置 | `backend/application-local.yml` | 数据库接入、JWT、定制路径/端点丢失 |
| 运行JAR和前端产物 | 固定运行JAR、`frontend/dist` | 缺少对应程序版本，无法准确回滚 |
| 部署配置 | Nginx配置、生成的启动脚本 | 需要重新配置入口和自动启动 |

如采用自定义上传目录、`AI_CONFIG_MASTER_KEY`环境主密钥或其他主密钥路径，应备份实际来源，不能只复制默认路径。主密钥文件是**32字节二进制**，不是API Key文本，也不是JWT密钥。

### 12.2 一次完整备份

先暂停新操作并停止本项目后端，保证数据库导出与图片/配置处于同一维护时点；记录备份时间和代码版本。不要在业务持续写入时把文件复制和数据库快照当成严格同一时刻。

在**运行账号**PowerShell中，用有备份权限的数据库管理账号执行：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$backupStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$BackupDir = Join-Path 'C:\Backups\SupermarketInventory' $backupStamp
New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
$dumpFile = Join-Path $BackupDir 'database.sql'
& (Join-Path $MySqlBin 'mysqldump.exe') --host=127.0.0.1 --port=3306 --protocol=TCP --user=root --password --default-character-set=utf8mb4 --single-transaction --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF "--result-file=$dumpFile" supermarket_inventory
if ($LASTEXITCODE -ne 0) { throw 'Database backup failed. Do not treat this file as a complete backup.' }
if ((Get-Item -LiteralPath $dumpFile).Length -eq 0) { throw 'Database backup is empty.' }
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'backend\application-local.yml') -Destination (Join-Path $BackupDir 'application-local.yml')
$uploads = Join-Path $ProjectRoot 'backend\.local\uploads'
if (Test-Path -LiteralPath $uploads) {
    Copy-Item -LiteralPath $uploads -Destination (Join-Path $BackupDir 'uploads') -Recurse
}
$masterFile = Join-Path $ProjectRoot 'backend\.local\secrets\ai-master.key'
if (Test-Path -LiteralPath $masterFile) {
    Copy-Item -LiteralPath $masterFile -Destination (Join-Path $BackupDir 'ai-master.key')
}
$runtimeJar = Join-Path $ProjectRoot 'backend\.local\runtime\inventory.jar'
if (Test-Path -LiteralPath $runtimeJar) {
    Copy-Item -LiteralPath $runtimeJar -Destination (Join-Path $BackupDir 'inventory.jar')
}
$dist = Join-Path $ProjectRoot 'frontend\dist'
if (Test-Path -LiteralPath $dist) {
    Copy-Item -LiteralPath $dist -Destination (Join-Path $BackupDir 'dist') -Recurse
}
$nginxConf = 'C:\Apps\nginx\conf\nginx.conf'
if (Test-Path -LiteralPath $nginxConf) {
    Copy-Item -LiteralPath $nginxConf -Destination (Join-Path $BackupDir 'nginx.conf')
}
Get-FileHash -LiteralPath $dumpFile -Algorithm SHA256
Write-Host "Backup directory: $BackupDir"
```

使用`mysqldump --result-file`直接写文件，避免Windows PowerShell 5.1的`>`把SQL转成UTF-16。导出包含业务和私有配置，备份目录、外部备份介质也应按私密资料管理。[mysqldump官方说明](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html)

该命令没有使用`--databases`，不会主动在dump中添加建库/选库操作，便于控制恢复入口；但视图定义可能仍引用原库名，不能因此断言改一个目标库名就完成隔离恢复。

### 12.3 在新电脑恢复原业务

1. 完成第2～4章安装和获取同版本代码，先不启动应用。
2. 在新MySQL实例确认`supermarket_inventory`不存在。只创建同名空库，**不导入演示初始化SQL**。
3. 将备份`database.sql`复制到新机`C:\Apps\SupermarketInventory\.local\deploy\restore.sql`，核对传输前后的SHA256。
4. 以数据库管理账号执行下面的恢复命令；前提是已按第5.2检查目标库不存在并明确创建了空库。

复制备份到导入路径的PowerShell命令，先更改实际备份目录：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$BackupDir = 'C:\Backups\SupermarketInventory\实际备份时间目录'
$DeployDir = Join-Path $ProjectRoot '.local\deploy'
New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
$sourceDump = Join-Path $BackupDir 'database.sql'
$restoreDump = Join-Path $DeployDir 'restore.sql'
if (-not (Test-Path -LiteralPath $sourceDump)) { throw 'Set BackupDir to a complete transferred backup.' }
if (Test-Path -LiteralPath $restoreDump) { throw 'An import file already exists. Confirm its identity before replacement.' }
Copy-Item -LiteralPath $sourceDump -Destination $restoreDump
if ((Get-FileHash -LiteralPath $sourceDump -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $restoreDump -Algorithm SHA256).Hash) {
    throw 'Transferred restore copy does not match the source backup.'
}
```

在`mysql>`创建库的SQL：

```sql
CREATE DATABASE supermarket_inventory
CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
EXIT;
```

PowerShell导入：

```powershell
$MySql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
& $MySql --host=127.0.0.1 --port=3306 --protocol=TCP --user=root --password --default-character-set=utf8mb4 --database=supermarket_inventory --execute="source C:/Apps/SupermarketInventory/.local/deploy/restore.sql"
if ($LASTEXITCODE -ne 0) { throw 'Restore failed. Keep the application stopped and inspect the error.' }
```

5. 按5.5为新MySQL创建应用数据库账号；逻辑库dump不会备份MySQL系统用户。新机应用账号密码可重新设置，但必须同步修改本地YAML。
6. 将备份`uploads`恢复到`backend/.local/uploads`，将原二进制主密钥恢复到`backend/.local/secrets/ai-master.key`；目标目录不存在时先新建。不要让应用先生成一份新主密钥再覆盖。
7. 恢复并核对私有YAML中的数据库凭据、JWT、上传路径和AI端点。**迁移时保留原AI主密钥，不运行第6章随机生成来替代它。** JWT可按迁移策略保留；换JWT会要求原会话重新登录。
8. 用新机运行账号核对私有文件读取权限。复制到新机后不应继续依赖旧机不存在的用户SID。主密钥仅授权实际运行账号读取，不向所有用户开放。
9. 先检查16表/8视图和29项一致性，再启动后端及入口，检查原账号、历史单据、图片、模型Key解密和真实生成。

恢复目标路径命令示例，先把`BackupDir`改为真实拷贝过来的备份目录；仅用于新目标，没有已有运行数据时执行：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
$BackupDir = 'C:\Backups\SupermarketInventory\实际备份时间目录'
$targetConfig = Join-Path $ProjectRoot 'backend\application-local.yml'
if (Test-Path -LiteralPath $targetConfig) { throw 'Target configuration already exists. Review it before restoring private configuration.' }
Copy-Item -LiteralPath (Join-Path $BackupDir 'application-local.yml') -Destination $targetConfig
$runAccount = [Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $targetConfig /inheritance:r /grant:r "${runAccount}:(F)" '*S-1-5-18:(F)'
if ($LASTEXITCODE -ne 0) { throw 'Private configuration ACL configuration failed.' }
$localDir = Join-Path $ProjectRoot 'backend\.local'
New-Item -ItemType Directory -Path (Join-Path $localDir 'secrets') -Force | Out-Null
if (Test-Path -LiteralPath (Join-Path $BackupDir 'uploads')) {
    if (Test-Path -LiteralPath (Join-Path $localDir 'uploads')) { throw 'Target uploads already exists. Review before replacing it.' }
    Copy-Item -LiteralPath (Join-Path $BackupDir 'uploads') -Destination (Join-Path $localDir 'uploads') -Recurse
}
if (Test-Path -LiteralPath (Join-Path $BackupDir 'ai-master.key')) {
    $targetMaster = Join-Path $localDir 'secrets\ai-master.key'
    if (Test-Path -LiteralPath $targetMaster) { throw 'Target master key already exists. Do not overwrite without confirming its source.' }
    Copy-Item -LiteralPath (Join-Path $BackupDir 'ai-master.key') -Destination $targetMaster
    if ([IO.File]::ReadAllBytes($targetMaster).Length -ne 32) { throw 'Unexpected master key length.' }
    $runAccount = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $targetMaster /inheritance:r /grant:r "${runAccount}:(F)"
    if ($LASTEXITCODE -ne 0) { throw 'Master key ACL configuration failed.' }
}
notepad.exe $targetConfig
```

编辑器中核对并更新新机数据库密码/路径，保存后再启动应用。若要在原电脑恢复到不同名字的验证库，必须额外审查dump中的视图、限定库名、DEFINER及检查脚本，或用独立MySQL实例复现同名库。仓库`03_verify.sql`含`USE supermarket_inventory;`，不能仅靠客户端`--database=测试库`就假定它检查测试库。不要向正式实例盲目恢复整份dump来尝试。

### 12.4 旧版本缺少AI用量字段

只用于已有旧库缺字段，当前唯一初始化SQL已经包含全部字段，新空库不用再执行升级。

先备份并使用有ALTER权限的管理账号，在专用终端：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath $ProjectRoot
$ToolsPython = Join-Path $ProjectRoot '.local\tools-venv\Scripts\python.exe'
$upgradePassword = Read-Host 'Enter the MySQL administrator password' -AsSecureString
$env:DB_USER = 'root'
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', $upgradePassword).Password
& $ToolsPython '.\scripts\upgrade-ai-usage.py' --database supermarket_inventory
if ($LASTEXITCODE -ne 0) { throw 'AI usage schema upgrade failed. Inspect before restarting the application.' }
```

脚本仅追加五个可空用量字段、约束和索引，不重置库存或历史，应用不会自动执行。数据库DDL可能独立提交；中断后根据脚本已存在项检查继续，不能假定全部回滚。完成后关闭专用终端。

### 12.5 程序更新和回滚

推荐维护顺序：记录版本 → 停止新操作/暂停入口 → 停后端及自启任务 → 完整备份 → 更新代码 → 构建 → 按版本说明处理数据库 → 替换运行JAR → 检查配置 → 启动后端 → 启动入口 → 第14章验收。

已停止固定JAR进程且备份完成后，更新命令核心为：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath (Join-Path $ProjectRoot 'backend')
.\mvnw.cmd '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw 'Backend update build failed.' }
Copy-Item -LiteralPath '.\target\inventory-1.0.0-SNAPSHOT.jar' -Destination '.\.local\runtime\inventory.jar' -Force
Set-Location -LiteralPath (Join-Path $ProjectRoot 'frontend')
npm.cmd ci
if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed.' }
npm.cmd run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend update build failed.' }
```

不要覆盖`backend/application-local.yml`、`.local/uploads`和`.local/secrets`，不要重导初始化SQL。回滚时停止新版本进程，恢复备份JAR、匹配的前端`dist`及部署配置；若数据库结构或业务数据已经改变，还须核对旧版本兼容性，不能只换旧JAR就承诺数据回滚成功。

## 13 常见问题与定位命令

### 13.1 快速对照

| 现象 | 优先检查 | 处理 |
| --- | --- | --- |
| `java`找不到或不是17 | `JAVA_HOME`、`Get-Command java.exe` | 按3.1设置JDK17，启动使用完整java路径 |
| `javac`不存在 | 是否只装JRE | 安装完整JDK17 |
| Maven无法下载 | DNS、443、代理、证书、时间 | 查看首个下载错误；保持HTTPS，不关闭证书校验绕过 |
| `npm.ps1 cannot be loaded` | PowerShell选择的命令 | 使用本文`npm.cmd` |
| `EBADENGINE`或Node API不兼容 | `node --version` | 换Node24，重新`npm.cmd ci`，不随意改锁文件 |
| `npm ci`失败 | 网络/registry或锁文件是否完整 | `npm.cmd config get registry`，检查交付文件；不要直接用强制安装掩盖版本冲突 |
| MySQL `Access denied` | root与应用密码、账号Host、真实实例 | 用同一个TCP地址测试`inventory_app`，核对`SHOW GRANTS` |
| `Public Key Retrieval is not allowed` | MySQL认证和TLS连接 | 核对服务器TLS及Connector配置；不要先把SSL全部关闭或对远程库盲目允许取公钥 |
| `Unknown database/table/column` | 是否选对库、完整初始化/增量升级 | 检查16表8视图及用量字段，不在已有业务库重导 |
| 不认识`utf8mb4_0900_ai_ci` | 是否误装MySQL5.7/MariaDB | 使用兼容MySQL8环境，不能仅删排序规则掩盖版本差异 |
| `JWT_SECRET`缺失或太短 | 工作目录、profile、本地YAML | 从backend启动，恢复私有配置；JWT至少32字节 |
| 启动`Port already in use` | 端口对应PID和命令行 | 识别后只停止本项目实例，或按13.3换端口 |
| 首页能开但登录失败/502 | 后端是否启动、代理目标 | 测8080及未认证`/auth/me`，看Nginx/Vite错误日志 |
| 详情页刷新404 | 静态托管的History回退 | 保留`try_files $uri $uri/ /index.html` |
| 图片413 | Nginx、Spring及项目图片上限 | Nginx3m、Spring单文件2MB/总请求3MB，业务图片仍最大2MiB |
| 图片404/默认头像 | 文件是否随数据库迁移 | 恢复真实uploads；演示种子图片缺失可用占位或重新上传 |
| AI Key存在但调用失败 | 管理Key优先级、端点/地域/模型权限 | 管理页查看来源；按安全错误处理，先核对云端配置 |
| AI提示无法解密 | 主密钥字节、路径、Windows账号ACL | 恢复原32字节文件，使用原运行身份；不要重新随机覆盖 |
| AI正文最后才一次出现 | 代理缓冲或额外网关 | 关闭SSE缓冲/压缩，检查`proxy_read_timeout`，使用正确POST流入口 |
| 销售超时或采购结果未知 | 原请求是否已提交 | 销售按原键恢复；采购/盘点先查列表核实，不换新请求重复写 |
| 商品不能销售/撤销/盘点 | 禁用、开关、过期、冻结、下游引用 | 按页面业务原因处理，不直接改数据库余额/冻结指针 |
| 登录后立即失效 | 密码/角色版本改变、时间、多个不同JWT实例 | 重新登录，核对时间与配置，不关服务端版本校验 |

### 13.2 识别进程再处理

只读取端口和进程身份：

```powershell
$listeners = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in 8080,5173,8088 })
$listeners | Select-Object LocalAddress,LocalPort,OwningProcess
foreach ($listener in $listeners) {
    Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)" |
        Select-Object ProcessId,Name,ExecutablePath,CommandLine
}
```

确认某个PID的命令行包含本项目固定运行JAR后，才对该PID执行`Stop-Process -Id 实际PID`；把实际PID替换为数字。不能使用“结束所有java/node/nginx进程”的命令。

常用日志：

```powershell
Get-Content -LiteralPath 'C:\Apps\SupermarketInventory\backend\.local\logs\backend.log' -Tail 100
Get-Content -LiteralPath 'C:\Apps\nginx\logs\error.log' -Tail 100
```

前台开发启动还应看原终端输出。日志、配置截图和数据库dump可能含私有内容，不作为公开问题附件直接上传。

### 13.3 更改后端端口

若8080确有其他服务占用，将本地YAML的`server.port`改为8082，重启本项目后端。

开发/预览方式在其启动终端同步：

```powershell
$env:BACKEND_PROXY_TARGET = 'http://127.0.0.1:8082'
Set-Location -LiteralPath 'C:\Apps\SupermarketInventory\frontend'
npm.cmd run dev
```

Nginx方式把三个`proxy_pass http://127.0.0.1:8080;`全部改成8082，`-t`通过后重载；同时修改计划任务启动脚本里的监听检查端口。只改前端代理不会让后端真的换端口。

### 13.4 数据库通信故障的分层排查

先核对服务、端口、凭据，再区分“新建TCP连接失败”“数据库握手/SQL失败”“池内旧连接失效”。不要把所有`Communications link failure`都归因于业务SQL。

装好11.1工具后，在专用诊断终端设置**应用运行账号**，只执行只读查询：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath $ProjectRoot
$ToolsPython = Join-Path $ProjectRoot '.local\tools-venv\Scripts\python.exe'
$probePassword = Read-Host 'Enter the inventory_app database password' -AsSecureString
$env:DB_USER = 'inventory_app'
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', $probePassword).Password
& $ToolsPython '.\scripts\diagnose-connectivity.py' --database supermarket_inventory --backend-url http://127.0.0.1:8080 --samples 120 --interval 0.5 --echo
```

报告写入`.local/diagnostics/`，包含数据库、未认证HTTP及独立TCP回显的时间、阶段和耗时，不执行业务写入，也不自动修改连接池或驱动。

本项目原电脑曾发现旧Npcap 0.9982运行时跨端口回环握手丢失，停止驱动后对照和完整回归通过。**这是原电脑的定位结果，不意味着每台新电脑都要停止Npcap或安装抓包软件。** 只有新机也存在旧Npcap、独立回显与MySQL同时间失败等对应证据时，才安排系统驱动层对照。

只读查看：

```powershell
sc.exe query npcap
Get-Service -Name MySQL80
Test-NetConnection 127.0.0.1 -Port 3306
```

系统服务1060表示没有该服务，不是本项目故障。确需临时暂停抓包驱动做对照时，由部署者在管理员终端执行`sc.exe stop npcap`；结束后按需要`sc.exe start npcap`。它影响抓包能力，不改数据库；原机已选择保持停止，新机不沿用该状态假设。

当前Hikari/JDBC已配置有界超时、生命周期与失效连接回收。加大超时、切IPv6、反复重发收款或重复导库不能作为传输丢包的修复证明。

## 14 部署验收清单

逐项实际检查并记录结果，新电脑的成功证据以本次操作为准，不照抄原电脑的验收结论。

| 检查项 | 通过标准 |
| --- | --- |
| 版本 | JDK17、Node24、预期MySQL版本，Wrapper3.9.9 |
| 目标数据库 | 连接`supermarket_inventory`，不是`mysql`系统库或误用测试副本 |
| 结构 | 16表、8视图、5个用量字段；无初始化SQL错误 |
| 一致性 | 当前目标库29项`violations=0`；核对检查脚本的`USE`目标 |
| 后端 | 正确工作目录启动，8080或约定端口监听，未认证`/auth/me`为401 |
| 页面 | 5173快速运行或8088打包入口可登录，内部路由刷新正常 |
| 三岗位 | 菜单与权限符合岗位，采购本人范围和收银本人记录正常 |
| 基础档案 | 新建商品库存为0，分类/供应商可查询，图片上传替换正常 |
| 采购 | 新专用商品草稿/提交不入库，管理员审核后库存及批次增加 |
| 销售 | 收银报价/收款成功，库存扣减，金额/找零/销售记录一致 |
| 盘点 | 新专用商品冻结、实盘、提交和审核后正确调账并解冻 |
| 统计 | 销售和库存图表/明细有合理数据，能按相同条件下钻 |
| 真实AI | 自有Key生成成功，正文流式显示，历史标识真实，已报告Token可核对 |
| AI失败隔离 | 未配置或云端失败时核心采购销售仍可用 |
| 打包入口 | 无需Vite，Nginx转发API/媒体及SSE正常 |
| 自动启动 | 若启用计划任务，重新登录后入口可用，后端能读原主密钥 |
| 备份恢复 | 数据、图片、配置、主密钥及程序版本已配套保存 |

使用专用新商品验证闭环，避免修改原演示盘点或为通过检查删除历史。真实模型验证会产生该云账户的实际调用消耗，由使用者在自己环境确认。

## 15 日常启动速查

### 15.1 本机开发/答辩，两窗口

终端A：

```powershell
$ProjectRoot = 'C:\Apps\SupermarketInventory'
Set-Location -LiteralPath $ProjectRoot
powershell.exe -NoProfile -ExecutionPolicy Bypass -File '.\scripts\start-backend.ps1' -JavaHome $env:JAVA_HOME
```

终端B：

```powershell
Set-Location -LiteralPath 'C:\Apps\SupermarketInventory\frontend'
$env:BACKEND_PROXY_TARGET = 'http://127.0.0.1:8080'
npm.cmd run dev
```

访问`http://127.0.0.1:5173`。首次安装依赖后，无需每次`npm ci`；没有更改依赖时也不需要反复安装JDK/MySQL。

### 15.2 已配置登录自启的打包方案

```powershell
Start-ScheduledTask -TaskName 'SupermarketInventory-Backend'
Start-ScheduledTask -TaskName 'SupermarketInventory-Web'
```

仅在对应实例尚未运行时执行，访问`http://127.0.0.1:8088`。已有实例正常运行就直接访问，不重复启动。

### 15.3 三条最重要的维护规则

1. 日常重启只启动服务，**不导入初始化SQL、不重置数据库、不重新随机生成原AI主密钥**。
2. 后端工作目录固定为`backend`，账号、图片和主密钥路径保持一致。
3. 程序、数据库、上传文件及密钥配套备份；升级先隔离验证，再按维护窗口执行。

## 16 依据文件

- [项目README](README.md)：现有启动方式、依赖版本、验收和模型配置约定。
- [系统详细说明书](系统详细说明书.md)：业务、架构、文件、数据库和接口全量说明。
- [后端配置](backend/src/main/resources/application.yml)、[本地配置导入](backend/src/main/resources/application-local.yml)、[本地配置示例](backend/application-local.example.yml)。
- [后端构建](backend/pom.xml)、[前端依赖和命令](frontend/package.json)、[Vite代理](frontend/vite.config.ts)。
- [唯一初始化SQL](doc/数据库/supermarket_inventory.sql)、[一致性检查](doc/数据库/03_verify.sql)。
- [后端启动脚本](scripts/start-backend.ps1)、[测试库准备](scripts/prepare-test-db.py)、[用量字段升级](scripts/upgrade-ai-usage.py)、[通信诊断](scripts/diagnose-connectivity.py)。
- [正式库验收记录](doc/正式库用户视角验收记录.md)：原电脑验证证据和历史失败，不等于新电脑已自动验收。

本文中的路径是统一示例，软件安装器界面和具体补丁号可能变化；以项目要求的大版本、官方安装说明、交付代码和新机实际检查为准。
