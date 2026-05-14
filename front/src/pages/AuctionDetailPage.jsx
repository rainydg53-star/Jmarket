import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ImageViewerModal from "../components/ImageViewerModal";
import { api } from "../lib/api";
import { getAuctionDisplayStatusInfo, getAuctionRemainingTimeInfo, isAuctionTimeExpired, isAuctionWaitingToStart } from "../lib/auctionStatus";
import { clearAccessToken, getAccessToken } from "../lib/auth";
import { openChatWindow } from "../lib/chatWindow";
import { API_BASE_URL } from "../lib/config";
import { canBidAuction, canUseUserActions } from "../lib/permissions";
import { parseRestrictionMessage } from "../lib/restriction";

const formatNumber = (value) => Number(value ?? 0).toLocaleString();

function AuctionBidTrendChart({ bids, startPrice, formatKst }) {
  const [hoveredPoint, setHoveredPoint] = useState(null);
  const chartBids = useMemo(
    () => [...bids].sort((a, b) => new Date(a.bidAt).getTime() - new Date(b.bidAt).getTime()),
    [bids]
  );
  const labels = {
    title: "입찰가 추이",
    emptyMeta: "입찰이 시작되면 가격 흐름이 그래프로 표시됩니다.",
    empty: "아직 입찰 데이터가 없습니다.",
    meta: "시간 순서대로 최고 입찰가 흐름을 보여줍니다.",
    aria: "입찰가 추이 그래프",
    countPrefix: "총",
    countSuffix: "회",
    won: "원",
    firstBid: "첫 입찰",
    latestBid: "최근 입찰",
  };
  const values = chartBids.map((bid) => Number(bid.amount ?? 0));
  const minValue = Math.min(Number(startPrice ?? 0), ...values);
  const maxValue = Math.max(Number(startPrice ?? 0), ...values, 1);
  const range = Math.max(1, maxValue - minValue);
  const width = 640;
  const height = 220;
  const padding = 32;
  const plotWidth = width - padding * 2;
  const plotHeight = height - padding * 2;

  const points = chartBids.map((bid, index) => {
    const x = chartBids.length === 1
      ? width / 2
      : padding + (index / (chartBids.length - 1)) * plotWidth;
    const y = padding + ((maxValue - Number(bid.amount ?? 0)) / range) * plotHeight;
    return { ...bid, x, y };
  });

  if (chartBids.length === 0) {
    return (
      <div className="card auction-chart-card">
        <div className="auction-chart-head">
          <div>
            <h2>{labels.title}</h2>
            <p className="meta">{labels.emptyMeta}</p>
          </div>
        </div>
        <div className="auction-chart-empty">{labels.empty}</div>
      </div>
    );
  }

  const path = points.map((point) => `${point.x},${point.y}`).join(" ");
  const firstBid = chartBids[0];
  const lastBid = chartBids[chartBids.length - 1];
  const activePoint = hoveredPoint ?? points[points.length - 1];

  return (
    <div className="card auction-chart-card">
      <div className="auction-chart-head">
        <div>
          <h2>{labels.title}</h2>
          <p className="meta">{labels.meta}</p>
        </div>
        <div className="auction-chart-summary">
          <span>{labels.countPrefix} {formatNumber(chartBids.length)}{labels.countSuffix}</span>
          <strong>{formatNumber(lastBid.amount)}{labels.won}</strong>
        </div>
      </div>
      <div className="auction-chart-canvas" aria-label={labels.aria}>
        <svg viewBox={`0 0 ${width} ${height}`} role="img">
          <line x1={padding} y1={padding} x2={padding} y2={height - padding} className="auction-chart-axis" />
          <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} className="auction-chart-axis" />
          <text x={padding} y={padding - 10} className="auction-chart-label">{formatNumber(maxValue)}{labels.won}</text>
          <text x={padding} y={height - 8} className="auction-chart-label">{formatNumber(minValue)}{labels.won}</text>
          <polyline points={path} className="auction-chart-line flow" />
          {activePoint ? (
            <>
              <line x1={activePoint.x} y1={padding} x2={activePoint.x} y2={height - padding} className="auction-chart-guide" />
              <foreignObject
                x={Math.min(Math.max(activePoint.x - 88, padding), width - padding - 176)}
                y={Math.max(activePoint.y - 74, padding)}
                width="176"
                height="64"
              >
                <div className="auction-chart-tooltip">
                  <strong>{formatNumber(activePoint.amount)}{labels.won}</strong>
                  <span>{activePoint.bidderNickname}</span>
                  <small>{formatKst(activePoint.bidAt)}</small>
                </div>
              </foreignObject>
            </>
          ) : null}
          {points.map((point, index) => (
            <circle
              key={point.id}
              cx={point.x}
              cy={point.y}
              r={hoveredPoint?.id === point.id ? 7 : 5}
              className="auction-chart-dot"
              style={{ animationDelay: `${index * 90}ms` }}
              onMouseEnter={() => setHoveredPoint(point)}
              onMouseLeave={() => setHoveredPoint(null)}
            />
          ))}
        </svg>
      </div>
      <div className="auction-chart-foot">
        <span>{labels.firstBid}: {formatNumber(firstBid.amount)}{labels.won} - {formatKst(firstBid.bidAt)}</span>
        <span>{labels.latestBid}: {lastBid.bidderNickname} - {formatKst(lastBid.bidAt)}</span>
      </div>
    </div>
  );
}

