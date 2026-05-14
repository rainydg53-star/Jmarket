import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import ConfirmModal from "../components/ConfirmModal";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { openChatWindow } from "../lib/chatWindow";
import { getTradeStatusTone } from "../lib/statusTone";

function TradesPage() {
  const navigate = useNavigate();
  const [me, setMe] = useState(null);
  const [trades, setTrades] = useState([]);
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [reviewDrafts, setReviewDrafts] = useState({});
  const [confirmModal, setConfirmModal] = useState(null);
  const [message, setMessage] = useState("거래 목록을 불러오는 중...");
  const [loading, setLoading] = useState(false);

  const statusLabel = (status) => {
    if (status === "REQUESTED") {
      return "구매 대기중";
    }
    if (status === "ACCEPTED") {
      return "거래중";
    }
    if (status === "COMPLETED") {
      return "거래완료";
    }
    if (status === "CANCELED") {
      return "취소";
    }
    return status;
  };

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const loadMe = useCallback(async () => {
    try {
      const response = await api("/api/auth/me");
      setMe(response);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
      }
    }
  }, [handleUnauthorized]);

  const loadTrades = useCallback(async (role) => {
    setLoading(true);
    try {
      const response = await api(`/api/trades/me?role=${role}`);
      setTrades(response);
      setMessage("거래 목록 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`거래 목록 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  const runTradeAction = async (tradeId, actionName) => {
    setLoading(true);
    try {
      await api(`/api/trades/${tradeId}/${actionName}`, { method: "PATCH" });
      await loadTrades(roleFilter);
      setMessage(`거래 ${actionName} 처리 성공`);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`거래 처리 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const confirmCancelTrade = (trade) => {
    setConfirmModal({
      title: "거래 취소",
      message: `${trade.productTitle} 거래를 취소할까요?`,
      detail: "취소 후에는 해당 거래 진행 상태가 종료됩니다.",
      confirmLabel: "거래 취소",
      onConfirm: async () => {
        await runTradeAction(trade.id, "cancel");
        setConfirmModal(null);
      },
    });
  };

  const openTradeChatRoom = async (tradeId) => {
    setLoading(true);
    try {
      const room = await api("/api/chat/rooms/trade", {
        method: "POST",
        body: JSON.stringify({ tradeId }),
      });
      openChatWindow(room.id);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`채팅방 생성 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const updateReviewDraft = (key, patch) => {
    setReviewDrafts((prev) => ({
      ...prev,
      [key]: { rating: 5, content: "", ...(prev[key] || {}), ...patch },
    }));
  };

  const submitReview = async (trade) => {
    const targetUserId = trade.buyerId === me.id ? trade.sellerId : trade.buyerId;
    const key = `TRADE-${trade.id}`;
    const draft = { rating: 5, content: "", ...(reviewDrafts[key] || {}) };
    if (!draft.content.trim()) {
      setMessage("리뷰 내용을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api("/api/reviews", {
        method: "POST",
        body: JSON.stringify({
          targetUserId,
          sourceType: "TRADE",
          sourceId: trade.id,
          rating: Number(draft.rating),
          content: draft.content.trim(),
        }),
      });
      setReviewDrafts((prev) => ({ ...prev, [key]: { rating: 5, content: "" } }));
      await loadTrades(roleFilter);
      setMessage("리뷰 등록 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`리뷰 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMe();
    loadTrades("ALL");
  }, [loadMe, loadTrades]);

  const onChangeRoleFilter = async (nextRole) => {
    setRoleFilter(nextRole);
    await loadTrades(nextRole);
  };
  const shouldShowMessage = loading || message.includes("실패") || message.includes("입력") || message.includes("처리 성공") || message.includes("등록 성공");

  const renderActions = (trade) => {
    if (!me) {
      return null;
    }

    const isBuyer = trade.buyerId === me.id;
    const isSeller = trade.sellerId === me.id;
    const actions = [];

    // 레거시 REQUESTED 거래 대응: 판매자가 수락해서 거래중으로 전환
    if (trade.status === "REQUESTED" && isSeller) {
      actions.push(
        <button key="accept" onClick={() => runTradeAction(trade.id, "accept")} disabled={loading}>거래시작(수락)</button>
      );
    }
    if (trade.status === "ACCEPTED" && isSeller && !trade.sellerConfirmedHanded) {
      actions.push(
        <button key="seller-confirm" onClick={() => runTradeAction(trade.id, "complete")} disabled={loading}>인계확인</button>
      );
    }
    if (trade.status === "ACCEPTED" && isBuyer && !trade.buyerConfirmedReceived) {
      actions.push(
        <button key="buyer-confirm" onClick={() => runTradeAction(trade.id, "complete")} disabled={loading}>인수확인</button>
      );
    }
    if ((trade.status === "REQUESTED" || trade.status === "ACCEPTED") && (isBuyer || isSeller)) {
      actions.push(
        <button key="cancel" className="danger-button" onClick={() => confirmCancelTrade(trade)} disabled={loading}>취소</button>
      );
    }

    if (actions.length === 0) {
      return <span className="meta">가능한 액션 없음</span>;
    }
    return <div className="actions">{actions}</div>;
  };

  const renderReviewForm = (trade) => {
    if (!me || trade.status !== "COMPLETED") {
      return null;
    }
    const targetNickname = trade.reviewTargetUserNickname || (trade.buyerId === me.id ? trade.sellerNickname : trade.buyerNickname);
    const key = `TRADE-${trade.id}`;
    const draft = { rating: 5, content: "", ...(reviewDrafts[key] || {}) };
    if (trade.reviewedByMe) {
      return (
        <div className="review-box">
          <strong>{targetNickname}님 리뷰 작성 완료</strong>
          <p className="meta">이 거래에 대한 후기를 이미 남겼습니다.</p>
        </div>
      );
    }
    return (
      <div className="review-box">
        <strong>{targetNickname}님에게 거래 후기 남기기</strong>
        <select
          className="select"
          value={draft.rating}
          onChange={(e) => updateReviewDraft(key, { rating: e.target.value })}
          disabled={loading}
        >
          {[5, 4, 3, 2, 1].map((rating) => (
            <option key={rating} value={rating}>{rating}점</option>
          ))}
        </select>
        <textarea
          className="textarea"
          value={draft.content}
          onChange={(e) => updateReviewDraft(key, { content: e.target.value })}
          placeholder="거래 경험을 남겨주세요."
          disabled={loading}
        />
        <button type="button" onClick={() => submitReview(trade)} disabled={loading}>리뷰 등록</button>
      </div>
    );
  };

  return (
    <main className="container">
      <h1>거래</h1>
      <div className="card">
        <label>내 거래 필터</label>
        <select
          value={roleFilter}
          onChange={(e) => onChangeRoleFilter(e.target.value)}
          disabled={loading}
          className="select"
        >
          <option value="ALL">전체</option>
          <option value="BUYER">구매자 거래</option>
          <option value="SELLER">판매자 거래</option>
        </select>

        <div className="actions">
          <button className="secondary-button" onClick={() => loadTrades()} disabled={loading}>목록 새로고침</button>
        </div>
      </div>

      <div className="card">
        <h2>거래 목록</h2>
        {shouldShowMessage ? (
          <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
            {loading ? "요청 처리 중..." : message}
          </p>
        ) : null}
        {trades.length === 0 ? (
          <p className="empty-box">거래 내역이 없습니다.</p>
        ) : (
          <ul className="list">
            {trades.map((trade) => (
              <li key={trade.id} className="list-item">
                <strong>{trade.productTitle}</strong>
                <span>
                  상태: <span className={`status-badge ${getTradeStatusTone(trade.status)}`}>{statusLabel(trade.status)}</span>
                </span>
                <span>거래금액: {trade.offeredPrice.toLocaleString()}원</span>
                <span>결제수단: 마일리지</span>
                <span>예약 마일리지: {trade.reservedMileageAmount.toLocaleString()}점</span>
                {trade.status === "ACCEPTED" ? (
                  <span>
                    확인상태: 판매자 인계 {trade.sellerConfirmedHanded ? "완료" : "대기"} / 구매자 인수 {trade.buyerConfirmedReceived ? "완료" : "대기"}
                  </span>
                ) : null}
                <span className="meta">구매자: {trade.buyerNickname} / 판매자: {trade.sellerNickname}</span>
                {trade.status === "COMPLETED" ? (
                  <span className="meta">
                    리뷰 상태: {trade.reviewedByMe ? "작성 완료" : `${trade.reviewTargetUserNickname || "상대방"}님 리뷰 작성 가능`}
                  </span>
                ) : null}
                {renderActions(trade)}
                {renderReviewForm(trade)}
                <div className="actions">
                  <button onClick={() => openTradeChatRoom(trade.id)} disabled={loading}>채팅 시작</button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
      {confirmModal ? (
        <ConfirmModal
          title={confirmModal.title}
          message={confirmModal.message}
          detail={confirmModal.detail}
          confirmLabel={confirmModal.confirmLabel}
          loading={loading}
          onCancel={() => setConfirmModal(null)}
          onConfirm={confirmModal.onConfirm}
        />
      ) : null}
    </main>
  );
}

export default TradesPage;
