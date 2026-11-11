import {
  test,
  expect,
  type Page,
} from "../../frontend/test-support/playwright";
import { login, confirm } from "./support/fixtures";
async function configure(page: Page, name: string) {
  await page.getByRole("menuitem", { name: "角色权限", exact: true }).click();
  await page
    .getByRole("row")
    .filter({ hasText: name })
    .getByRole("button", { name: "配置权限", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: `${name} · 权限树`, exact: true }),
  ).toBeVisible();
}
function checkbox(page: Page, code: string) {
  return page
    .locator(".el-tree-node__content")
    .filter({ has: page.locator(`[data-permission="${code}"]`) })
    .getByRole("checkbox");
}
async function toggle(page: Page, code: string) {
  await page
    .locator(".el-tree-node__content")
    .filter({ has: page.locator(`[data-permission="${code}"]`) })
    .locator(".el-checkbox")
    .click();
}
async function restore(page: Page) {
  await page.getByRole("button", { name: "恢复默认权限", exact: true }).click();
  await confirm(page, "恢复默认权限");
}
test("cashier permission changes invalidate sessions, hide the menu, reject direct routes and restore defaults", async ({
  page,
}) => {
  test.setTimeout(120000);
  await login(page, "admin");
  await configure(page, "前台收银操作员");
  const cashier = await page.context().newPage();
  try {
    await login(cashier, "cashier02");
    await expect(checkbox(page, "statistics.sales")).toBeChecked();
    await toggle(page, "statistics.sales");
    await page
      .getByRole("button", { name: "保存岗位权限", exact: true })
      .click();
    await confirm(page, "保存岗位权限");
    await expect(
      page.getByRole("button", { name: "保存岗位权限", exact: true }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "保存岗位权限", exact: true }),
    ).not.toHaveClass(/is-loading/);
    await cashier.goto("/");
    await expect(cashier).toHaveURL(/\/login/);
    await login(cashier, "cashier02");
    await expect(
      cashier.getByRole("menuitem", { name: "销售统计", exact: true }),
    ).toHaveCount(0);
    await cashier.goto("/statistics/sales");
    await expect(
      cashier.locator("main").getByText("访问受限", { exact: true }),
    ).toBeVisible();
    await restore(page);
    await expect(checkbox(page, "statistics.sales")).toBeChecked();
    await cashier.goto("/");
    await expect(cashier).toHaveURL(/\/login/);
    await login(cashier, "cashier02");
    await expect(
      cashier.getByRole("menuitem", { name: "销售统计", exact: true }),
    ).toBeVisible();
    await page.getByRole("menuitem", { name: "操作记录", exact: true }).click();
    await page
      .getByRole("textbox", { name: "操作对象关键词", exact: true })
      .fill("CASHIER");
    await page
      .getByRole("button", { name: "查询操作记录", exact: true })
      .click();
    await expect(
      page.getByRole("row").filter({ hasText: "ROLE_RESTORE" }).first(),
    ).toBeVisible();
    await expect(
      page.getByRole("row").filter({ hasText: "ROLE_SAVE" }).first(),
    ).toBeVisible();
  } finally {
    await configure(page, "前台收银操作员");
    await restore(page);
    await expect(checkbox(page, "statistics.sales")).toBeChecked();
    await cashier.close();
  }
});
test("permission hierarchy and critical management nodes stay protected and an administrator reauthenticates after changing their own role", async ({
  page,
}) => {
  test.setTimeout(120000);
  await login(page, "admin");
  await configure(page, "超市管理员");
  try {
    await expect(checkbox(page, "system.role.save")).toBeDisabled();
    await expect(checkbox(page, "system.user")).toBeDisabled();
    await toggle(page, "sale.order");
    await expect(checkbox(page, "sale.order.complete")).not.toBeChecked();
    await toggle(page, "sale.order.complete");
    await expect(checkbox(page, "sale.order")).toBeChecked();
    await page.getByRole("menuitem", { name: "商品档案", exact: true }).click();
    await page
      .getByRole("dialog", { name: "离开页面", exact: true })
      .getByRole("button", { name: "继续编辑", exact: true })
      .click();
    await expect(checkbox(page, "sale.order.correct")).not.toBeChecked();
    await page.getByRole("button", { name: "刷新权限树", exact: true }).click();
    await page
      .getByRole("dialog", { name: "离开页面", exact: true })
      .getByRole("button", { name: "确认离开", exact: true })
      .click();
    await expect(checkbox(page, "sale.order.correct")).toBeChecked();
    await toggle(page, "base.product.delete");
    await page
      .getByRole("button", { name: "保存岗位权限", exact: true })
      .click();
    await confirm(page, "保存岗位权限");
    await expect(page).toHaveURL(/\/login/);
    await login(page, "admin");
    await page.getByRole("menuitem", { name: "商品档案", exact: true }).click();
    await expect(
      page.getByRole("button", { name: "删除", exact: true }),
    ).toHaveCount(0);
    await configure(page, "超市管理员");
    await restore(page);
    await expect(page).toHaveURL(/\/login/);
    await login(page, "admin");
    await page.getByRole("menuitem", { name: "商品档案", exact: true }).click();
    await expect(
      page.getByRole("button", { name: "删除", exact: true }).first(),
    ).toBeVisible();
  } finally {
    if (page.url().includes("/login")) await login(page, "admin");
    await configure(page, "超市管理员");
    await restore(page);
    await expect(page).toHaveURL(/\/login/);
    await login(page, "admin");
  }
});
