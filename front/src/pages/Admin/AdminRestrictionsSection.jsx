import { formatDateTime } from "./adminUtils";

export default function AdminRestrictionsSection({ restrictions, loading, deactivateRestriction }) {
  return (
        <section className="card">
          <h2>기능별 제재 현황</h2>
          {restrictions.length === 0 ? (
            <p className="meta">등록된 기능 제재가 없습니다.</p>
          ) : (
            <ul className="list admin-list">
              {restrictions.map((restriction) => (
                <li className={`list-item${restriction.active ? "" : " sold"}`} key={restriction.id}>
                  <strong>{restriction.userNickname} #{restriction.userId}</strong>
                  <span className="meta">제재: {restriction.typeLabel} · 상태: {restriction.active ? "적용중" : "만료/해제"}</span>
                  <span className="meta">사유: {restriction.reason ?? "-"}</span>
                  <span className="meta">종료: {formatDateTime(restriction.restrictedUntil)} · 등록: {formatDateTime(restriction.createdAt)}</span>
                  {restriction.active ? (
                    <div className="actions compact-actions">
                      <button type="button" onClick={() => deactivateRestriction(restriction)} disabled={loading}>제재 해제</button>
                    </div>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </section>
  );
}
