import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { getAuctionDisplayStatusInfo, getAuctionRemainingTimeInfo, isAuctionTimeExpired } from "../lib/auctionStatus";
import { clearAccessToken } from "../lib/auth";
import { loadCategoryOptions } from "../lib/categories";
import { canCreateAuction, isAdmin } from "../lib/permissions";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const CATEGORY_OPTIONS = [
  { value: "", label: "전체 카테고리" },
  { value: "DIGITAL_APPLIANCE", label: "디지털/가전" },
  { value: "FASHION", label: "패션/잡화" },
  { value: "BEAUTY_HEALTH", label: "뷰티/헬스" },
  { value: "LIVING_INTERIOR", label: "리빙/인테리어" },
  { value: "LUXURY_WATCH", label: "명품/시계" },
  { value: "COLLECTIBLE_GOODS", label: "수집품/굿즈" },
  { value: "SPORTS_LEISURE", label: "스포츠/레저" },
  { value: "BOOK_TICKET_GOODS", label: "도서/티켓/굿즈" },
  { value: "ETC", label: "기타" },
];

const SORT_OPTIONS = [
  { value: "ENDING_SOON", label: "마감 임박순" },
  { value: "LATEST", label: "최신순" },
  { value: "PRICE_ASC", label: "시작가 낮은순" },
  { value: "PRICE_DESC", label: "시작가 높은순" },
];
const AUCTION_SORT_VALUES = new Set(SORT_OPTIONS.map((option) => option.value));

const readAuctionFilters = (searchParams) => {
  const nextSort = searchParams.get("sort") || "ENDING_SOON";
  return {
    keyword: searchParams.get("keyword") || "",
    category: searchParams.get("category") || "",
    sort: AUCTION_SORT_VALUES.has(nextSort) ? nextSort : "ENDING_SOON",
  };
};

const buildAuctionSearchParams = ({ keyword, category, sort }) => {
  const params = new URLSearchParams();
  const normalizedKeyword = (keyword || "").trim().replace(/\s+/g, " ");
  if (normalizedKeyword) {
    params.set("keyword", normalizedKeyword);
  }
  if (category) {
    params.set("category", category);
  }
  if (sort && sort !== "ENDING_SOON") {
    params.set("sort", sort);
  }
  return params;
};

function AuctionProductsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialFilters = readAuctionFilters(searchParams);
  const [auctions, setAuctions] = useState([]);
  const [keyword, setKeyword] = useState(initialFilters.keyword);
  const [category, setCategory] = useState(initialFilters.category);
  const [categoryOptions, setCategoryOptions] = useState(CATEGORY_OPTIONS);
  const [sort, setSort] = useState(initialFilters.sort);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("경매 목록을 불러오는 중...");
  const [me, setMe] = useState(null);
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

  const loadAuctions = useCallback(async (filters) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filters.keyword.trim()) {
        params.set("keyword", filters.keyword.trim());
      }
      if (filters.category) {
        params.set("category", filters.category);
      }
      params.set("sort", filters.sort);
      const response = await api(`/api/auctions?${params.toString()}`);
      setAuctions(response);
      setMessage("경매 목록 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`경매 목록 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  useEffect(() => {
    api("/api/auth/me")
      .then(setMe)
      .catch(() => setMe(null));
    loadCategoryOptions({ includeAll: true })
      .then(setCategoryOptions)
      .catch((error) => setMessage(`카테고리 조회 실패: ${error.message}`));
  }, []);

  useEffect(() => {
    const nextFilters = readAuctionFilters(searchParams);
    setKeyword(nextFilters.keyword);
    setCategory(nextFilters.category);
    setSort(nextFilters.sort);
    loadAuctions(nextFilters);
  }, [loadAuctions, searchParams]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 60000);
    return () => window.clearInterval(timer);
  }, []);
  const shouldShowMessage = loading || isAdmin(me) || message.includes("실패") || message.includes("불러오는 중");

  return (
    <main className="container">
      <h1>경매상품목록</h1>

      <div className="card">
        <div className="filter-bar">
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                setSearchParams(buildAuctionSearchParams({ keyword, category, sort }));
              }
            }}
            placeholder="경매명, 설명, 판매자 검색"
            disabled={loading}
          />
          <select className="select" value={category} onChange={(e) => setCategory(e.target.value)} disabled={loading}>
            {categoryOptions.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
          <select className="select" value={sort} onChange={(e) => setSort(e.target.value)} disabled={loading}>
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
        <div className="actions">
          <button type="button" onClick={() => setSearchParams(buildAuctionSearchParams({ keyword, category, sort }))} disabled={loading}>검색</button>
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              setKeyword("");
              setCategory("");
              setSort("ENDING_SOON");
              setSearchParams({});
            }}
            disabled={loading}
          >
            초기화
          </button>
          {canCreateAuction(me) ? (
            <button type="button" onClick={() => navigate("/auctions/manage")} disabled={loading}>경매 등록하기</button>
          ) : null}
        </div>
        {shouldShowMessage ? (
          <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
            {loading ? "요청 처리 중..." : message}
          </p>
        ) : null}

        {auctions.length === 0 ? (
          <p className="empty-box">표시할 경매가 없습니다.</p>
        ) : (
          <ul className="list">
            {auctions.map((auction) => {
              const remainingTime = getAuctionRemainingTimeInfo(auction, now);
              const displayStatus = getAuctionDisplayStatusInfo(auction, now);
              const visuallyClosed = auction.status === "CLOSED" || isAuctionTimeExpired(auction, now);
              return (
              <li key={auction.id} className={`list-item${visuallyClosed ? " sold" : ""}`}>
                {auction.thumbnailUrl ? (
                  <Link to={`/auctions/${auction.id}`} className="product-list-thumb-link" aria-label={`${auction.productTitle} 상세보기`}>
                    <img className="product-list-thumb" src={imageSrc(auction.thumbnailUrl)} alt={auction.productTitle} />
                  </Link>
                ) : null}
                <Link to={`/auctions/${auction.id}`} className="list-title">
                  {auction.productTitle}
                </Link>
                <span>
                  상태: <span className={`status-badge ${displayStatus.tone}`}>{displayStatus.label}</span>
                </span>
                <span>
                  남은 시간: <span className={`auction-time-badge ${remainingTime.tone}`}>{remainingTime.label}</span>
                </span>
                <span className="meta">카테고리: {auction.categoryLabel}</span>
                <span className="meta">
                  판매자: <Link to={`/users/${auction.sellerId}`}>{auction.sellerNickname}</Link>
                </span>
                <span>현재 최고가: {auction.currentHighestBid.toLocaleString()}원</span>
                <span className="meta">
                  {auction.status === "CLOSED" ? "마감(KST)" : "종료(KST)"}: {formatKst(auction.status === "CLOSED" ? auction.closedAt : auction.endAt)}
                </span>
                {visuallyClosed ? <span className="sold-badge">{auction.status === "CLOSED" ? "경매마감" : "마감 확인중"}</span> : null}
              </li>
              );
            })}
          </ul>
        )}
      </div>
    </main>
  );
}

export default AuctionProductsPage;
