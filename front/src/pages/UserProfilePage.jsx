import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { API_BASE_URL } from "../lib/config";

function UserProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [message, setMessage] = useState("사용자 프로필을 불러오는 중...");
  const [loading, setLoading] = useState(false);

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api(`/api/users/${userId}/profile`);
      setProfile(response);
      setMessage("사용자 프로필 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`사용자 프로필 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized, userId]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const shouldShowMessage = loading || message.includes("실패") || message.includes("불러오는 중");

  return (
    <main className="container">
      <h1>사용자 프로필</h1>
      <div className="card">
        <p className="meta">
          <Link to="/products">상품 목록으로</Link>
        </p>
        {shouldShowMessage ? (
          <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
            {loading ? "요청 처리 중..." : message}
          </p>
        ) : null}
      </div>

      {profile ? (
        <>
          <div className="card">
            <h2>{profile.nickname}</h2>
            <p className="meta">평점: {Number(profile.averageRating || 0).toFixed(1)}점 ({profile.reviewCount}개)</p>
            <p className="meta">매너 온도: {Number(profile.mannerTemperature || 36.5).toFixed(1)}도</p>
            <p className="meta">판매 상품: {profile.sellingProductCount}개</p>
            <div className="actions">
              <button
                type="button"
                onClick={() => navigate(`/reports?targetType=USER&targetId=${profile.id}&reason=${encodeURIComponent("사용자 신고")}`)}
                disabled={loading}
              >
                사용자 신고
              </button>
            </div>
          </div>

          <div className="card">
            <h2>판매 상품</h2>
            {profile.sellingProducts.length === 0 ? (
              <p className="empty-box">현재 판매 상품이 없습니다.</p>
            ) : (
              <ul className="list">
                {profile.sellingProducts.map((product) => (
                  <li key={product.id} className={`list-item${product.tradeStatus !== "ON_SALE" ? " sold" : ""}`}>
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
                    <span className="sold-badge">{product.tradeStatusLabel}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="card">
            <h2>받은 리뷰</h2>
            {profile.reviews.length === 0 ? (
              <p className="empty-box">아직 받은 리뷰가 없습니다.</p>
            ) : (
              <ul className="list">
                {profile.reviews.map((review) => (
                  <li key={review.id} className="list-item">
                    <strong>{review.rating}점 · {review.reviewerNickname}</strong>
                    <span className="meta">{review.sourceTitle || "거래"} 후기</span>
                    <p>{review.content}</p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      ) : null}
    </main>
  );
}

export default UserProfilePage;
