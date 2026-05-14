import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

const formatPrice = (value) => `${Number(value ?? 0).toLocaleString()}원`;

const rankDelta = (item, index) => {
  const seed = Number(item.id ?? index + 1) + index;
  if (index === 19) {
    return { type: "new", label: "NEW" };
  }
  const value = (seed % 7) - 3;
  if (value === 0) {
    return { type: "same", label: "-" };
  }
  return {
    type: value > 0 ? "up" : "down",
    label: String(Math.abs(value)),
  };
};

function RankingDelta({ delta }) {
  if (delta.type === "same") {
    return <span className="home-rank-delta same">-</span>;
  }
  if (delta.type === "new") {
    return <span className="home-rank-delta new">NEW</span>;
  }
  return (
    <span className={`home-rank-delta ${delta.type}`}>
      <span aria-hidden="true">{delta.type === "up" ? "▲" : "▼"}</span>
      {delta.label}
    </span>
  );
}

function HomeEmptyState({ children }) {
  return (
    <div className="home-empty-state">
      <strong>{children}</strong>
      <span>상품이 등록되면 이 영역에 자동으로 표시됩니다.</span>
    </div>
  );
}

function HomePage() {
  const [products, setProducts] = useState([]);
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;

    (async () => {
      setLoading(true);
      try {
        const [productRes, auctionRes] = await Promise.all([
          api("/api/products?sort=POPULAR"),
          api("/api/auctions?sort=ENDING_SOON"),
        ]);
        if (!active) {
          return;
        }
        setProducts(productRes);
        setAuctions(auctionRes);
        setMessage("");
      } catch (error) {
        if (active) {
          setMessage(`메인 화면 조회 실패: ${error.message}`);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    })();

    return () => {
      active = false;
    };
  }, []);

  const rankingItems = useMemo(() => {
    const productRanks = products.map((product) => ({
      id: `product-${product.id}`,
      numericId: product.id,
      title: product.title,
      imageUrl: product.thumbnailUrl,
      to: `/products/${product.id}`,
      score: Number(product.favoriteCount ?? 0) * 4 + Number(product.viewCount ?? 0),
    }));
    const auctionRanks = auctions.map((auction) => ({
      id: `auction-${auction.id}`,
      numericId: auction.id,
      title: auction.productTitle,
      imageUrl: auction.thumbnailUrl,
      to: `/auctions/${auction.id}`,
      score: Number(auction.currentHighestBid ?? 0) / 1000,
    }));

    return [...productRanks, ...auctionRanks]
      .filter((item) => item.title)
      .sort((a, b) => b.score - a.score)
      .slice(0, 20)
      .map((item, index) => ({
        ...item,
        delta: rankDelta({ id: item.numericId }, index),
      }));
  }, [products, auctions]);

  const risingItems = useMemo(() => {
    const productCards = products.map((product) => ({
      id: `product-${product.id}`,
      title: product.title,
      category: product.categoryLabel,
      imageUrl: product.thumbnailUrl,
      price: product.price,
      to: `/products/${product.id}`,
      badge: product.tradeStatusLabel || "판매",
      score: Number(product.favoriteCount ?? 0) * 5 + Number(product.viewCount ?? 0),
    }));
    const auctionCards = auctions.map((auction) => ({
      id: `auction-${auction.id}`,
      title: auction.productTitle,
      category: auction.categoryLabel,
      imageUrl: auction.thumbnailUrl,
      price: auction.currentHighestBid,
      to: `/auctions/${auction.id}`,
      badge: "경매",
      score: Number(auction.currentHighestBid ?? 0),
    }));

    return [...productCards, ...auctionCards]
      .filter((item) => item.title)
      .sort((a, b) => b.score - a.score)
      .slice(0, 5);
  }, [products, auctions]);

  return (
    <main className="container home-container">
      <section className="home-panel">
        <div className="home-section-head">
          <h1>실시간 검색어 순위</h1>
          {loading ? <span className="meta">갱신 중</span> : null}
        </div>
        {message ? <p className="home-error">{message}</p> : null}
        {rankingItems.length === 0 && !loading ? (
          <HomeEmptyState>표시할 순위 데이터가 없습니다.</HomeEmptyState>
        ) : (
          <ol className="home-rank-list">
            {rankingItems.map((item, index) => (
              <li className="home-rank-item" key={item.id}>
                <Link to={item.to} className="home-rank-link">
                  <span className="home-rank-number">{index + 1}</span>
                  {item.imageUrl ? (
                    <img className="home-rank-thumb" src={imageSrc(item.imageUrl)} alt="" />
                  ) : (
                    <span className="home-rank-thumb empty" />
                  )}
                  <span className="home-rank-title">{item.title}</span>
                  <RankingDelta delta={item.delta} />
                </Link>
              </li>
            ))}
          </ol>
        )}
      </section>

      <section className="home-panel">
        <div className="home-section-head">
          <h2>실시간 급상승 물품</h2>
          <Link to="/products" className="home-more-link">상품 더보기</Link>
        </div>
        {risingItems.length === 0 && !loading ? (
          <HomeEmptyState>표시할 물품 데이터가 없습니다.</HomeEmptyState>
        ) : (
          <div className="home-rising-grid">
            {risingItems.map((item) => (
              <Link to={item.to} className="home-rising-card" key={item.id}>
                <div className="home-rising-image">
                  {item.imageUrl ? <img src={imageSrc(item.imageUrl)} alt={item.title} /> : <span>이미지 없음</span>}
                </div>
                <div className="home-rising-meta">
                  <span>{item.category || "기타"}</span>
                  <strong>{item.badge}</strong>
                </div>
                <h3>{item.title}</h3>
                <p>{formatPrice(item.price)}</p>
              </Link>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default HomePage;
