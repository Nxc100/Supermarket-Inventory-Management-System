import {
  test,
  expect,
  type Page,
} from "../../frontend/test-support/playwright";
import { login, confirm, stockedProduct } from "./support/fixtures";
async function loginPassword(page: Page, name: string, password: string) {
  await page.goto("/login");
  await page.getByRole("textbox", { name: "*用户名", exact: true }).fill(name);
  await page
    .getByRole("textbox", { name: "*密码", exact: true })
    .fill(password);
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await expect(page.getByRole("heading", { name: /欢迎回来/ })).toBeVisible();
}
async function create(page: Page, role = "前台收银操作员") {
  const username = "UI_U_" + Date.now();
  await page.getByRole("menuitem", { name: "用户管理", exact: true }).click();
  await page.getByRole("button", { name: "新增用户", exact: true }).click();
  const editor = page.getByRole("dialog", { name: "新增用户", exact: true });
  await editor
    .getByRole("textbox", { name: "账号用户名", exact: true })
    .fill(username);
  await editor
    .getByRole("textbox", { name: "账号姓名", exact: true })
    .fill("页面账号验证");
  await editor.locator(".el-select").click();
  await page.getByRole("option", { name: role, exact: true }).click();
  await editor
    .getByRole("textbox", { name: "账号密码", exact: true })
    .fill("Test123456");
  await editor
    .getByRole("textbox", { name: "确认账号密码", exact: true })
    .fill("Test123456");
  await editor.getByRole("button", { name: "保存用户", exact: true }).click();
  await expect(editor).toBeHidden();
  await filter(page, username);
  return username;
}
async function filter(page: Page, username: string) {
  await page
    .getByRole("textbox", { name: "用户关键词", exact: true })
    .fill(username);
  await page.getByRole("button", { name: "查询用户", exact: true }).click();
  await expect(
    page.getByRole("row").filter({ hasText: username }).first(),
  ).toBeVisible();
}
test("account creation, password reset, role change, disable and delete revoke old sessions through the UI", async ({
  page,
}) => {
  test.setTimeout(120000);
  await login(page, "admin");
  const username = await create(page);
  const row = page.getByRole("row").filter({ hasText: username });
  const userPage = await page.context().newPage();
  try {
    await login(userPage, username);
    await expect(
      userPage.getByRole("menuitem", { name: "用户管理", exact: true }),
    ).toHaveCount(0);
    await row.getByRole("button", { name: "重置密码", exact: true }).click();
    const reset = page.getByRole("dialog", {
      name: "重置用户密码",
      exact: true,
    });
    await reset
      .getByRole("textbox", { name: "账号密码", exact: true })
      .fill("Updated123456");
    await reset
      .getByRole("textbox", { name: "确认账号密码", exact: true })
      .fill("Updated123456");
    await reset.getByRole("button", { name: "重置密码", exact: true }).click();
    await confirm(page, "确认重置密码");
    await expect(reset).toBeHidden();
    await userPage.goto("/");
    await expect(userPage).toHaveURL(/\/login/);
    await loginPassword(userPage, username, "Updated123456");
    await page.getByRole("button", { name: "刷新用户", exact: true }).click();
    await expect(row).toBeVisible();
    await row.getByRole("button", { name: "停用用户", exact: true }).click();
    await confirm(page, "停用用户");
    await expect(row).toContainText("已停用");
    await userPage.goto("/");
    await expect(userPage).toHaveURL(/\/login/);
    await row.getByRole("button", { name: "启用用户", exact: true }).click();
    await confirm(page, "启用用户");
    await expect(row).toContainText("已启用");
    await loginPassword(userPage, username, "Updated123456");
    await row.getByRole("button", { name: "编辑用户", exact: true }).click();
    const edit = page.getByRole("dialog", { name: "编辑用户", exact: true });
    await expect(
      edit.getByRole("textbox", { name: "账号用户名", exact: true }),
    ).toBeDisabled();
    await edit
      .getByRole("textbox", { name: "账号姓名", exact: true })
      .fill("页面采购验证");
    await edit.locator(".el-select").click();
    await page
      .getByRole("option", { name: "采购入库操作员", exact: true })
      .click();
    await edit.getByRole("button", { name: "保存用户", exact: true }).click();
    await expect(edit).toBeHidden();
    await expect(row).toContainText("采购入库操作员");
    await userPage.goto("/");
    await expect(userPage).toHaveURL(/\/login/);
    await loginPassword(userPage, username, "Updated123456");
    await expect(
      userPage.getByRole("menuitem", { name: "供应商管理", exact: true }),
    ).toBeVisible();
    await expect(
      userPage.getByRole("menuitem", { name: "前台开单", exact: true }),
    ).toHaveCount(0);
    await page.getByRole("button", { name: "刷新用户", exact: true }).click();
    await expect(row).toBeVisible();
    await row.getByRole("button", { name: "删除账号", exact: true }).click();
    await confirm(page, "删除账号");
    await expect(
      page.getByText("暂无符合条件的用户", { exact: true }),
    ).toBeVisible();
    await userPage.goto("/");
    await expect(userPage).toHaveURL(/\/login/);
  } finally {
    await userPage.close();
  }
});

