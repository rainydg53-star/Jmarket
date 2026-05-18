import "../css/pages/MePage.css";
﻿import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { openChatWindow } from "../lib/chatWindow";
import { getChatRoomTypeLabel } from "../lib/chatRoomType";
import { API_BASE_URL } from "../lib/config";
import { getTradeStatusTone } from "../lib/statusTone";

const SIDE_MENUS = [
  { key: "PURCHASE", label: "구매내역" },
  { key: "AUCTION_PURCHASE", label: "경매 구매내역" },
  { key: "SALES", label: "판매내역" },
  { key: "FAVORITES", label: "내찜목록" },
  { key: "RECENT_PRODUCTS", label: "최근본 상품" },
  { key: "COMPLETED", label: "종료내역" },
  { key: "CANCELED", label: "취소내역" },
  { key: "REPORTS", label: "신고내역" },
  { key: "CHATS", label: "채팅내역" },
  { key: "REVIEWS", label: "받은 리뷰" },
  { key: "PROFILE", label: "개인정보-개인정보수정" },
];

function MePage() {
  const navigate = useNavigate();
  const [selectedMenu, setSelectedMenu] = useState("PURCHASE");
  const [me, setMe] = useState(null);
  const [account, setAccount] = useState(null);
  const [trades, setTrades] = useState([]);
  const [auctionPurchases, setAuctionPurchases] = useState([]);
  const [reports, setReports] = useState([]);
  const [chatRooms, setChatRooms] = useState([]);
  const [favoriteProducts, setFavoriteProducts] = useState([]);
  const [recentProducts, setRecentProducts] = useState([]);
  const [reviewSummary, setReviewSummary] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [message, setMessage] = useState("내 정보를 불러오는 중...");
  const [modalMessage, setModalMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [profileForm, setProfileForm] = useState({
    name: "",
    nickname: "",
    phoneNumber: "",
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    newPasswordConfirm: "",
  });

  const formatNumber = (value) => Number(value ?? 0).toLocaleString();

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

  const formatShortDateTime = (value) => {
    if (!value) {
      return "-";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(value));
  };

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

  const loadMyHubData = useCallback(async () => {
    setLoading(true);
    try {
      const [meRes, accountRes, tradesRes, auctionPurchasesRes, reportsRes, roomsRes] = await Promise.all([
        api("/api/auth/me"),
        api("/api/mileage/me"),
        api("/api/trades/me?role=ALL"),
        api("/api/auctions/me/purchases"),
        api("/api/reports/me"),
        api("/api/chat/rooms/me"),
      ]);
      const [summaryRes, reviewsRes, favoritesRes, recentRes] = await Promise.all([
        api(`/api/users/${meRes.id}/reviews/summary`),
        api(`/api/users/${meRes.id}/reviews`),
        api("/api/products/me/favorites"),
        api("/api/products/me/recent"),
      ]);

      setMe(meRes);
      setAccount(accountRes);
      setTrades(tradesRes);
      setAuctionPurchases(auctionPurchasesRes);
      setReports(reportsRes);
      setChatRooms(roomsRes);
      setFavoriteProducts(favoritesRes);
      setRecentProducts(recentRes);
      setReviewSummary(summaryRes);
      setReviews(reviewsRes);
      setProfileForm({
        name: meRes.name ?? "",
        nickname: meRes.nickname ?? "",
        phoneNumber: meRes.phoneNumber ?? "",
      });
      setMessage("내 정보 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`내 정보 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  const saveProfile = async () => {
    const trimmedName = profileForm.name.trim();
    const trimmedNickname = profileForm.nickname.trim();
    const trimmedPhone = profileForm.phoneNumber.trim();

    if (trimmedName.length < 2) {
      setModalMessage("이름은 2자 이상 입력해주세요.");
      return;
    }
    if (!/^[a-zA-Z0-9가-힣_]{2,20}$/.test(trimmedNickname)) {
      setModalMessage("닉네임은 2~20자, 한글/영문/숫자/_ 만 사용 가능합니다.");
      return;
    }
    if (!/^\d{10,11}$/.test(trimmedPhone)) {
      setModalMessage("전화번호는 숫자만 10~11자리로 입력해주세요.");
      return;
    }

    setSavingProfile(true);
    try {
      const updated = await api("/api/auth/me", {
        method: "PATCH",
        body: JSON.stringify({
          name: trimmedName,
          nickname: trimmedNickname,
          phoneNumber: trimmedPhone,
        }),
      });
      setMe(updated);
      setProfileForm({
        name: updated.name ?? "",
        nickname: updated.nickname ?? "",
        phoneNumber: updated.phoneNumber ?? "",
      });
      setModalMessage("개인정보 수정 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setModalMessage(`개인정보 수정 실패: ${error.message}`);
    } finally {
      setSavingProfile(false);
    }
  };

  const resetPasswordForm = () => {
    setPasswordForm({
      currentPassword: "",
      newPassword: "",
      newPasswordConfirm: "",
    });
  };

  const changePassword = async () => {
    if (!passwordForm.currentPassword) {
      setModalMessage("현재 비밀번호를 입력해주세요.");
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      setModalMessage("새 비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.newPasswordConfirm) {
      setModalMessage("새 비밀번호 확인이 일치하지 않습니다.");
      return;
    }

    setSavingPassword(true);
    try {
      await api("/api/auth/me/password", {
        method: "PATCH",
        body: JSON.stringify(passwordForm),
      });
      resetPasswordForm();
      setShowPasswordForm(false);
      setModalMessage("비밀번호 변경 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setModalMessage(`비밀번호 변경 실패: ${error.message}`);
    } finally {
      setSavingPassword(false);
    }
  };

  useEffect(() => {
    loadMyHubData();
  }, [loadMyHubData]);

  const purchaseTrades = useMemo(() => {
    if (!me) {
      return [];
    }
    return trades.filter((trade) => trade.buyerId === me.id);
  }, [trades, me]);

  const salesTrades = useMemo(() => {
    if (!me) {
      return [];
    }
    return trades.filter((trade) => trade.sellerId === me.id);
  }, [trades, me]);

  const completedTrades = useMemo(
    () => trades.filter((trade) => trade.status === "COMPLETED"),
    [trades]
  );

  const canceledTrades = useMemo(
    () => trades.filter((trade) => trade.status === "CANCELED"),
    [trades]
  );

  const renderTradeList = (rows) => {
    if (rows.length === 0) {
      return <p className="empty-box">내역이 없습니다.</p>;
    }

    return (
      <ul className="list">
        {rows.map((trade) => (
          <li key={trade.id} className="list-item">
            <strong>{trade.productTitle}</strong>
            <span>
              상태: <span className={`status-badge ${getTradeStatusTone(trade.status)}`}>{statusLabel(trade.status)}</span>
            </span>
            <span>거래금액: {formatNumber(trade.offeredPrice)}원</span>
            <span className="meta">구매자: {trade.buyerNickname} / 판매자: {trade.sellerNickname}</span>
            <span className="meta">요청일시: {formatDateTime(trade.requestedAt)}</span>
          </li>
        ))}
      </ul>
    );
  };

  const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

  const renderProductList = (rows, emptyMessage) => {
    if (rows.length === 0) {
      return <p className="empty-box">{emptyMessage}</p>;
    }

    return (
      <ul className="list">
        {rows.map((product) => (
          <li key={product.id} className={`list-item product-mini-item${product.sold ? " sold" : ""}`}>
            {product.thumbnailUrl ? (
              <img className="product-mini-thumb" src={imageSrc(product.thumbnailUrl)} alt={product.title} />
            ) : null}
            <strong>{product.title}</strong>
            <span>{formatNumber(product.price)}원</span>
            <span className="meta">카테고리: {product.categoryLabel}</span>
            <span className="meta">조회 {formatNumber(product.viewCount)} · 찜 {formatNumber(product.favoriteCount)}</span>
            <span className="meta">판매자: {product.sellerNickname}</span>
            {product.tradeStatusLabel ? <span className="sold-badge">{product.tradeStatusLabel}</span> : null}
            <div className="actions">
              <button type="button" onClick={() => navigate(`/products/${product.id}`)}>상세보기</button>
            </div>
          </li>
        ))}
      </ul>
    );
  };

  const renderContent = () => {
    if (selectedMenu === "PURCHASE") {
      return renderTradeList(purchaseTrades);
    }
    if (selectedMenu === "AUCTION_PURCHASE") {
      if (auctionPurchases.length === 0) {
        return <p className="empty-box">낙찰(구매)한 경매 내역이 없습니다.</p>;
      }
      return (
        <ul className="list">
          {auctionPurchases.map((auction) => (
            <li key={auction.id} className="list-item">
              <strong>{auction.productTitle}</strong>
              <span>낙찰가: {formatNumber(auction.winningBidAmount ?? auction.currentHighestBid)}원</span>
              <span className="meta">마감일시: {formatDateTime(auction.closedAt)}</span>
              <div className="actions">
                <button type="button" onClick={() => navigate(`/auctions/${auction.id}`)}>상세보기</button>
              </div>
            </li>
          ))}
        </ul>
      );
    }
    if (selectedMenu === "SALES") {
      return renderTradeList(salesTrades);
    }
    if (selectedMenu === "FAVORITES") {
      return renderProductList(favoriteProducts, "찜한 상품이 없습니다.");
    }
    if (selectedMenu === "RECENT_PRODUCTS") {
      return renderProductList(recentProducts, "최근 본 상품이 없습니다.");
    }
    if (selectedMenu === "COMPLETED") {
      return renderTradeList(completedTrades);
    }
    if (selectedMenu === "CANCELED") {
      return renderTradeList(canceledTrades);
    }
    if (selectedMenu === "REPORTS") {
      if (reports.length === 0) {
        return <p className="empty-box">신고 내역이 없습니다.</p>;
      }
      return (
        <ul className="list">
          {reports.map((report) => (
            <li key={report.id} className="list-item">
              <strong>#{report.id} {report.reason}</strong>
              <span>{report.targetType} #{report.targetId}</span>
              <span className="meta">상태: {report.status}</span>
              <span className="meta">등록: {formatDateTime(report.createdAt)}</span>
            </li>
          ))}
        </ul>
      );
    }
    if (selectedMenu === "CHATS") {
      if (chatRooms.length === 0) {
        return <p className="empty-box">채팅 내역이 없습니다.</p>;
      }
      return (
        <ul className="list">
          {chatRooms.map((room) => {
            const roomTitle = room.roomType === "AUCTION_BID" || room.roomType === "AUCTION"
              ? (room.auctionProductTitle ? `${room.auctionProductTitle} · 경매 #${room.auctionId}` : `경매 #${room.auctionId}`)
              : (room.tradeProductTitle ? `${room.tradeProductTitle} · 거래 #${room.tradeId}` : `거래 #${room.tradeId}`);
            return (
            <li key={room.id} className="list-item">
              <strong>{roomTitle}</strong>
              <span>타입: {getChatRoomTypeLabel(room.roomType)}</span>
              <span>
                안읽음: {Number(room.unreadCount ?? 0).toLocaleString()}건 / 최근: {formatShortDateTime(room.lastMessageAt ?? room.createdAt)}
              </span>
              <span className="meta">
                마지막 메시지: {room.lastMessageContent ? `${room.lastMessageSenderNickname ?? "상대"}: ${room.lastMessageContent}` : "아직 메시지가 없습니다."}
              </span>
              <span className="meta">참여자: {room.participantANickname} / {room.participantBNickname}</span>
              <div className="actions">
                <button type="button" onClick={() => openChatWindow(room.id)}>채팅방 이동</button>
              </div>
            </li>
            );
          })}
        </ul>
      );
    }
    if (selectedMenu === "REVIEWS") {
      if (reviews.length === 0) {
        return <p className="empty-box">받은 리뷰가 없습니다.</p>;
      }
      return (
        <ul className="list">
          {reviews.map((review) => (
            <li key={review.id} className="list-item">
              <strong>{review.rating}점 · {review.content}</strong>
              <span>상품: {review.sourceTitle ?? "-"}</span>
              <span className="meta">작성자: {review.reviewerNickname}</span>
              <span className="meta">유형: {review.sourceType === "TRADE" ? "일반 거래" : "경매"} #{review.sourceId}</span>
              <span className="meta">작성일시: {formatDateTime(review.createdAt)}</span>
            </li>
          ))}
        </ul>
      );
    }

    return (
      <div className="profile-panel">
        <p className="meta">이메일(아이디): {me?.loginId ?? "-"}</p>
        <p className="meta">
          매너온도: {Number(reviewSummary?.mannerTemperature ?? 36.5).toFixed(1)}도 · 평점: {reviewSummary?.averageRating ?? 0}점 · 리뷰 {reviewSummary?.reviewCount ?? 0}개
        </p>

        <label>이름</label>
        <input
          value={profileForm.name}
          onChange={(e) => setProfileForm((prev) => ({ ...prev, name: e.target.value }))}
          disabled={savingProfile}
        />

        <label>닉네임</label>
        <input
          value={profileForm.nickname}
          onChange={(e) => setProfileForm((prev) => ({ ...prev, nickname: e.target.value }))}
          disabled={savingProfile}
        />

        <label>전화번호 (숫자만)</label>
        <input
          value={profileForm.phoneNumber}
          onChange={(e) => setProfileForm((prev) => ({ ...prev, phoneNumber: e.target.value.replace(/\D/g, "") }))}
          disabled={savingProfile}
        />

        <div className="actions">
          <button type="button" onClick={saveProfile} disabled={savingProfile}>저장</button>
          <button
            type="button"
            onClick={() => {
              setShowPasswordForm((prev) => !prev);
              resetPasswordForm();
            }}
            disabled={savingPassword}
          >
            비밀번호 변경
          </button>
        </div>

        {showPasswordForm ? (
          <div className="password-change-panel">
            <label>현재 비밀번호</label>
            <input
              type="password"
              value={passwordForm.currentPassword}
              onChange={(e) => setPasswordForm((prev) => ({ ...prev, currentPassword: e.target.value }))}
              disabled={savingPassword}
            />

            <label>변경할 비밀번호</label>
            <input
              type="password"
              value={passwordForm.newPassword}
              onChange={(e) => setPasswordForm((prev) => ({ ...prev, newPassword: e.target.value }))}
              disabled={savingPassword}
            />

            <label>비밀번호 확인</label>
            <input
              type="password"
              value={passwordForm.newPasswordConfirm}
              onChange={(e) => setPasswordForm((prev) => ({ ...prev, newPasswordConfirm: e.target.value }))}
              disabled={savingPassword}
            />

            <div className="actions">
              <button type="button" onClick={changePassword} disabled={savingPassword}>변경 완료</button>
              <button
                type="button"
                onClick={() => {
                  resetPasswordForm();
                  setShowPasswordForm(false);
                }}
                disabled={savingPassword}
              >
                취소
              </button>
            </div>
          </div>
        ) : null}
      </div>
    );
  };

  const selectedLabel = SIDE_MENUS.find((menu) => menu.key === selectedMenu)?.label ?? "내 정보";
  const shouldShowMessage = loading || message !== "내 정보 조회 성공";

  return (
    <main className="container my-page-container">
      <h1>내 정보</h1>

      <div className="card mileage-summary-card">
        <div className="summary-row">
          <div>
            <p className="meta">내 사용 가능 마일리지</p>
            <p className="summary-value">{formatNumber(account?.availableBalance)}점</p>
          </div>
          <div>
            <p className="meta">총 마일리지</p>
            <p className="summary-value">{formatNumber(account?.balance)}점</p>
          </div>
          <div>
            <p className="meta">예약 마일리지</p>
            <p className="summary-value">{formatNumber(account?.reservedBalance)}점</p>
          </div>
        </div>
        <div className="actions">
          <button type="button" className="secondary-button" onClick={loadMyHubData} disabled={loading || savingProfile || savingPassword}>새로고침</button>
          <button type="button" className="secondary-button" onClick={() => navigate("/mileage")}>충전/결제 페이지</button>
        </div>
        <div className="notice-box">
          <h3>신뢰도</h3>
          <p className="summary-value">{Number(reviewSummary?.mannerTemperature ?? 36.5).toFixed(1)}도</p>
          <p className="meta">평균 평점 {reviewSummary?.averageRating ?? 0}점 · 받은 리뷰 {reviewSummary?.reviewCount ?? 0}개</p>
          {reviews.slice(0, 3).map((review) => (
            <p className="meta" key={review.id}>
              {review.rating}점 · {review.content} - {review.reviewerNickname}
            </p>
          ))}
        </div>
        {shouldShowMessage ? <p>{loading ? "요청 처리 중..." : message}</p> : null}
      </div>

      <div className="mypage-layout">
        <aside className="mypage-sidebar card">
          <h2>메뉴</h2>
          <div className="side-menu-list">
            {SIDE_MENUS.map((menu) => (
              <button
                key={menu.key}
                type="button"
                className={`side-menu-btn${selectedMenu === menu.key ? " active" : ""}`}
                onClick={() => setSelectedMenu(menu.key)}
              >
                {menu.label}
              </button>
            ))}
          </div>
        </aside>

        <section className="mypage-content card">
          <h2>{selectedLabel}</h2>
          {renderContent()}
        </section>
      </div>

      {modalMessage ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card signup-alert-modal">
            <h2>알림</h2>
            <p>{modalMessage}</p>
            <div className="actions">
              <button type="button" onClick={() => setModalMessage("")}>확인</button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

export default MePage;
