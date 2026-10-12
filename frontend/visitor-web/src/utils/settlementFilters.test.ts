import { describe, expect, it } from "vitest";
import { matchesSettlementFilters, type SettlementFilter } from "./settlementFilters";

describe("settlement ledger filters", () => {
  const record = { 姓名: "李慧怡", 中心: "法务中心", 部门: "法务部", 岗位: "法务专员", 性别: "女", 结算月份: "2026-09", 实际入住时间: "2026-09-03T14:00:00", 备注: "", 居住天数: 30 };
  const condition = (field: string, operator: SettlementFilter["operator"], value = ""): SettlementFilter => ({ field, operator, value });
  it("keeps blank optional conditions unrestricted", () => {
    expect(matchesSettlementFilters(record, [condition("", "contains"), condition("姓名", "contains")])).toBe(true);
  });
  it("combines all selected ledger and date conditions with AND", () => {
    expect(matchesSettlementFilters(record, [condition("中心", "contains", "法务"), condition("性别", "equals", "女"), condition("结算月份", "gte", "2026-08"), condition("结算月份", "lte", "2026-09")])).toBe(true);
    expect(matchesSettlementFilters(record, [condition("中心", "contains", "法务"), condition("性别", "equals", "男")])).toBe(false);
  });
  it("supports explicit empty fields without treating missing dates as matching ranges", () => {
    expect(matchesSettlementFilters(record, [condition("备注", "empty")])).toBe(true);
    expect(matchesSettlementFilters(record, [condition("备注", "notEmpty")])).toBe(false);
    expect(matchesSettlementFilters(record, [condition("计划退宿", "gte", "2026-09-01")])).toBe(false);
  });
  it("includes the end day and compares numeric fields numerically", () => {
    expect(matchesSettlementFilters(record, [condition("实际入住时间", "lte", "2026-09-03"), condition("居住天数", "gte", "9")])).toBe(true);
    expect(matchesSettlementFilters(record, [condition("实际入住时间", "lte", "2026-09-02")])).toBe(false);
  });
});
