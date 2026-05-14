const ADMIN_TABS = [
  ["dashboard", "통계"],
  ["users", "회원"],
  ["restrictions", "기능제재"],
  ["categories", "카테고리"],
  ["operations", "상품/경매"],
  ["withdrawals", "출금"],
  ["audit", "감사 로그"],
];

export default function AdminTabs({ activeTab, onChange }) {
  return (
    <div className="pill-tabs">
      {ADMIN_TABS.map(([key, label]) => (
        <button
          key={key}
          type="button"
          className={`pill-tab${activeTab === key ? " active" : ""}`}
          onClick={() => onChange(key)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
