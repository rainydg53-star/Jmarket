import { AuditActionRatioChart, TrendChart } from "./AdminCharts";
import { formatAuditAction, formatAuditTarget, formatDateTime } from "./adminUtils";

export default function AdminAuditSection({ dashboard, auditLogs }) {
  const auditDailyMetrics = dashboard?.auditDailyMetrics ?? [];
  const auditActionMetrics = dashboard?.auditActionMetrics ?? [];

  return (
    <>
      <section className="dashboard-trends audit-visuals">
        <TrendChart title="관리자 조치 수" unit="건" data={auditDailyMetrics} field="actionCount" tone="blue" />
        <AuditActionRatioChart data={auditActionMetrics} />
      </section>
      <section className="card">
        <h2>감사 로그</h2>
        <ul className="list admin-list">
          {auditLogs.map((log) => (
            <li className="list-item" key={log.id}>
              <strong>{formatAuditAction(log.action)}</strong>
              <span className="meta">관리자: {log.adminNickname} #{log.adminUserId}</span>
              <span className="meta">대상: {formatAuditTarget(log.targetType)} {log.targetId ?? ""} · {log.memo ?? "-"}</span>
              <span className="meta">{formatDateTime(log.createdAt)}</span>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
