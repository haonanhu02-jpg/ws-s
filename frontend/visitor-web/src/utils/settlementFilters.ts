export type SettlementFilter = { field: string; operator: "contains" | "equals" | "gte" | "lte" | "empty" | "notEmpty"; value: string };

export function matchesSettlementFilters(record: Record<string, unknown>, filters: SettlementFilter[]): boolean {
  return filters.every(filter => {
    if (!filter.field) return true;
    const raw = record[filter.field];
    const text = String(raw ?? "").trim();
    const value = filter.value.trim();
    if (filter.operator === "empty") return text === "";
    if (filter.operator === "notEmpty") return text !== "";
    if (!value) return true;
    if (filter.operator === "contains") return text.toLocaleLowerCase().includes(value.toLocaleLowerCase());
    if (filter.operator === "equals") return text.toLocaleLowerCase() === value.toLocaleLowerCase();
    if (!text) return false;
    if (typeof raw === "number") return Number.isFinite(Number(value)) && (filter.operator === "gte" ? raw >= Number(value) : raw <= Number(value));
    // A date input represents the whole selected day, including timestamped stays.
    const compared = /^\d{4}-\d{2}-\d{2}$/.test(value) ? text.slice(0, 10) : text;
    return filter.operator === "gte" ? compared >= value : compared <= value;
  });
}
