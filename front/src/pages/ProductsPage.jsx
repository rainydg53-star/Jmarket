import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { loadCategoryOptions } from "../lib/categories";
import { API_BASE_URL } from "../lib/config";
import { canCreateProduct, isAdmin } from "../lib/permissions";

import "../css/pages/ProductsPage.css";
const RECENT_SEARCH_KEY = "jmarket:recent-product-searches";

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
  { value: "LATEST", label: "최신순" },
  { value: "PRICE_ASC", label: "가격 낮은순" },
  { value: "PRICE_DESC", label: "가격 높은순" },
  { value: "POPULAR", label: "관심 많은순" },
];
const PRODUCT_SORT_VALUES = new Set(SORT_OPTIONS.map((option) => option.value));

const sellerTrustLabel = (product) => {
  const reviewCount = Number(product.sellerReviewCount || 0);
  if (reviewCount === 0) {
    return "후기 없음 · 매너 36.5도";
  }
  return `평점 ${Number(product.sellerAverageRating || 0).toFixed(1)} (${reviewCount}개) · 매너 ${Number(product.sellerMannerTemperature || 36.5).toFixed(1)}도`;
};

const normalizeSearchTerm = (value) => value.trim().replace(/\s+/g, " ");

const readProductFilters = (searchParams) => {
  const nextSort = searchParams.get("sort") || "LATEST";
  return {
    keyword: searchParams.get("keyword") || "",
    category: searchParams.get("category") || "",
    sort: PRODUCT_SORT_VALUES.has(nextSort) ? nextSort : "LATEST",
  };
};

const buildProductSearchParams = ({ keyword, category, sort }) => {
  const params = new URLSearchParams();
  const normalizedKeyword = normalizeSearchTerm(keyword || "");
  if (normalizedKeyword) {
    params.set("keyword", normalizedKeyword);
  }
  if (category) {
    params.set("category", category);
  }
  if (sort && sort !== "LATEST") {
    params.set("sort", sort);
  }
  return params;
};

const loadRecentSearches = () => {
  try {
    return JSON.parse(localStorage.getItem(RECENT_SEARCH_KEY) || "[]");
  } catch {
    return [];
  }
};

const saveRecentSearch = (term) => {
  const normalized = normalizeSearchTerm(term);
  if (!normalized) {
    return loadRecentSearches();
  }
  const next = [
    normalized,
    ...loadRecentSearches().filter((item) => item.toLowerCase() !== normalized.toLowerCase()),
  ].slice(0, 6);
  localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(next));
  return next;
};

function ProductsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialFilters = readProductFilters(searchParams);
  const [products, setProducts] = useState([]);
  const [keyword, setKeyword] = useState(initialFilters.keyword);
  const [category, setCategory] = useState(initialFilters.category);
  const [categoryOptions, setCategoryOptions] = useState(CATEGORY_OPTIONS);
  const [sort, setSort] = useState(initialFilters.sort);
  const [recentSearches, setRecentSearches] = useState(() => loadRecentSearches());
  const [searchFocused, setSearchFocused] = useState(false);
  const [message, setMessage] = useState("상품 목록을 불러오는 중...");
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const loadProducts = useCallback(async (filters) => {
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
      const response = await api(`/api/products?${params.toString()}`);
      setProducts(response);
      setMessage("상품 목록 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 목록 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  const executeSearch = (filters = { keyword, category, sort }) => {
    const normalizedKeyword = normalizeSearchTerm(filters.keyword);
    if (normalizedKeyword) {
      setRecentSearches(saveRecentSearch(normalizedKeyword));
    }
    setSearchParams(buildProductSearchParams({ ...filters, keyword: normalizedKeyword }));
  };

  useEffect(() => {
    api("/api/auth/me")
      .then(setMe)
      .catch(() => setMe(null));
    loadCategoryOptions({ includeAll: true })
      .then(setCategoryOptions)
      .catch((error) => setMessage(`카테고리 조회 실패: ${error.message}`));
  }, []);

  useEffect(() => {
    const nextFilters = readProductFilters(searchParams);
    setKeyword(nextFilters.keyword);
    setCategory(nextFilters.category);
    setSort(nextFilters.sort);
    loadProducts(nextFilters);
  }, [loadProducts, searchParams]);

  const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

  const autocompleteSuggestions = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    if (!normalizedKeyword) {
      return [];
    }
    const candidates = products.flatMap((product) => [
      product.title,
      product.sellerNickname,
      product.categoryLabel,
    ].filter(Boolean));
    return [...new Set(candidates)]
      .filter((item) => item.toLowerCase().includes(normalizedKeyword))
      .slice(0, 6);
  }, [keyword, products]);

  const popularKeywords = useMemo(() => (
    [...products]
      .sort((a, b) => ((b.favoriteCount ?? 0) * 3 + (b.viewCount ?? 0)) - ((a.favoriteCount ?? 0) * 3 + (a.viewCount ?? 0)))
      .map((product) => product.title)
      .filter(Boolean)
      .slice(0, 6)
  ), [products]);

  const applyKeyword = (nextKeyword) => {
    setKeyword(nextKeyword);
    setSearchFocused(false);
    executeSearch({ keyword: nextKeyword, category, sort });
  };
  const shouldShowMessage = loading || isAdmin(me) || message.includes("실패") || message.includes("불러오는 중");

  return (
    <main className="container">
      <h1>상품목록</h1>

      <div className="card">
        <div className="filter-bar">
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onFocus={() => setSearchFocused(true)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                executeSearch();
                setSearchFocused(false);
              }
            }}
            placeholder="상품명, 설명, 판매자 검색"
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
        {(searchFocused || keyword || recentSearches.length > 0 || popularKeywords.length > 0) ? (
          <div className="search-assist-panel">
            {autocompleteSuggestions.length > 0 ? (
              <div className="search-assist-section">
                <span className="meta">자동완성</span>
                <div className="search-chip-row">
                  {autocompleteSuggestions.map((item) => (
                    <button type="button" className="search-chip" key={`suggest-${item}`} onClick={() => applyKeyword(item)}>
                      {item}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
            {recentSearches.length > 0 ? (
              <div className="search-assist-section">
                <span className="meta">최근검색어</span>
                <div className="search-chip-row">
                  {recentSearches.map((item) => (
                    <button type="button" className="search-chip" key={`recent-${item}`} onClick={() => applyKeyword(item)}>
                      {item}
                    </button>
                  ))}
                  <button
                    type="button"
                    className="search-chip muted"
                    onClick={() => {
                      localStorage.removeItem(RECENT_SEARCH_KEY);
                      setRecentSearches([]);
                    }}
                  >
                    전체삭제
                  </button>
                </div>
              </div>
            ) : null}
            {popularKeywords.length > 0 ? (
              <div className="search-assist-section">
                <span className="meta">인기검색어</span>
                <div className="search-chip-row">
                  {popularKeywords.map((item) => (
                    <button type="button" className="search-chip" key={`popular-${item}`} onClick={() => applyKeyword(item)}>
                      {item}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}
        <div className="actions">
          <button type="button" onClick={() => executeSearch()} disabled={loading}>검색</button>
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              setKeyword("");
              setCategory("");
              setSort("LATEST");
              setSearchParams({});
            }}
            disabled={loading}
          >
            초기화
          </button>
          {canCreateProduct(me) ? (
            <button type="button" onClick={() => navigate("/products/new")}>상품 등록하기</button>
          ) : null}
        </div>
        {shouldShowMessage ? (
          <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
            {loading ? "요청 처리 중..." : message}
          </p>
        ) : null}
        {products.length === 0 ? (
          <p className="empty-box">등록된 상품이 없습니다.</p>
        ) : (
          <ul className="list">
            {products.map((product) => {
              const isUnavailable = product.tradeStatus === "RESERVED" || product.tradeStatus === "COMPLETED" || product.sold;
              return (
              <li key={product.id} className={`list-item${isUnavailable ? " sold" : ""}`}>
                {product.thumbnailUrl ? (
                  <Link to={`/products/${product.id}`} className="product-list-thumb-link" aria-label={`${product.title} 상세보기`}>
                    <img className="product-list-thumb" src={imageSrc(product.thumbnailUrl)} alt={product.title} />
                  </Link>
                ) : null}
                <Link to={`/products/${product.id}`} className="list-title">
                  {product.title}
                </Link>
                <span>{product.price.toLocaleString()}원</span>
                <span className="meta">카테고리: {product.categoryLabel}</span>
                <span className="meta">조회 {product.viewCount.toLocaleString()} · 찜 {product.favoriteCount.toLocaleString()}</span>
                <span className="meta">
                  판매자: <Link to={`/users/${product.sellerId}`}>{product.sellerNickname}</Link> · {sellerTrustLabel(product)}
                </span>
                {product.tradeStatusLabel ? <span className="sold-badge">{product.tradeStatusLabel}</span> : null}
              </li>
            );
            })}
          </ul>
        )}
      </div>
    </main>
  );
}

export default ProductsPage;