function getBidFailureMessage(error, { auction, minimumBid }) {
  const currentPrice = formatNumber(auction?.currentHighestBid ?? auction?.startPrice);
  const minimumBidText = formatNumber(minimumBid);

  switch (error.code) {
    case "U004":
      return "입찰할 수 없는 경매입니다. 이미 마감되었거나 아직 입찰 시간이 아닐 수 있습니다. 경매 상태와 종료 시간을 확인해주세요.";
    case "U005":
      return `입찰 금액이 낮습니다. 현재 최고가는 ${currentPrice}원이고, 최소 입찰가는 ${minimumBidText}원입니다. 금액을 올려 다시 시도해주세요.`;
    case "U006":
      return "본인이 등록한 경매 상품에는 입찰할 수 없습니다. 판매자는 입찰 대신 경매 상태와 입찰 내역만 확인할 수 있습니다.";
    case "U008":
      return "이미 이 경매의 최고 입찰자입니다. 다른 사용자가 더 높은 금액으로 입찰한 뒤에 다시 입찰할 수 있습니다.";
    case "M002":
      return `사용 가능한 마일리지가 부족합니다. 최소 ${minimumBidText}원 이상 입찰하려면 마일리지를 충전한 뒤 다시 시도해주세요.`;
    case "A007":
      return "현재 계정에 입찰 제한이 적용되어 있습니다. 제한 사유와 해제 시간을 확인해주세요.";
    case "S001":
      return "로그인이 필요합니다. 다시 로그인한 뒤 입찰해주세요.";
    case "S002":
      return "입찰 권한이 없습니다. 계정 상태를 확인해주세요.";
    default:
      return `입찰을 처리하지 못했습니다. ${error.message ?? "잠시 후 다시 시도해주세요."}`;
  }
}

