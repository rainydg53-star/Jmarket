import { api } from "./api";

export async function loadCategoryOptions({ includeAll = false } = {}) {
  const categories = await api("/api/categories");
  const options = categories.map((category) => ({
    value: category.code,
    label: category.name,
  }));
  return includeAll ? [{ value: "", label: "전체 카테고리" }, ...options] : options;
}
