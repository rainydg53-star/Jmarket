import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";

import "../css/pages/ReportsPage.css";

const STATUS_LABEL = {
  PENDING: "대기",
  RESOLVED: "처리완료",
  REJECTED: "반려",
};

const STATUS_TONE = {
  PENDING: "warning",
  RESOLVED: "success",
  REJECTED: "muted",
};

const TARGET_TYPE_LABEL = {
  PRODUCT: "상품",
  TRADE: "거래",
  CHAT_ROOM: "채팅방",
  USER: "사용자",
};

const RESOLVE_ACTIONS = ["NONE", "WARNING", "PRODUCT_REMOVED", "TEMP_SUSPEND", "PERMANENT_BAN"];

const ACTION_LABEL = {
  NONE: "조치 없음",
  WARNING: "경고",
  PRODUCT_REMOVED: "상품 강제 삭제",
  TEMP_SUSPEND: "7일 로그인 차단",
  PERMANENT_BAN: "영구 로그인 차단",
};

const RESOLVE_PRESETS = [
  { status: "RESOLVED", action: "WARNING", label: "경고 처리", memo: "신고 내용을 확인하여 경고 조치했습니다." },
  { status: "RESOLVED", action: "PRODUCT_REMOVED", label: "상품 삭제", memo: "신고 대상 상품을 확인하여 강제 삭제 처리했습니다." },
  { status: "RESOLVED", action: "TEMP_SUSPEND", label: "7일 차단", memo: "운영 정책 위반으로 7일 로그인 차단 처리했습니다." },
  { status: "REJECTED", action: "NONE", label: "반려", memo: "신고 내용을 검토했으나 정책 위반으로 보기 어려워 반려했습니다." },
];

const formatReportTarget = (report) => {
  if (!report) {
    return "-";
  }
  const label = TARGET_TYPE_LABEL[report.targetType] ?? report.targetType;
  return report.targetSummary || `${label} #${report.targetId}`;
};

const getReportStatusTone = (status) => STATUS_TONE[status] ?? "muted";

