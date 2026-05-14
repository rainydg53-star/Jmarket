import { Link } from "react-router-dom";
import { getAuctionDisplayStatusInfo, getAuctionRemainingTimeInfo } from "../../lib/auctionStatus";
import { formatDateTime, formatNumber } from "./adminUtils";

export default function AdminOperationsSection({ products, auctions, loading, deleteProduct, cancelAuction }) {
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
            <h2>경매 운영</h2>
            <ul className="list admin-list">
              {auctions.map((auction) => {
                const remainingTime = getAuctionRemainingTimeInfo(auction);
                const displayStatus = getAuctionDisplayStatusInfo(auction);
                return (
                <li className="list-item" key={auction.id}>
                  <Link to={`/auctions/${auction.id}`} className="admin-title-link">
                    {auction.productTitle}
                  </Link>
                  <span className="meta">판매자: {auction.sellerNickname} · 상태: <span className={`status-badge ${displayStatus.tone}`}>{displayStatus.label}</span></span>
                  <span className="meta">시작가 {formatNumber(auction.startPrice)}원 · 현재가 {formatNumber(auction.currentPrice)}원</span>
                  <span className="meta">남은 시간: <span className={`auction-time-badge ${remainingTime.tone}`}>{remainingTime.label}</span></span>
                  <span className="meta">종료: {formatDateTime(auction.endAt)}</span>
                  <div className="actions compact-actions">
                    <button type="button" className="danger-button" onClick={() => cancelAuction(auction)} disabled={loading || auction.status !== "OPEN"}>강제 마감</button>
                  </div>
                </li>
                );
              })}
            </ul>
          </div>
        </section>
  );
}
