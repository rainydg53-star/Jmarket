import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
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

const TARGET_TYPES = ["PRODUCT", "TRADE", "CHAT_ROOM", "USER"];
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

const ACTION_DESCRIPTION = {
  NONE: "신고 내용을 확인했지만 별도 제재 없이 기록만 남깁니다.",
  WARNING: "대상 사용자에게 경고 조치를 남깁니다.",
  PRODUCT_REMOVED: "문제가 된 상품을 운영자가 강제로 삭제한 것으로 기록합니다.",
  TEMP_SUSPEND: "대상 사용자를 일정 기간 로그인 차단한 것으로 기록합니다.",
  PERMANENT_BAN: "대상 사용자를 영구 로그인 차단한 것으로 기록합니다.",
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

function ReportsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [me, setMe] = useState(null);
  const [targetType, setTargetType] = useState("PRODUCT");
  const [targetId, setTargetId] = useState("");
  const [targetDisplay, setTargetDisplay] = useState("");
  const [reason, setReason] = useState("");
  const [detail, setDetail] = useState("");
  const [reports, setReports] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [selected, setSelected] = useState(null);
  const [resolveStatus, setResolveStatus] = useState("RESOLVED");
  const [resolveAction, setResolveAction] = useState("NONE");
  const [resolveMemo, setResolveMemo] = useState("");
  const [reportStatusFilter, setReportStatusFilter] = useState("PENDING");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("신고 정보를 불러오는 중...");
  const [modalState, setModalState] = useState({ open: false, title: "", body: "" });

  const isAdmin = me?.role === "ADMIN" || me?.role === "SUPER_ADMIN";
  const viewMode = searchParams.get("mode") === "list" ? "list" : "create";
  const showCreate = !isAdmin && viewMode === "create";
  const showList = isAdmin || viewMode === "list";
  const pageTitle = showCreate ? "신고 등록" : isAdmin ? "전체 신고 관리" : "내 신고 목록";

  const filteredReports = useMemo(() => {
    if (!isAdmin) {
      return reports;
    }
    return reports.filter((report) => report.status === reportStatusFilter);
  }, [isAdmin, reports, reportStatusFilter]);

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const openModal = useCallback((title, body) => {
    setModalState({ open: true, title, body });
  }, []);

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

  const reportTargetLink = (report) => {
    if (!report) {
      return null;
    }
    if (report.targetType === "PRODUCT") {
      return { to: `/products/${report.targetId}`, label: "상품 상세 보기" };
    }
    if (report.targetType === "USER") {
      return null;
    }
    if (report.targetType === "CHAT_ROOM") {
      return { to: `/chat/rooms/${report.targetId}`, label: "채팅방 보기" };
    }
    if (report.targetType === "TRADE") {
      return { to: "/trades", label: "거래 목록 보기" };
    }
    return null;
  };

  const reportOwnerLink = (report) => {
    if (!report?.targetOwnerUserId) {
      return null;
    }
    return { to: `/users/${report.targetOwnerUserId}`, label: "대상 사용자 프로필 보기" };
  };

  const reportReporterLink = (report) => {
    if (!report?.reporterId) {
      return null;
    }
    return { to: `/users/${report.reporterId}`, label: "사용자 프로필 보기" };
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

  const resolveTargetDisplay = useCallback(async (nextType, nextId) => {
    const numericTargetId = Number(nextId);
    if (!Number.isFinite(numericTargetId) || numericTargetId <= 0) {
      setTargetDisplay("");
      return;
    }

    try {
      if (nextType === "PRODUCT") {
        const product = await api(`/api/products/${numericTargetId}`);
        setTargetDisplay(`${product.title} · 판매자 ${product.sellerNickname}`);
        return;
      }
      if (nextType === "USER") {
        const profile = await api(`/api/users/${numericTargetId}/profile`);
        setTargetDisplay(profile.nickname);
        return;
      }
      if (nextType === "TRADE") {
        const trade = await api(`/api/trades/${numericTargetId}`);
        setTargetDisplay(`${trade.productTitle} · ${trade.buyerNickname} / ${trade.sellerNickname}`);
        return;
      }
      if (nextType === "CHAT_ROOM") {
        const room = await api(`/api/chat/rooms/${numericTargetId}`);
        setTargetDisplay(`${room.participantANickname} / ${room.participantBNickname}`);
        return;
      }
      setTargetDisplay("");
    } catch {
      setTargetDisplay("");
    }
  }, []);

  const loadMeAndReports = useCallback(async () => {
    setLoading(true);
    try {
      const meRes = await api("/api/auth/me");
      setMe(meRes);
      const adminLike = meRes.role === "ADMIN" || meRes.role === "SUPER_ADMIN";
      const list = await api(adminLike ? "/api/admin/reports" : "/api/reports/me");
      setReports(list);
      const reportIdFromUrl = searchParams.get("reportId");
      if (reportIdFromUrl) {
        const numericReportId = Number(reportIdFromUrl);
        if (Number.isFinite(numericReportId)) {
          setSelectedId(numericReportId);
          const adminLike = meRes.role === "ADMIN" || meRes.role === "SUPER_ADMIN";
          const detailPath = adminLike ? `/api/admin/reports/${numericReportId}` : `/api/reports/${numericReportId}`;
          const detail = await api(detailPath);
          setSelected(detail);
          setResolveStatus(detail.status === "PENDING" ? "RESOLVED" : detail.status);
          setResolveAction(detail.resolutionAction ?? "NONE");
          setResolveMemo(detail.resolutionMemo ?? "");
        }
      }
      setMessage("신고 목록 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`신고 목록 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized, searchParams]);

  const loadDetail = async (reportId) => {
    setLoading(true);
    try {
      const path = isAdmin ? `/api/admin/reports/${reportId}` : `/api/reports/${reportId}`;
      const res = await api(path);
      setSelected(res);
      setResolveStatus(res.status === "PENDING" ? "RESOLVED" : res.status);
      setResolveAction(res.resolutionAction ?? "NONE");
      setResolveMemo(res.resolutionMemo ?? "");
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
  };

  const createReport = async () => {
    const numericTargetId = Number(targetId);
    if (!Number.isFinite(numericTargetId) || numericTargetId <= 0) {
      openModal("입력 확인", "대상 ID는 1 이상의 숫자여야 합니다.");
      return;
    }
    if (!reason.trim()) {
      openModal("입력 확인", "신고 사유를 입력해주세요.");
      return;
    }
    if (!detail.trim()) {
      openModal("입력 확인", "신고 내용을 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      await api("/api/reports", {
        method: "POST",
        body: JSON.stringify({
          targetType,
          targetId: numericTargetId,
          reason: reason.trim(),
          detail: detail.trim(),
        }),
      });
      setReason("");
      setDetail("");
      setTargetId("");
      setTargetDisplay("");
      await loadMeAndReports();
      openModal("신고 등록 완료", "신고가 정상적으로 접수되었습니다.");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      openModal("신고 등록 실패", error.message);
    } finally {
      setLoading(false);
    }
  };

  const resolveReport = async () => {
    if (!selectedId) {
      setMessage("처리할 신고를 먼저 선택해주세요.");
      return;
    }
    if (resolveStatus === "RESOLVED" && resolveAction !== "NONE" && !resolveMemo.trim()) {
      setMessage("제재 조치를 남길 때는 처리 메모를 입력해주세요.");
      return;
    }
    if (resolveStatus === "REJECTED" && !resolveMemo.trim()) {
      setMessage("반려할 때는 반려 사유를 처리 메모에 남겨주세요.");
      return;
    }
    setLoading(true);
    try {
      await api(`/api/admin/reports/${selectedId}/resolve`, {
        method: "PATCH",
        body: JSON.stringify({
          status: resolveStatus,
          resolutionAction: resolveAction,
          resolutionMemo: resolveMemo.trim(),
        }),
      });
      await loadDetail(selectedId);
      await loadMeAndReports();
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

  useEffect(() => {
    const prefillTargetType = searchParams.get("targetType");
    const prefillTargetId = searchParams.get("targetId");
    const prefillReason = searchParams.get("reason");
    const resolvedPrefillTargetType = prefillTargetType && TARGET_TYPES.includes(prefillTargetType)
      ? prefillTargetType
      : "PRODUCT";
    if (prefillTargetType && TARGET_TYPES.includes(prefillTargetType)) {
      setTargetType(resolvedPrefillTargetType);
    }
    if (prefillTargetId) {
      setTargetId(prefillTargetId);
      resolveTargetDisplay(resolvedPrefillTargetType, prefillTargetId);
    }
    if (prefillReason) {
      setReason(prefillReason);
    }
    loadMeAndReports();
  }, [loadMeAndReports, resolveTargetDisplay, searchParams]);
  const shouldShowMessage = loading
    || message.includes("실패")
    || message.includes("입력")
    || message.includes("선택")
    || message.includes("완료")
    || message.includes("불러오는 중");

  return (
    <main className="container">
      <h1>{pageTitle}</h1>
      {shouldShowMessage ? (
        <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
          {loading ? "요청 처리 중..." : message}
        </p>
      ) : null}

      {!isAdmin ? (
        <div className="mode-switch">
          <Link className={showCreate ? "active" : ""} to="/reports?mode=create">신고 등록</Link>
          <Link className={showList ? "active" : ""} to="/reports?mode=list">내 신고 목록</Link>
        </div>
      ) : null}

      {showCreate ? (
        <div className="card">
          <h2>신고 등록</h2>
          <p className="meta">문제가 있는 상품, 거래, 채팅방, 사용자를 선택해 신고할 수 있습니다.</p>
          <label>대상 타입</label>
          <select
            className="select"
            value={targetType}
            onChange={(e) => {
              setTargetType(e.target.value);
              setTargetId("");
              setTargetDisplay("");
            }}
            disabled={loading}
          >
            {TARGET_TYPES.map((type) => (
              <option key={type} value={type}>{TARGET_TYPE_LABEL[type]}</option>
            ))}
          </select>

          <label>신고 대상</label>
          {targetDisplay ? (
            <div className="report-target-display">
              <strong>{targetDisplay}</strong>
            </div>
          ) : (
            <>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={targetId}
                onChange={(e) => {
                  const nextId = e.target.value.replace(/\D/g, "");
                  setTargetId(nextId);
                  resolveTargetDisplay(targetType, nextId);
                }}
                disabled={loading}
                placeholder="상품/프로필/거래/채팅방에서 신고 버튼을 누르면 자동으로 입력됩니다"
              />
              <p className="meta">직접 입력 시 숫자만 입력됩니다. 오른쪽 증감 버튼은 표시되지 않습니다.</p>
            </>
          )}

          <label>신고 사유</label>
          <input value={reason} onChange={(e) => setReason(e.target.value)} disabled={loading} maxLength={100} />

          <label>신고 내용</label>
          <textarea className="textarea" value={detail} onChange={(e) => setDetail(e.target.value)} disabled={loading} maxLength={5000} />

          <div className="actions">
            <button onClick={createReport} disabled={loading}>신고 등록</button>
          </div>
        </div>
      ) : null}

      {showList ? (
        <div className="card">
          <h2>{isAdmin ? "전체 신고 목록" : "내 신고 목록"}</h2>
          <p className="meta">{isAdmin ? "접수된 신고를 확인하고 처리 상태를 저장할 수 있습니다." : "내가 등록한 신고의 처리 상태를 확인할 수 있습니다."}</p>
          {isAdmin ? (
            <div className="mode-switch report-status-filter" aria-label="신고 상태 필터">
              {["PENDING", "RESOLVED", "REJECTED"].map((status) => (
                <button
                  key={status}
                  type="button"
                  className={reportStatusFilter === status ? "active" : ""}
                  onClick={() => setReportStatusFilter(status)}
                  disabled={loading}
                >
                  {STATUS_LABEL[status] ?? status}
                </button>
              ))}
            </div>
          ) : null}
          {filteredReports.length === 0 ? (
            <p className="empty-box">등록된 신고가 없습니다.</p>
          ) : (
            <ul className="list">
              {filteredReports.map((report) => {
                const targetLink = reportTargetLink(report);
                const ownerLink = reportOwnerLink(report);
                const reporterLink = reportReporterLink(report);
                const actionLinks = dedupeLinks([
                  targetLink,
                  ownerLink,
                  isAdmin ? reporterLink : null,
                ]);
                return (
                  <li key={report.id} className="list-item">
                    <button
                      type="button"
                      disabled={loading}
                      onClick={() => navigate(`/reports/${report.id}`)}
                    >
                      #{report.id} {report.reason}
                    </button>
                    <span className="meta">대상: {formatReportTarget(report)}</span>
                    {report.targetOwnerNickname ? (
                      <span className="meta">대상 사용자: {report.targetOwnerNickname} #{report.targetOwnerUserId}</span>
                    ) : null}
                    <span className="meta">
                      누적 신고: 대상 {report.targetReportCount ?? 0}건
                      {report.targetOwnerUserId ? ` / 사용자 ${report.targetOwnerReportCount ?? 0}건` : ""}
                    </span>
                    <span className="meta">
                      상태: <span className={`status-badge ${getReportStatusTone(report.status)}`}>{STATUS_LABEL[report.status] ?? report.status}</span>
                    </span>
                    <span className="meta">신고자: {report.reporterNickname} #{report.reporterId}</span>
                    <span className="meta">{formatDateTime(report.createdAt)}</span>
                    <div className="actions">
                      {actionLinks.map((link) => (
                        <Link className="report-action-link" to={link.to} key={link.to}>{link.label}</Link>
                      ))}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      ) : null}

      {showList && selected ? (
        <div className="card report-detail-card">
          <div className="report-detail-head">
            <div>
              <h2>신고 상세</h2>
              <p className="meta">신고 #{selected.id} · {formatDateTime(selected.createdAt)}</p>
            </div>
            <span className={"status-badge " + getReportStatusTone(selected.status)}>{STATUS_LABEL[selected.status] ?? selected.status}</span>
          </div>

          <div className="report-summary-grid">
            <div>
              <span className="meta">신고 대상</span>
              <strong>{formatReportTarget(selected)}</strong>
            </div>
            <div>
              <span className="meta">대상 누적 신고</span>
              <strong>{selected.targetReportCount ?? 0}건</strong>
            </div>
            <div>
              <span className="meta">대상 사용자 누적 신고</span>
              <strong>{selected.targetOwnerUserId ? (selected.targetOwnerReportCount ?? 0) + "건" : "-"}</strong>
            </div>
          </div>

          <p><strong>신고자:</strong> {selected.reporterNickname} ({selected.reporterEmail})</p>
          <p><strong>대상 타입:</strong> {TARGET_TYPE_LABEL[selected.targetType] ?? selected.targetType}</p>
          <p><strong>대상 사용자:</strong> {selected.targetOwnerNickname ? selected.targetOwnerNickname + " #" + selected.targetOwnerUserId : "-"}</p>
          <p><strong>사유:</strong> {selected.reason}</p>
          <p><strong>내용:</strong> {selected.detail}</p>
          <p><strong>처리 조치:</strong> {ACTION_LABEL[selected.resolutionAction] ?? selected.resolutionAction ?? "-"}</p>
          <p><strong>처리 메모:</strong> {selected.resolutionMemo || "-"}</p>
          <div className="actions">
            {dedupeLinks([
              reportTargetLink(selected),
              reportOwnerLink(selected),
              isAdmin ? reportReporterLink(selected) : null,
            ]).map((link) => (
              <Link className="report-action-link" to={link.to} key={link.to}>{link.label}</Link>
            ))}
          </div>

          {isAdmin && selected.status === "PENDING" ? (
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
                <p className="meta">{ACTION_DESCRIPTION[resolveAction]}</p>
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
      ) : null}

      {modalState.open ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>{modalState.title}</h2>
            <p>{modalState.body}</p>
            <div className="actions">
              <button type="button" onClick={() => setModalState({ open: false, title: "", body: "" })}>
                확인
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

export default ReportsPage;
