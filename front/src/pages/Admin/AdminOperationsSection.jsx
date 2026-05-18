import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAuctionDisplayStatusInfo, getAuctionRemainingTimeInfo } from "../../lib/auctionStatus";
import { formatDateTime, formatNumber } from "./adminUtils";

export default function AdminOperationsSection({ products, auctions, loading, deleteProduct, cancelAuction, deleteAuction, restoreAuction }) {
  const [auctionVisibility, setAuctionVisibility] = useState("visible");

  const filteredAuctions = useMemo(() => {
    if (auctionVisibility === "hidden") {
      return auctions.filter((auction) => auction.hidden);
    }
    if (auctionVisibility === "all") {
      return auctions;
    }
    return auctions.filter((auction) => !auction.hidden);
  }, [auctions, auctionVisibility]);

  const hiddenCount = auctions.filter((auction) => auction.hidden).length;
  const visibleCount = auctions.length - hiddenCount;

  return (
        <section className="admin-two-column">
          <div className="card">
            <h2>상품 운영</h2>
            <ul className="list admin-list">
              {products.map((product) => (
                <li className="list-item" key={product.id}>
                  <Link to={`/products/${product.id}`} className="admin-title-link">
                    {product.title}
                  </Link>
                  <span className="meta">판매자: {product.sellerNickname} · {product.categoryLabel} · {formatNumber(product.price)}원</span>
                  <span className="meta">{product.listingType} · {product.sold ? "거래완료" : "판매중"} · {formatDateTime(product.createdAt)}</span>
                  <div className="actions compact-actions">
                    <button type="button" className="danger-button" onClick={() => deleteProduct(product)} disabled={loading}>강제 삭제</button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
          <div className="card">
            <div className="admin-section-heading">
              <div>
                <h2>경매 운영</h2>
                <p className="meta">노출중 {visibleCount}건 · 숨김 {hiddenCount}건</p>
              </div>
              <div className="admin-inline-filter" aria-label="경매 노출 상태 필터">
                <button type="button" className={auctionVisibility === "visible" ? "active" : ""} onClick={() => setAuctionVisibility("visible")}>노출중</button>
                <button type="button" className={auctionVisibility === "hidden" ? "active" : ""} onClick={() => setAuctionVisibility("hidden")}>숨김</button>
                <button type="button" className={auctionVisibility === "all" ? "active" : ""} onClick={() => setAuctionVisibility("all")}>전체</button>
              </div>
            </div>
            <ul className="list admin-list">
              {filteredAuctions.map((auction) => {
                const remainingTime = getAuctionRemainingTimeInfo(auction);
                const displayStatus = getAuctionDisplayStatusInfo(auction);
                return (
                <li className="list-item" key={auction.id}>
                  <Link to={`/auctions/${auction.id}`} className="admin-title-link">
                    {auction.productTitle}
                  </Link>
                  <span className="meta">
                    판매자: {auction.sellerNickname} · 상태: <span className={`status-badge ${displayStatus.tone}`}>{displayStatus.label}</span>
                    {auction.hidden ? <span className="status-badge muted">숨김</span> : null}
                  </span>
                  <span className="meta">시작가 {formatNumber(auction.startPrice)}원 · 현재가 {formatNumber(auction.currentPrice)}원</span>
                  <span className="meta">남은 시간: <span className={`auction-time-badge ${remainingTime.tone}`}>{remainingTime.label}</span></span>
                  <span className="meta">종료: {formatDateTime(auction.endAt)}</span>
                  <div className="actions compact-actions">
                    {auction.hidden ? (
                      <button type="button" className="secondary-button" onClick={() => restoreAuction(auction)} disabled={loading}>복구</button>
                    ) : (
                      <>
                        <button type="button" className="danger-button" onClick={() => cancelAuction(auction)} disabled={loading || auction.status !== "OPEN"}>강제 마감</button>
                        <button type="button" className="danger-button" onClick={() => deleteAuction(auction)} disabled={loading || auction.status !== "CLOSED"}>숨김 처리</button>
                      </>
                    )}
                  </div>
                </li>
                );
              })}
              {filteredAuctions.length === 0 ? (
                <li className="list-item empty-state">표시할 경매가 없습니다.</li>
              ) : null}
            </ul>
          </div>
        </section>
  );
}
