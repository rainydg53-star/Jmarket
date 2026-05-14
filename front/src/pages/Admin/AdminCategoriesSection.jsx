
export default function AdminCategoriesSection({ categories, categoryForm, setCategoryForm, loading, createCategory, updateCategory, deleteCategory }) {
  return (
        <section className="card">
          <h2>카테고리 관리</h2>
          <form className="admin-category-form" onSubmit={createCategory}>
            <input placeholder="코드 예: DIGITAL_APPLIANCE" value={categoryForm.code} onChange={(e) => setCategoryForm((prev) => ({ ...prev, code: e.target.value }))} />
            <input placeholder="이름 예: 디지털/가전" value={categoryForm.name} onChange={(e) => setCategoryForm((prev) => ({ ...prev, name: e.target.value }))} />
            <input type="number" value={categoryForm.displayOrder} onChange={(e) => setCategoryForm((prev) => ({ ...prev, displayOrder: Number(e.target.value) }))} />
            <label className="inline-check">
              <input type="checkbox" checked={categoryForm.active} onChange={(e) => setCategoryForm((prev) => ({ ...prev, active: e.target.checked }))} />
              활성
            </label>
            <button type="submit" disabled={loading}>추가</button>
          </form>
          <ul className="list admin-list">
            {categories.map((category) => (
              <li className="list-item" key={category.id}>
                <strong>{category.name}</strong>
                <span className="meta">{category.code} · 순서 {category.displayOrder} · {category.active ? "활성" : "비활성"}</span>
                <div className="actions compact-actions">
                  <button type="button" onClick={() => {
                    const name = window.prompt("카테고리 이름", category.name);
                    if (name) updateCategory(category, { name });
                  }} disabled={loading}>이름 수정</button>
                  <button type="button" onClick={() => updateCategory(category, { active: !category.active })} disabled={loading}>
                    {category.active ? "비활성" : "활성"}
                  </button>
                  <button type="button" className="danger-button" onClick={() => deleteCategory(category)} disabled={loading}>삭제</button>
                </div>
              </li>
            ))}
          </ul>
        </section>
  );
}
