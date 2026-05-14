import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { getNotificationLinkLabel, resolveNotificationLink } from "../lib/notificationLinks";

const TYPE_LABELS = {
  AUCTION_OUTBID: "상위입찰",
  AUCTION_OUTBID_LOST: "입찰밀림",
  AUCTION_WON: "낙찰",
  AUCTION_SOLD: "낙찰확정",
  TRADE_REQUESTED: "구매신청",
  TRADE_COMPLETED: "거래완료",
  TRADE_REVIEW_REQUESTED: "후기요청",
  REVIEW_RECEIVED: "리뷰",
  REPORT_RESOLVED: "신고처리",
  USER_RESTRICTED: "이용제한",
  CHAT_MESSAGE: "채팅",
};

const FILTER_OPTIONS = [
  { value: "ALL", label: "전체" },
  { value: "AUCTION", label: "경매" },
  { value: "TRADE", label: "거래" },
  { value: "CHAT", label: "채팅" },
  { value: "REVIEW", label: "리뷰" },
  { value: "REPORT", label: "신고" },
  { value: "SYSTEM", label: "운영" },
];

const TYPE_GROUPS = {
  AUCTION_OUTBID: "AUCTION",
  AUCTION_OUTBID_LOST: "AUCTION",
  AUCTION_WON: "AUCTION",
  AUCTION_SOLD: "AUCTION",
  TRADE_REQUESTED: "TRADE",
  TRADE_COMPLETED: "TRADE",
  TRADE_REVIEW_REQUESTED: "TRADE",
  REVIEW_RECEIVED: "REVIEW",
  REPORT_RESOLVED: "REPORT",
  USER_RESTRICTED: "SYSTEM",
  CHAT_MESSAGE: "CHAT",
};

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("ko-KR");
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [unreadOnly, setUnreadOnly] = useState(true);
  const [selectedType, setSelectedType] = useState("ALL");
  const [items, setItems] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadAll = async (nextUnreadOnly = unreadOnly) => {
    setLoading(true);
    setError("");
    try {
      const [list, unread] = await Promise.all([
        api(`/api/notifications/me?unreadOnly=${nextUnreadOnly ? "true" : "false"}`),
        api("/api/notifications/me/unread-count"),
      ]);
      setItems(list);
      setUnreadCount(unread.unreadCount ?? 0);
    } catch (err) {
      setError(err.message || "알림을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const visibleItems = useMemo(() => {
    const filtered = selectedType === "ALL"
      ? items
      : items.filter((item) => TYPE_GROUPS[item.type] === selectedType);
    return [...filtered].sort((a, b) => {
      if (!a.readAt && b.readAt) {
        return -1;
      }
      if (a.readAt && !b.readAt) {
        return 1;
      }
      return Number(b.id ?? 0) - Number(a.id ?? 0);
    });
  }, [items, selectedType]);

  const filteredUnreadCount = visibleItems.filter((item) => !item.readAt).length;

  const handleUnreadOnlyChange = async (next) => {
    setUnreadOnly(next);
    await loadAll(next);
  };

  const handleReadOne = async (notificationId) => {
    try {
      await api(`/api/notifications/${notificationId}/read`, { method: "PATCH" });
      await loadAll();
    } catch (err) {
      setError(err.message || "읽음 처리에 실패했습니다.");
    }
  };

  const handleReadAll = async () => {
    try {
      await api("/api/notifications/read-all", { method: "PATCH" });
      await loadAll();
    } catch (err) {
      setError(err.message || "전체 읽음 처리에 실패했습니다.");
    }
  };

  const handleOpenNotification = async (item) => {
    try {
      if (!item.readAt) {
        await api(`/api/notifications/${item.id}/read`, { method: "PATCH" });
      }
      const link = resolveNotificationLink(item);
      if (link) {
        navigate(link);
        return;
      }
      await loadAll();
    } catch (err) {
      setError(err.message || "알림 처리에 실패했습니다.");
    }
  };

  return (
    <main className="container">
      <h1>알림</h1>
      <section className="card">
        <div className="notification-summary">
          <div>
            <p className="meta">읽지 않은 알림</p>
            <strong>{unreadCount.toLocaleString()}건</strong>
          </div>
          <div>
            <p className="meta">현재 보기</p>
            <strong>{visibleItems.length.toLocaleString()}건</strong>
          </div>
          <div>
            <p className="meta">필터 내 미읽음</p>
            <strong>{filteredUnreadCount.toLocaleString()}건</strong>
          </div>
        </div>

        <div className="pill-tabs">
          {FILTER_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={`pill-tab${selectedType === option.value ? " active" : ""}`}
              onClick={() => setSelectedType(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>

        <div className="actions">
          <button type="button" onClick={() => handleUnreadOnlyChange(!unreadOnly)}>
            {unreadOnly ? "전체 보기" : "읽지 않은 알림만"}
          </button>
          <button type="button" onClick={handleReadAll} disabled={unreadCount === 0 || loading}>
            전체 읽음
          </button>
          <button type="button" onClick={() => loadAll()} disabled={loading}>
            새로고침
          </button>
        </div>
        {error ? <p className="page-message error">{error}</p> : null}
      </section>

      <section className="card">
        {loading ? <p className="page-message loading">불러오는 중...</p> : null}
        {!loading && visibleItems.length === 0 ? <p className="empty-box">표시할 알림이 없습니다.</p> : null}
        {!loading && visibleItems.length > 0 ? (
          <ul className="list notification-list">
            {visibleItems.map((item) => (
              <li key={item.id} className={`list-item notification-center-item${item.readAt ? "" : " unread-item"}`}>
                <div className="notification-item-head">
                  <span className={`notification-badge badge-${String(item.type || "").toLowerCase()}`}>
                    {TYPE_LABELS[item.type] ?? item.type ?? "알림"}
                  </span>
                  {!item.readAt ? <span className="unread-dot">읽지 않음</span> : <span className="read-dot">읽음</span>}
                </div>
                <strong>{item.title}</strong>
                <span>{item.message}</span>
                <span className="meta">
                  발생: {formatDateTime(item.occurredAt)} / 읽음: {formatDateTime(item.readAt)}
                </span>
                <div className="actions">
                  {resolveNotificationLink(item) ? (
                    <button type="button" onClick={() => handleOpenNotification(item)}>
                      {getNotificationLinkLabel(item)}
                    </button>
                  ) : null}
                  {!item.readAt ? (
                    <button type="button" onClick={() => handleReadOne(item.id)}>
                      읽음 처리
                    </button>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        ) : null}
      </section>
    </main>
  );
}