function ReportDetailPage() {
  const { reportId } = useParams();
  const navigate = useNavigate();
  const [me, setMe] = useState(null);
  const [report, setReport] = useState(null);
  const [resolveStatus, setResolveStatus] = useState("RESOLVED");
  const [resolveAction, setResolveAction] = useState("NONE");
  const [resolveMemo, setResolveMemo] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("신고 상세를 불러오는 중입니다.");

  const isAdmin = me?.role === "ADMIN" || me?.role === "SUPER_ADMIN";

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const formatDateTime = (value) => {
    if (!value) {
      return "-";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      timeZone: "Asia/Seoul",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(new Date(value));
  };

  const reportTargetLink = (item) => {
    if (!item) {
      return null;
    }
    if (item.targetType === "PRODUCT") {
      return { to: `/products/${item.targetId}`, label: "상품 상세 보기" };
    }
    if (item.targetType === "CHAT_ROOM") {
      return { to: `/chat/rooms/${item.targetId}`, label: "채팅방 보기" };
    }
    if (item.targetType === "TRADE") {
      return { to: "/trades", label: "거래 목록 보기" };
    }
    return null;
  };

  const reportOwnerLink = (item) => {
    if (!item?.targetOwnerUserId) {
      return null;
    }
    return { to: `/users/${item.targetOwnerUserId}`, label: "대상 사용자 프로필 보기" };
  };

  const reportReporterLink = (item) => {
    if (!item?.reporterId) {
      return null;
    }
    return { to: `/users/${item.reporterId}`, label: "신고자 프로필 보기" };
  };

  const dedupeLinks = (links) => {
    const seen = new Set();
    return links.filter((link) => {
      if (!link || seen.has(link.to)) {
        return false;
      }
      seen.add(link.to);
      return true;
    });
  };

  const loadReport = useCallback(async () => {
    setLoading(true);
    try {
      const meRes = await api("/api/auth/me");
      const adminLike = meRes.role === "ADMIN" || meRes.role === "SUPER_ADMIN";
      const detail = await api(adminLike ? `/api/admin/reports/${reportId}` : `/api/reports/${reportId}`);
      setMe(meRes);
      setReport(detail);
      setResolveStatus(detail.status === "PENDING" ? "RESOLVED" : detail.status);
      setResolveAction(detail.resolutionAction ?? "NONE");
      setResolveMemo(detail.resolutionMemo ?? "");
      setMessage("신고 상세 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`신고 상세 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized, reportId]);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  const resolveReport = async () => {
    if (!report) {
      return;
    }
    if (resolveStatus === "RESOLVED" && resolveAction !== "NONE" && !resolveMemo.trim()) {
      setMessage("처리 메모를 입력해주세요.");
      return;
    }
    if (resolveStatus === "REJECTED" && !resolveMemo.trim()) {
      setMessage("반려 사유를 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      const updated = await api(`/api/admin/reports/${report.id}/resolve`, {
        method: "PATCH",
        body: JSON.stringify({
          status: resolveStatus,
          resolutionAction: resolveAction,
          resolutionMemo: resolveMemo.trim(),
        }),
      });
      setReport(updated);
      setMessage("신고 처리 완료");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`신고 처리 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const shouldShowMessage = loading || message.includes("실패") || message.includes("입력") || message.includes("반려") || message.includes("불러오는 중");

  return (
    <main className="container">
      <div className="report-detail-head">
        <div>
          <p className="meta">
            <Link to={isAdmin ? "/reports?mode=list" : "/reports?mode=list"}>신고 목록으로</Link>
          </p>
          <h1>신고 상세</h1>
        </div>
        {report ? (
          <span className={`status-badge ${getReportStatusTone(report.status)}`}>
            {STATUS_LABEL[report.status] ?? report.status}
          </span>
        ) : null}
      </div>

      {shouldShowMessage ? (
        <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
          {loading ? "요청 처리 중입니다." : message}
        </p>
      ) : null}

      {!report ? (
        <div className="card">
          <p className="empty-box">신고 상세 정보가 없습니다.</p>
        </div>
      ) : (
        <div className="card report-detail-card">
          <div className="report-detail-head">
            <div>
              <h2>신고 #{report.id}</h2>
              <p className="meta">접수일시: {formatDateTime(report.createdAt)}</p>
            </div>
            <span className={`status-badge ${getReportStatusTone(report.status)}`}>
              {STATUS_LABEL[report.status] ?? report.status}
            </span>
          </div>

          <div className="report-summary-grid">
            <div>
              <span className="meta">신고 대상</span>
              <strong>{formatReportTarget(report)}</strong>
            </div>
            <div>
              <span className="meta">대상 누적 신고</span>
              <strong>{report.targetReportCount ?? 0}건</strong>
            </div>
            <div>
              <span className="meta">대상 사용자 누적 신고</span>
              <strong>{report.targetOwnerUserId ? `${report.targetOwnerReportCount ?? 0}건` : "-"}</strong>
            </div>
          </div>

          <p><strong>신고자</strong> {report.reporterNickname} ({report.reporterEmail})</p>
          <p><strong>대상 유형</strong> {TARGET_TYPE_LABEL[report.targetType] ?? report.targetType}</p>
          <p><strong>대상 사용자</strong> {report.targetOwnerNickname ? `${report.targetOwnerNickname} #${report.targetOwnerUserId}` : "-"}</p>
          <p><strong>사유</strong> {report.reason}</p>
          <p><strong>내용</strong> {report.detail}</p>
          <p><strong>처리 조치</strong> {ACTION_LABEL[report.resolutionAction] ?? report.resolutionAction ?? "-"}</p>
          <p><strong>처리 메모</strong> {report.resolutionMemo || "-"}</p>
          <p><strong>처리일시</strong> {formatDateTime(report.processedAt)}</p>

          <div className="actions">
            {dedupeLinks([
              reportTargetLink(report),
              reportOwnerLink(report),
              isAdmin ? reportReporterLink(report) : null,
            ]).map((link) => (
              <Link className="report-action-link" to={link.to} key={link.to}>{link.label}</Link>
            ))}
          </div>

          {isAdmin && report.status === "PENDING" ? (
            <>
              <div className="report-resolve-panel">
                <h3>빠른 처리</h3>
                <div className="actions compact-actions">
                  {RESOLVE_PRESETS.map((preset) => (
                    <button
                      key={preset.label}
                      type="button"
                      onClick={() => {
                        setResolveStatus(preset.status);
                        setResolveAction(preset.action);
                        setResolveMemo(preset.memo);
                      }}
                      disabled={loading}
                    >
                      {preset.label}
                    </button>
                  ))}
                </div>
              </div>

              <label>처리 상태</label>
              <select className="select" value={resolveStatus} onChange={(e) => setResolveStatus(e.target.value)} disabled={loading}>
                <option value="RESOLVED">처리완료</option>
                <option value="REJECTED">반려</option>
              </select>

              <label>처리 조치</label>
              <select className="select" value={resolveAction} onChange={(e) => setResolveAction(e.target.value)} disabled={loading}>
                {RESOLVE_ACTIONS.map((action) => (
                  <option key={action} value={action}>{ACTION_LABEL[action]}</option>
                ))}
              </select>

              <label>처리 메모</label>
              <textarea
                className="textarea"
                value={resolveMemo}
                onChange={(e) => setResolveMemo(e.target.value)}
                disabled={loading}
                maxLength={1000}
                placeholder="처리 내용을 입력해주세요."
              />

              <div className="actions">
                <button onClick={resolveReport} disabled={loading}>처리 저장</button>
              </div>
            </>
          ) : null}
        </div>
      )}
    </main>
  );
}

export default ReportDetailPage;