function AuctionDetailPage() {
  const { auctionId } = useParams();
  const navigate = useNavigate();
  const clientRef = useRef(null);
  const [me, setMe] = useState(null);
  const [auction, setAuction] = useState(null);
  const [bids, setBids] = useState([]);
  const [bidAmount, setBidAmount] = useState("");
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [viewerOpen, setViewerOpen] = useState(false);
  const [reviewDraft, setReviewDraft] = useState({ rating: 5, content: "" });
  const [restrictionModal, setRestrictionModal] = useState(null);
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [message, setMessage] = useState("경매 정보를 불러오는 중입니다...");
  const [now, setNow] = useState(Date.now());

  const formatKst = (isoString) => {
    if (!isoString) {
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
    }).format(new Date(isoString));
  };

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

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

  const loadAuction = useCallback(async () => {
    try {
      const response = await api(`/api/auctions/${auctionId}`);
      setAuction(response);
      setActiveImageIndex(0);
      setMessage("");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`경매 상세 조회 실패: ${error.message}`);
    }
  }, [auctionId, handleUnauthorized]);

  const loadBids = useCallback(async () => {
    try {
      const response = await api(`/api/auctions/${auctionId}/bids`);
      setBids(response);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
      }
    }
  }, [auctionId, handleUnauthorized]);

  const placeBidWithAmount = async (forcedAmount) => {
    if (!auction) {
      setMessage("경매 정보를 아직 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
      return;
    }
    if (auction.status !== "OPEN" || isAuctionTimeExpired(auction) || isAuctionWaitingToStart(auction)) {
      setMessage(getBidFailureMessage({ code: "U004" }, { auction, minimumBid }));
      await loadAuction();
      await loadBids();
      return;
    }
    if (isSeller) {
      setMessage(getBidFailureMessage({ code: "U006" }, { auction, minimumBid }));
      return;
    }
    if (isTopBidder) {
      setMessage(getBidFailureMessage({ code: "U008" }, { auction, minimumBid }));
      return;
    }

    let amountToSend = null;
    if (forcedAmount !== null) {
      amountToSend = forcedAmount;
    } else {
      const trimmed = bidAmount.trim();
      if (trimmed) {
        const amount = Number(trimmed);
        if (!Number.isFinite(amount) || amount < 0) {
          setMessage("입찰 금액은 0 이상의 숫자로 입력해주세요.");
          return;
        }
        if (amount < minimumBid) {
          setMessage(getBidFailureMessage({ code: "U005" }, { auction, minimumBid }));
          return;
        }
        amountToSend = amount;
      }
    }

    if (amountToSend !== null) {
      const amount = Number(amountToSend);
      if (!Number.isFinite(amount) || amount < 0) {
        setMessage("입찰 금액은 0 이상의 숫자로 입력해주세요.");
        return;
      }
    }

    setLoading(true);
    try {
      await api(`/api/auctions/${auctionId}/bids`, {
        method: "POST",
        body: JSON.stringify({ amount: amountToSend }),
      });
      setMessage("입찰이 완료되었습니다.");
      setBidAmount("");
      await loadAuction();
      await loadBids();
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      if (error.code === "A007") {
        setRestrictionModal(parseRestrictionMessage(error.message));
        setMessage(getBidFailureMessage(error, { auction, minimumBid }));
        return;
      }
      setMessage(getBidFailureMessage(error, { auction, minimumBid }));
      if (["U004", "U005", "U008"].includes(error.code)) {
        await loadAuction();
        await loadBids();
      }
    } finally {
      setLoading(false);
    }
  };

  const placeBid = async () => {
    await placeBidWithAmount(null);
  };

  const openAuctionChatRoom = async (counterpartyUserId) => {
    setLoading(true);
    try {
      const room = await api("/api/chat/rooms/auction", {
        method: "POST",
        body: JSON.stringify({
          auctionId: Number(auctionId),
          counterpartyUserId,
        }),
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

  const submitAuctionReview = async () => {
    if (!auction || !me) {
      return;
    }
    const targetUserId = me.id === auction.sellerId ? auction.winnerUserId : auction.sellerId;
    if (!targetUserId) {
      setMessage("후기를 남길 대상이 없습니다.");
      return;
    }
    if (!reviewDraft.content.trim()) {
      setMessage("후기 내용을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api("/api/reviews", {
        method: "POST",
        body: JSON.stringify({
          targetUserId,
          sourceType: "AUCTION",
          sourceId: Number(auctionId),
          rating: Number(reviewDraft.rating),
          content: reviewDraft.content.trim(),
        }),
      });
      setReviewDraft({ rating: 5, content: "" });
      setMessage("경매 후기가 등록되었습니다.");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`경매 후기 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMe();
    loadAuction();
    loadBids();
  }, [loadAuction, loadBids, loadMe]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 60000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      return undefined;
    }

    const wsBaseUrl = API_BASE_URL.replace(/^http/, "ws");
    const client = new Client({
      brokerURL: `${wsBaseUrl}/ws-chat`,
      reconnectDelay: 3000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/auctions.${auctionId}`, (frame) => {
          const payload = JSON.parse(frame.body);
          if (payload.type !== "BID_PLACED") {
            return;
          }
          setAuction(payload.auction);
          setBids((prev) => {
            if (prev.some((bid) => bid.id === payload.bid.id)) {
              return prev;
            }
            return [...prev, payload.bid];
          });
          setMessage("새 입찰이 반영됐습니다.");
        });
      },
      onStompError: (frame) => {
        setMessage(`STOMP 에러: ${frame.headers?.message ?? "unknown error"}`);
      },
      onWebSocketClose: () => {
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [auctionId]);

  const isSeller = Boolean(me && auction && me.id === auction.sellerId);
  const canActAsUser = canUseUserActions(me);
  const canBidAsUser = canBidAuction(me);
  const isWinner = Boolean(me && auction && auction.winnerUserId && me.id === auction.winnerUserId);
  const isTopBidder = Boolean(me && auction && auction.currentHighestBidderId && me.id === auction.currentHighestBidderId);
  const timeExpired = isAuctionTimeExpired(auction, now);
  const waitingToStart = isAuctionWaitingToStart(auction, now);
  const isOpen = auction?.status === "OPEN";
  const canBidNow = isOpen && !waitingToStart && !timeExpired && canBidAsUser;
  const isClosed = auction?.status === "CLOSED";
  const hasWinner = Boolean(auction?.winnerUserId);
  const canReviewAuction = Boolean(
    me
    && auction
    && auction.status === "CLOSED"
    && auction.winnerUserId
    && (me.id === auction.sellerId || me.id === auction.winnerUserId)
  );
  const minimumBid = useMemo(() => {
    if (!auction) {
      return 0;
    }
    const current = auction.currentHighestBid ?? auction.startPrice;
    return Math.ceil(current * 1.1);
  }, [auction]);
  const settlementStatus = !isClosed
    ? "정산 대기"
    : hasWinner
      ? "정산 완료"
      : "정산 없음";
  const reviewStatus = !isClosed
    ? "경매 종료 후 후기 작성 가능"
    : canReviewAuction
      ? "후기 작성 가능"
      : hasWinner
        ? "낙찰자와 판매자만 후기 작성 가능"
        : "낙찰자가 없어 후기 불가";
  const counterpartyUserId = isSeller ? auction?.winnerUserId : auction?.sellerId;
  const counterpartyLabel = isSeller ? auction?.winnerNickname : auction?.sellerNickname;
  const visibleImages = auction?.images?.filter((image) => image.imageUrl) || [];
  const activeImage = visibleImages[activeImageIndex] || visibleImages[0];
  const viewerImages = visibleImages.map((image, index) => ({
    src: imageSrc(image.imageUrl),
    alt: `${auction?.productTitle ?? "경매"} ${index + 1}`,
  }));
  const remainingTime = getAuctionRemainingTimeInfo(auction, now);
  const displayStatus = getAuctionDisplayStatusInfo(auction, now);
  const shouldShowMessage = loading || Boolean(message);
  const auctionTimerId = auction?.id;
  const auctionTimerStatus = auction?.status;
  const auctionTimerStartAt = auction?.startAt;
  const auctionTimerEndAt = auction?.endAt;

  useEffect(() => {
    if (!auctionTimerId || auctionTimerStatus !== "OPEN") {
      return undefined;
    }
    const startTime = auctionTimerStartAt ? new Date(auctionTimerStartAt).getTime() : NaN;
    const endTime = auctionTimerEndAt ? new Date(auctionTimerEndAt).getTime() : NaN;
    const currentTime = Date.now();
    const targetTime = Number.isFinite(startTime) && startTime > currentTime ? startTime : endTime;
    if (!Number.isFinite(targetTime)) {
      return undefined;
    }
    const delay = Math.max(0, targetTime - currentTime + 1000);
    const timer = window.setTimeout(() => {
      setMessage(targetTime === startTime ? "" : "경매 마감 시간이 지나 상태를 확인하는 중입니다.");
      loadAuction();
      loadBids();
    }, delay);
    return () => window.clearTimeout(timer);
  }, [auctionTimerId, auctionTimerStatus, auctionTimerStartAt, auctionTimerEndAt, loadAuction, loadBids]);

  const detailLabels = {
    pageTitle: "경매 상세",
    backToList: "경매 목록으로",
    realtime: "실시간 연결 상태",
    connected: "연결됨",
    disconnected: "연결 대기 중",
    loading: "처리 중...",
    prev: "이전",
    next: "다음",
    category: "카테고리",
    seller: "판매자",
    status: "상태",
    startPrice: "시작가",
    currentHighest: "현재 최고가",
    instantBuy: "즉시구매가",
    notSet: "없음",
    minimumBid: "최소 입찰가(1.1배)",
    topBidder: "최고 입찰자",
    startAt: "시작 시간(KST)",
    endAt: "종료 시간(KST)",
    topBidderNotice: "현재 최고 입찰자입니다. 추가 입찰은 불가합니다.",
    winner: "낙찰자",
    resultDone: "낙찰 완료",
    noWinner: "유찰",
    resultTitle: "경매가 낙찰으로 종료되었습니다.",
    noWinnerTitle: "낙찰자 없이 경매가 종료되었습니다.",
    resultHelp: "낙찰 정보와 정산 상태를 확인한 뒤 거래를 진행해주세요.",
    noWinnerHelp: "입찰자가 없어 정산이 필요하지 않은 상태입니다.",
    winningBid: "낙찰가",
    settlement: "정산 상태",
    review: "후기 상태",
    chat: "채팅하기",
    counterpartyChat: "상대와 채팅",
    writeReview: "후기 작성",
    bidPlaceholder: "입찰가 입력, 최소",
    bid: "입찰",
    instantBuyAction: "즉시구매",
    sellerChat: "판매자 채팅",
    reportSeller: "판매자 신고",
    transactionReview: "거래 후기",
    target: "대상",
    reviewPlaceholder: "경매 거래에 대한 후기를 입력해주세요.",
    submitReview: "후기 등록",
    bidHistory: "입찰 내역",
    noBids: "아직 입찰이 없습니다.",
    bidderChat: "입찰자 채팅",
    restrictedTitle: "이용이 제한된 기능입니다.",
    restrictedBody: "기능을 사용할 수 없는 상태입니다.",
    reason: "사유",
    restrictedUntil: "제한 해제",
    close: "닫기",
    won: "원",
    point: "점",
    pendingSettlement: "정산 대기",
    completedSettlement: "정산 완료",
    noSettlement: "정산 없음",
  };

  return (
    <main className="container">
      <h1>{detailLabels.pageTitle}</h1>
      <div className="card">
        <p className="meta">
          <Link to="/auctions">{detailLabels.backToList}</Link>
        </p>
        <p className="meta">
          {detailLabels.realtime}: {connected ? detailLabels.connected : detailLabels.disconnected}
        </p>
        {shouldShowMessage ? <p>{loading ? detailLabels.loading : message}</p> : null}
      </div>

      {auction ? (
        <div className="card">
          {visibleImages.length > 0 ? (
            <div className="product-gallery">
              <div className="gallery-main">
                <button
                  type="button"
                  className="gallery-nav"
                  onClick={() => setActiveImageIndex((prev) => (prev - 1 + visibleImages.length) % visibleImages.length)}
                  disabled={visibleImages.length < 2}
                >
                  {detailLabels.prev}
                </button>
                <button
                  type="button"
                  className="gallery-main-image-button"
                  onClick={() => setViewerOpen(true)}
                  aria-label="경매 이미지 크게 보기"
                >
                  <img src={imageSrc(activeImage.imageUrl)} alt={auction.productTitle} />
                </button>
                <button
                  type="button"
                  className="gallery-nav"
                  onClick={() => setActiveImageIndex((prev) => (prev + 1) % visibleImages.length)}
                  disabled={visibleImages.length < 2}
                >
                  {detailLabels.next}
                </button>
              </div>
              <div className="gallery-thumbs">
                {visibleImages.map((image, index) => (
                  <button
                    type="button"
                    className={index === activeImageIndex ? "active" : ""}
                    onClick={() => setActiveImageIndex(index)}
                    key={`${image.imageUrl}-${index}`}
                  >
                    <img src={imageSrc(image.imageUrl)} alt={`${auction.productTitle} ${index + 1}`} />
                  </button>
                ))}
              </div>
            </div>
          ) : null}
          <h2>{auction.productTitle}</h2>
          {auction.productDescription ? <p>{auction.productDescription}</p> : null}
          <p className="meta">{detailLabels.category}: {auction.categoryLabel}</p>
          <p className="meta">
            {detailLabels.seller}: <Link to={`/users/${auction.sellerId}`}>{auction.sellerNickname}</Link>
          </p>
          <p className="meta">
            {detailLabels.status}: <span className={`status-badge ${displayStatus.tone}`}>{displayStatus.label}</span>
          </p>
          <p className="meta">
            남은 시간: <span className={`auction-time-badge ${remainingTime.tone}`}>{remainingTime.label}</span>
          </p>
          <p className="meta">{detailLabels.startPrice}: {formatNumber(auction.startPrice)}{detailLabels.won}</p>
          <p className="meta">{detailLabels.currentHighest}: {formatNumber(auction.currentHighestBid)}{detailLabels.won}</p>
          <p className="meta">
            {detailLabels.instantBuy}: {auction.instantBuyPrice ? `${formatNumber(auction.instantBuyPrice)}${detailLabels.won}` : detailLabels.notSet}
          </p>
          <p className="meta">{detailLabels.minimumBid}: {formatNumber(minimumBid)}{detailLabels.won}</p>
          <p className="meta">{detailLabels.topBidder}: {auction.currentHighestBidderNickname ?? "-"}</p>
          <p className="meta">{detailLabels.startAt}: {formatKst(auction.startAt)}</p>
          <p className="meta">{detailLabels.endAt}: {formatKst(auction.endAt)}</p>
          {isTopBidder ? <p className="meta">{detailLabels.topBidderNotice}</p> : null}
          {auction.winnerNickname ? (
            <p className="meta">{detailLabels.winner}: {auction.winnerNickname} ({formatNumber(auction.winningBidAmount)}{detailLabels.won})</p>
          ) : null}

          {isClosed ? (
            <div className={`auction-result-panel${hasWinner ? " settled" : " no-winner"}`}>
              <div>
                <span className="result-badge">{hasWinner ? detailLabels.resultDone : detailLabels.noWinner}</span>
                <h3>{hasWinner ? detailLabels.resultTitle : detailLabels.noWinnerTitle}</h3>
                <p className="meta">{hasWinner ? detailLabels.resultHelp : detailLabels.noWinnerHelp}</p>
              </div>
              <div className="auction-result-grid">
                <div>
                  <span className="meta">{detailLabels.winner}</span>
                  <strong>{auction.winnerNickname ?? "-"}</strong>
                </div>
                <div>
                  <span className="meta">{detailLabels.winningBid}</span>
                  <strong>{auction.winningBidAmount ? `${formatNumber(auction.winningBidAmount)}${detailLabels.won}` : "-"}</strong>
                </div>
                <div>
                  <span className="meta">{detailLabels.settlement}</span>
                  <strong>{settlementStatus}</strong>
                </div>
                <div>
                  <span className="meta">{detailLabels.review}</span>
                  <strong>{reviewStatus}</strong>
                </div>
              </div>
              <div className="actions">
                {counterpartyUserId && (isSeller || isWinner) ? (
                  <button type="button" onClick={() => openAuctionChatRoom(counterpartyUserId)} disabled={loading}>
                    {counterpartyLabel ? `${counterpartyLabel} ${detailLabels.chat}` : detailLabels.counterpartyChat}
                  </button>
                ) : null}
                {canReviewAuction ? (
                  <button type="button" onClick={() => document.getElementById("auction-review-form")?.scrollIntoView({ behavior: "smooth", block: "start" })}>
                    {detailLabels.writeReview}
                  </button>
                ) : null}
              </div>
            </div>
          ) : null}

          {timeExpired && !isClosed ? (
            <div className="auction-result-panel no-winner">
              <div>
                <span className="result-badge">마감 확인중</span>
                <h3>경매 마감 시간이 지났습니다.</h3>
                <p className="meta">자동 마감 처리 결과를 확인하고 있습니다. 잠시 후 낙찰자와 정산 상태가 반영됩니다.</p>
              </div>
            </div>
          ) : null}

          {waitingToStart ? (
            <div className="auction-result-panel">
              <div>
                <span className="result-badge">진행 대기중</span>
                <h3>아직 경매가 시작되지 않았습니다.</h3>
                <p className="meta">시작 시간: {formatKst(auction.startAt)}</p>
              </div>
            </div>
          ) : null}

          {canBidNow ? (
            <div className="actions">
              <input
                type="number"
                min="0"
                value={bidAmount}
                onChange={(e) => setBidAmount(e.target.value)}
                disabled={loading || isSeller || isTopBidder || !canBidNow}
                placeholder={`${detailLabels.bidPlaceholder} ${formatNumber(minimumBid)}${detailLabels.won}`}
              />
              <button onClick={placeBid} disabled={loading || isSeller || isTopBidder || !canBidNow}>{detailLabels.bid}</button>
              {!isSeller && auction.instantBuyPrice ? (
                <button onClick={() => placeBidWithAmount(auction.instantBuyPrice)} disabled={loading || isTopBidder || !canBidNow}>
                  {detailLabels.instantBuyAction}
                </button>
              ) : null}
              {!isSeller && canActAsUser && auction.sellerId ? (
                <button onClick={() => openAuctionChatRoom(auction.sellerId)} disabled={loading}>{detailLabels.sellerChat}</button>
              ) : null}
              {!isSeller && canActAsUser && auction.sellerId ? (
                <button
                  type="button"
                  onClick={() => navigate(`/reports?targetType=USER&targetId=${auction.sellerId}&reason=${encodeURIComponent("경매 판매자 신고")}`)}
                  disabled={loading}
                >
                  {detailLabels.reportSeller}
                </button>
              ) : null}
            </div>
          ) : null}

          {canReviewAuction ? (
            <div className="review-box" id="auction-review-form">
              <h3>{detailLabels.transactionReview}</h3>
              <p className="meta">
                {detailLabels.target}: {me.id === auction.sellerId ? auction.winnerNickname : auction.sellerNickname}
              </p>
              <select
                className="select"
                value={reviewDraft.rating}
                onChange={(e) => setReviewDraft((prev) => ({ ...prev, rating: e.target.value }))}
                disabled={loading}
              >
                {[5, 4, 3, 2, 1].map((rating) => (
                  <option key={rating} value={rating}>{rating}{detailLabels.point}</option>
                ))}
              </select>
              <textarea
                className="textarea"
                value={reviewDraft.content}
                onChange={(e) => setReviewDraft((prev) => ({ ...prev, content: e.target.value }))}
                placeholder={detailLabels.reviewPlaceholder}
                disabled={loading}
              />
              <button type="button" onClick={submitAuctionReview} disabled={loading}>{detailLabels.submitReview}</button>
            </div>
          ) : null}
        </div>
      ) : null}

      {auction ? (
        <AuctionBidTrendChart bids={bids} startPrice={auction.startPrice} formatKst={formatKst} />
      ) : null}

      <div className="card">
        <h2>{detailLabels.bidHistory}</h2>
        {bids.length === 0 ? (
          <p className="meta">{detailLabels.noBids}</p>
        ) : (
          <ul className="list">
            {bids.map((bid) => (
              <li key={bid.id} className="list-item">
                <span>{bid.bidderNickname}</span>
                <span>{formatNumber(bid.amount)}{detailLabels.won}</span>
                <span className="meta">{formatKst(bid.bidAt)}</span>
                {isSeller ? (
                  <div className="actions">
                    <button onClick={() => openAuctionChatRoom(bid.bidderId)} disabled={loading}>{detailLabels.bidderChat}</button>
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      {restrictionModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>{detailLabels.restrictedTitle}</h2>
            <p>{restrictionModal.feature} {detailLabels.restrictedBody}</p>
            <p>{detailLabels.reason}: {restrictionModal.reason}</p>
            {restrictionModal.restrictedUntil ? <p>{detailLabels.restrictedUntil}: {restrictionModal.restrictedUntil}</p> : null}
            <div className="actions">
              <button type="button" onClick={() => setRestrictionModal(null)}>{detailLabels.close}</button>
            </div>
          </div>
        </div>
      ) : null}

      {viewerOpen ? (
        <ImageViewerModal
          images={viewerImages}
          activeIndex={activeImageIndex}
          title={auction?.productTitle ?? "경매 이미지"}
          onChange={setActiveImageIndex}
          onClose={() => setViewerOpen(false)}
        />
      ) : null}
    </main>
  );
}

export default AuctionDetailPage;