test("duplicate account input is preserved and current or active-task accounts are protected", async ({
  page,
}) => {
  await login(page, "admin");
  await page.getByRole("menuitem", { name: "用户管理", exact: true }).click();
  await filter(page, "admin");
  const self = page.getByRole("row").filter({ hasText: "当前账号" });
  await expect(
    self.getByRole("button", { name: "停用用户", exact: true }),
  ).toBeDisabled();
  await expect(
    self.getByRole("button", { name: "删除账号", exact: true }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "新增用户", exact: true }).click();
  const editor = page.getByRole("dialog", { name: "新增用户", exact: true });
  await editor
    .getByRole("textbox", { name: "账号用户名", exact: true })
    .fill("admin");
  await editor
    .getByRole("textbox", { name: "账号姓名", exact: true })
    .fill("重复账号验证");
  await editor
    .getByRole("textbox", { name: "账号密码", exact: true })
    .fill("Test123456");
  await editor
    .getByRole("textbox", { name: "确认账号密码", exact: true })
    .fill("Test123456");
  await editor.getByRole("button", { name: "保存用户", exact: true }).click();
  await expect(editor.getByRole("alert")).toContainText("用户名已存在");
  await expect(
    editor.getByRole("textbox", { name: "账号姓名", exact: true }),
  ).toHaveValue("重复账号验证");
  await editor.getByRole("button", { name: "取消", exact: true }).click();
  await page
    .getByRole("dialog", { name: "未保存的修改", exact: true })
    .getByRole("button", { name: "放弃修改", exact: true })
    .click();
  await filter(page, "buyer01");
  const buyer = page.getByRole("row").filter({ hasText: "buyer01" });
  await expect(
    buyer.getByRole("button", { name: "停用用户", exact: true }),
  ).toBeDisabled();
  await buyer.getByRole("button", { name: "编辑用户", exact: true }).click();
  const edit = page.getByRole("dialog", { name: "编辑用户", exact: true });
  await expect(
    edit.getByRole("combobox", { name: "账号岗位", exact: true }),
  ).toBeDisabled();
  await edit.getByRole("button", { name: "取消", exact: true }).click();
  await buyer
    .getByRole("button", { name: "处理未完成任务", exact: true })
    .click();
  await expect(
    page
      .getByRole("dialog", { name: "buyer01 · 未完成任务", exact: true })
      .getByRole("button", { name: "打开任务", exact: true })
      .first(),
  ).toBeVisible();
});

test("administrator follows an account task to close its stocktake before disabling the owner", async ({
  page,
}) => {
  test.setTimeout(150000);
  const product = await stockedProduct(page);
  await login(page, "admin");
  const username = await create(page, "采购入库操作员");
  const owner = await page.context().newPage();
  try {
    await login(owner, username);
    await owner
      .getByRole("menuitem", { name: "库存盘点", exact: true })
      .click();
    await owner.getByRole("button", { name: "新建盘点", exact: true }).click();
    await owner
      .getByRole("textbox", { name: "盘点选品关键词", exact: true })
      .fill(product.code);
    await owner
      .getByRole("button", { name: "搜索盘点商品", exact: true })
      .click();
    await owner
      .getByRole("row")
      .filter({ hasText: product.code })
      .getByRole("button", { name: "选取商品", exact: true })
      .click();
    await owner.getByRole("button", { name: "开始盘点", exact: true }).click();
    await confirm(owner, "开始盘点");
    await expect(
      owner.getByRole("heading", { name: "盘点详情", exact: true }),
    ).toBeVisible();
    await page.getByRole("button", { name: "刷新用户", exact: true }).click();
    const row = page.getByRole("row").filter({ hasText: username });
    await expect(row).toContainText("盘点 1");
    await expect(
      row.getByRole("button", { name: "停用用户", exact: true }),
    ).toBeDisabled();
    await row
      .getByRole("button", { name: "处理未完成任务", exact: true })
      .click();
    await page
      .getByRole("dialog", { name: `${username} · 未完成任务`, exact: true })
      .getByRole("button", { name: "打开任务", exact: true })
      .click();
    await page.getByRole("button", { name: "关闭盘点", exact: true }).click();
    const close = page.getByRole("dialog", { name: "关闭盘点", exact: true });
    await close
      .getByRole("textbox")
      .fill("账号测试：管理员接续关闭盘点，保留原库存");
    await close.getByRole("button", { name: "关闭盘点", exact: true }).click();
    await expect(close).toBeHidden();
    await expect(
      page.getByText("已关闭", { exact: true }).first(),
    ).toBeVisible();
    await page.getByRole("menuitem", { name: "用户管理", exact: true }).click();
    await filter(page, username);
    await expect(row).toContainText("盘点 0");
    await row.getByRole("button", { name: "停用用户", exact: true }).click();
    await confirm(page, "停用用户");
    await expect(row).toContainText("已停用");
    await expect(
      row.getByRole("button", { name: "删除账号", exact: true }),
    ).toBeDisabled();
    await owner.reload();
    await expect(owner).toHaveURL(/\/login/);
  } finally {
    await owner.close();
  }
});
