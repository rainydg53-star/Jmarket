import { formatAuditAction, formatNumber, formatShortDate } from "./adminUtils";

export function TrendChart({ title, unit, data, field, tone }) {
  const maxValue = Math.max(1, ...data.map((item) => Number(item[field] ?? 0)));

  return (
    <div className="card trend-card">
      <div className="trend-card-head">
        <h3>{title}</h3>
        <span className="meta">최근 7일</span>
      </div>
      <div className="trend-bars">
        {data.map((item) => {
          const value = Number(item[field] ?? 0);
          const height = Math.max(8, Math.round((value / maxValue) * 120));
          return (
            <div className="trend-bar-item" key={`${field}-${item.date}`}>
              <strong>{formatNumber(value)}</strong>
              <div className="trend-bar-track">
                <span
                  className={`trend-bar-fill ${tone}`}
                  style={{ height: `${height}px` }}
                  title={`${formatShortDate(item.date)} ${formatNumber(value)}${unit}`}
                />
              </div>
              <span className="meta">{formatShortDate(item.date)}</span>
            </div>
          );
        })}
      </div>
      <p className="meta trend-unit">단위: {unit}</p>
    </div>
  );
}

export function AuditActionRatioChart({ data }) {
  const total = data.reduce((sum, item) => sum + Number(item.count ?? 0), 0);

  return (
    <div className="card audit-ratio-card">
      <div className="trend-card-head">
        <h3>조치 유형별 비율</h3>
        <span className="meta">최근 7일</span>
      </div>
      {data.length === 0 ? (
        <p className="meta">최근 조치 로그가 없습니다.</p>
      ) : (
        <div className="audit-ratio-list">
          {data.map((item) => {
            const count = Number(item.count ?? 0);
            const percent = total > 0 ? Math.round((count / total) * 100) : 0;
            return (
              <div className="audit-ratio-row" key={item.action}>
                <div className="audit-ratio-label">
                  <strong>{formatAuditAction(item.action)}</strong>
                  <span className="meta">
                    {formatNumber(count)}건 · {percent}%
                  </span>
                </div>
                <div className="audit-ratio-track">
                  <span className="audit-ratio-fill" style={{ width: `${Math.max(4, percent)}%` }} />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
