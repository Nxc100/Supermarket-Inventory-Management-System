# Windows 数据库与图片迁移

迁移时需要同时复制：**当前数据库备份、整个图片上传目录、最新项目代码**。

## 1. 本机导出

暂时停止后端，导出和复制图片期间暂停业务操作。

在 PowerShell 中执行，按提示输入本机 MySQL 密码：

```powershell
mysqldump -u root -p --routines --triggers --events --single-transaction --default-character-set=utf8mb4 --set-gtid-purged=OFF --result-file=supermarket_inventory_full.sql supermarket_inventory
```

确认没有报错，执行 `$LASTEXITCODE` 应返回 `0`。SQL 文件保存在当前目录。使用 `--result-file` 避免 Windows PowerShell 重定向的编码问题。

将以下内容复制到新电脑：

- 最新项目代码。
- `supermarket_inventory_full.sql`。
- 整个 `backend\.local\uploads\` 目录，保持文件名不变；该目录被 Git 忽略，需要单独复制。

## 2. 新电脑导入

建议使用与本机相同的 MySQL 8.0.34。以下步骤适用于新空数据库；目标库已有数据时先备份，导入可能覆盖同名表。

假设 SQL 文件放在 `D:\transfer\`，先进入 MySQL：

```powershell
mysql -u root -p --default-character-set=utf8mb4
```

在 MySQL 提示符中逐条执行：

```sql
CREATE DATABASE supermarket_inventory
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE supermarket_inventory;
SOURCE D:/transfer/supermarket_inventory_full.sql;
```

确认导入过程无报错。

## 3. 放置图片并启动

1. 将图片目录复制到新项目的 `backend\.local\uploads\`，避免多套一层 `uploads`。
2. 修改新机数据库连接配置，后端工作目录设为 `backend`。如果配置了 `UPLOAD_DIR`，图片必须放在该配置指向的目录。
3. 启动前后端，登录检查商品图片、头像和业务数据。

完成整库和图片迁移后，**不需要运行图片初始化脚本，也不要再导入仓库中的初始化 SQL**。

如果还要保留管理页面保存的模型 API Key，需要一并迁移实际使用的加密主密钥，默认文件为 `backend\.local\secrets\ai-master.key`；也可以在新机重新填写模型 API Key。本地数据库配置和密钥文件通常也被 Git 忽略。

参考：[MySQL 官方 mysqldump 说明](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html)。
