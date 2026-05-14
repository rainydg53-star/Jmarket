import { useMemo } from "react";
import { TrendChart } from "./AdminCharts";
import { formatNumber } from "./adminUtils";

export default function AdminDashboardSection({ dashboard }) {
  const dashboardCards = useMemo(() => ([
    ["전체 회원", dashboard?.totalUsers],
    ["오늘 가입", dashboard?.todayJoinedUsers],
    ["일반 상품", dashboard?.directProducts],
    ["경매 상품", dashboard?.auctionProducts],
    ["진행 경매", dashboard?.openAuctions],
    ["거래 완료", dashboard?.completedTrades],
    ["마일리지 충전", dashboard?.totalMileageCharged],
    ["마일리지 사용", dashboard?.totalMileageUsed],
    ["승인 결제액", dashboard?.approvedPaymentAmount],
  ]), [dashboard]);
  const dailyMetrics = dashboard?.dailyMetrics ?? [];

  return (
    <>
      <section className="admin-grid">
        {dashboardCards.map(([label, value]) => (
          <div className="card stat-card" key={label}>
            <span className="meta">{label}</span>
            <strong>{formatNumber(value)}</strong>
          </div>
        ))}
      </section>
      {dailyMetrics.length > 0 ? (
        <section className="dashboard-trends">
          <TrendChart title="일일 활성 회원" unit="명" data={dailyMetrics} field="activeUsers" tone="blue" />
          <TrendChart title="거래 완료수" unit="건" data={dailyMetrics} field="completedTrades" tone="green" />
          <TrendChart title="신고 처리수" unit="건" data={dailyMetrics} field="processedReports" tone="red" />
          <TrendChart title="마일리지 충전량" unit="P" data={dailyMetrics} field="mileageCharged" tone="sky" />
          <TrendChart title="마일리지 사용량" unit="P" data={dailyMetrics} field="mileageUsed" tone="amber" />
        </section>
      ) : null}
    </>
  );
}
