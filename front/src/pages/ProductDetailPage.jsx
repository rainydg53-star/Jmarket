import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ConfirmModal from "../components/ConfirmModal";
import ImageViewerModal from "../components/ImageViewerModal";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { openChatWindow } from "../lib/chatWindow";
import { API_BASE_URL } from "../lib/config";
import { canUseUserActions } from "../lib/permissions";

import "../css/pages/ProductDetailPage.css";
const sellerTrustLabel = (product) => {
  const reviewCount = Number(product?.sellerReviewCount || 0);
  if (reviewCount === 0) {
    return "후기 없음 · 매너 36.5도";
  }
  return `평점 ${Number(product?.sellerAverageRating || 0).toFixed(1)} (${reviewCount}개) · 매너 ${Number(product?.sellerMannerTemperature || 36.5).toFixed(1)}도`;
};

function ProductDetailPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [me, setMe] = useState(null);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [viewerOpen, setViewerOpen] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [questionText, setQuestionText] = useState("");
  const [questionSecret, setQuestionSecret] = useState(false);
  const [answerDrafts, setAnswerDrafts] = useState({});
  const [message, setMessage] = useState("상품을 불러오는 중...");
  const [loading, setLoading] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showMileageModal, setShowMileageModal] = useState(false);
  const [myTradeForProduct, setMyTradeForProduct] = useState(null);
  const [mileageAccount, setMileageAccount] = useState(null);

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

  const sanitizeHtml = (html) => html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "")
    .replace(/\son\w+="[^"]*"/gi, "")
    .replace(/\son\w+='[^']*'/gi, "");

  const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const loadProduct = useCallback(async () => {
    try {
      const response = await api(`/api/products/${productId}`);
      setProduct(response);
      setActiveImageIndex(0);
      setMessage("상품 상세 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 상세 조회 실패: ${error.message}`);
    }
  }, [handleUnauthorized, productId]);

  const loadQuestions = useCallback(async () => {
    try {
      const response = await api(`/api/products/${productId}/questions`);
      setQuestions(response);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
      }
    }
  }, [handleUnauthorized, productId]);

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

  const loadMileageAccount = useCallback(async () => {
    try {
      const response = await api("/api/mileage/me");
      setMileageAccount(response);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
      }
    }
  }, [handleUnauthorized]);

  const loadMyTradeForProduct = useCallback(async () => {
    try {
      const response = await api("/api/trades/me?role=ALL");
      const matched = response
        .filter((trade) => String(trade.productId) === String(productId))
        .sort((a, b) => new Date(b.requestedAt).getTime() - new Date(a.requestedAt).getTime());

      const active = matched.find((trade) => trade.status === "REQUESTED" || trade.status === "ACCEPTED");
      setMyTradeForProduct(active || matched[0] || null);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
      }
    }
  }, [handleUnauthorized, productId]);

  const toggleFavorite = async () => {
    setLoading(true);
    try {
      const response = await api(`/api/products/${productId}/favorite`, { method: "PATCH" });
      setProduct((prev) => ({
        ...prev,
        favorited: response.favorited,
        favoriteCount: response.favoriteCount,
      }));
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`찜하기 처리 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const createQuestion = async () => {
    if (!questionText.trim()) {
      setMessage("상품 문의 내용을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api(`/api/products/${productId}/questions`, {
        method: "POST",
        body: JSON.stringify({
          question: questionText.trim(),
          secret: questionSecret,
        }),
      });
      setQuestionText("");
      setQuestionSecret(false);
      await loadQuestions();
      setMessage("상품 문의 등록 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 문의 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const answerQuestion = async (questionId) => {
    const answer = answerDrafts[questionId]?.trim();
    if (!answer) {
      setMessage("답변 내용을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api(`/api/products/${productId}/questions/${questionId}/answer`, {
        method: "PATCH",
        body: JSON.stringify({ answer }),
      });
      setAnswerDrafts((prev) => ({ ...prev, [questionId]: "" }));
      await loadQuestions();
      setMessage("상품 문의 답변 등록 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 문의 답변 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const requestTrade = async () => {
    setLoading(true);
    try {
      const account = await api("/api/mileage/me");
      setMileageAccount(account);
      if (Number(account.availableBalance ?? 0) < Number(product?.price ?? 0)) {
        setShowMileageModal(true);
        return;
      }

      const payload = {
        productId: Number(productId),
      };
      const trade = await api("/api/trades", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      const room = await api("/api/chat/rooms/trade", {
        method: "POST",
        body: JSON.stringify({ tradeId: trade.id }),
      });
      await loadMyTradeForProduct();
      openChatWindow(room.id);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      if (error.code === "M002" || error.message.includes("마일리지가 부족")) {
        setShowMileageModal(true);
        return;
      }
      setMessage(`거래 요청 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const openTradeChatRoom = async () => {
    if (!myTradeForProduct) {
      return;
    }
    setLoading(true);
    try {
      const room = await api("/api/chat/rooms/trade", {
        method: "POST",
        body: JSON.stringify({ tradeId: myTradeForProduct.id }),
      });
      openChatWindow(room.id);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`거래 채팅 연결 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const deleteProduct = async () => {
    setLoading(true);
    try {
      await api(`/api/products/${productId}`, {
        method: "DELETE",
      });
      setShowDeleteModal(false);
      navigate("/products", { replace: true });
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 삭제 실패: ${error.message}`);
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMe();
    loadMileageAccount();
    loadProduct();
    loadMyTradeForProduct();
    loadQuestions();
  }, [loadMe, loadMileageAccount, loadMyTradeForProduct, loadProduct, loadQuestions]);

  const isSeller = Boolean(me && product && me.id === product.sellerId);
  const canActAsUser = canUseUserActions(me);
  const isSold = Boolean(product?.sold || product?.tradeStatus === "COMPLETED");
  const isReserved = Boolean(product?.tradeStatus === "RESERVED");
  const hasActiveTrade = Boolean(
    myTradeForProduct && (myTradeForProduct.status === "REQUESTED" || myTradeForProduct.status === "ACCEPTED")
  );
  const visibleImages = (product?.images || []).filter((image) => image.imageUrl);
  const activeImage = visibleImages[activeImageIndex] || visibleImages[0];
  const viewerImages = visibleImages.map((image, index) => ({
    src: imageSrc(image.imageUrl),
    alt: `${product?.title ?? "상품"} ${index + 1}`,
  }));
  const shouldShowMessage = loading
    || message.includes("실패")
    || message.includes("오류")
    || message.includes("불러오는 중");

  return (
    <main className="container">
      <h1>상품 상세</h1>
      <div className="card">
        <p className="meta">
          <Link to="/products">목록으로</Link>
        </p>
        {shouldShowMessage ? <p>{loading ? "요청 처리 중..." : message}</p> : null}
      </div>

      {product ? (
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
                  이전
                </button>
                <button
                  type="button"
                  className="gallery-main-image-button"
                  onClick={() => setViewerOpen(true)}
                  aria-label="상품 이미지 크게 보기"
                >
                  <img src={imageSrc(activeImage.imageUrl)} alt={product.title} />
                </button>
                <button
                  type="button"
                  className="gallery-nav"
                  onClick={() => setActiveImageIndex((prev) => (prev + 1) % visibleImages.length)}
                  disabled={visibleImages.length < 2}
                >
                  다음
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
                    <img src={imageSrc(image.imageUrl)} alt={`${product.title} ${index + 1}`} />
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          <h2>{product.title}</h2>
          <p className="price-text">{product.price.toLocaleString()}원</p>
          <div
            className="product-description"
            dangerouslySetInnerHTML={{ __html: sanitizeHtml(product.description) }}
          />

          <p className="meta">
            판매자: <Link to={`/users/${product.sellerId}`}>{product.sellerNickname}</Link> · {sellerTrustLabel(product)}
          </p>
          <p className="meta">카테고리: {product.categoryLabel}</p>
          <p className="meta">조회수: {product.viewCount.toLocaleString()} · 찜: {product.favoriteCount.toLocaleString()}</p>
          {product.tradeStatusLabel ? <p className="meta">상태: {product.tradeStatusLabel}</p> : null}
          {isSeller ? (
            <div className="actions">
              <button type="button" onClick={() => navigate(`/products/${productId}/edit`)} disabled={loading}>수정하기</button>
              <button className="danger-button" onClick={() => setShowDeleteModal(true)} disabled={loading}>삭제</button>
            </div>
          ) : canActAsUser ? (
            <div className="actions">
              <button onClick={toggleFavorite} disabled={loading}>
                {product.favorited ? "찜 해제" : "찜하기"}
              </button>
              <button
                type="button"
                onClick={() => navigate(`/reports?targetType=PRODUCT&targetId=${product.id}&reason=${encodeURIComponent("부적절한 상품")}`)}
                disabled={loading}
              >
                상품 신고
              </button>
              <button
                type="button"
                onClick={() => navigate(`/reports?targetType=USER&targetId=${product.sellerId}&reason=${encodeURIComponent("판매자 신고")}`)}
                disabled={loading}
              >
                판매자 신고
              </button>
            </div>
          ) : null}
        </div>
      ) : null}

      {product && !isSeller && canActAsUser ? (
        <div className="card">
          {myTradeForProduct ? (
            <>
              <h2>내 거래 상태</h2>
              <p className="meta">상태: {statusLabel(myTradeForProduct.status)}</p>
              <p className="meta">거래금액: {myTradeForProduct.offeredPrice.toLocaleString()}원</p>
              <p className="meta">결제수단: 마일리지</p>
              <div className="actions">
                <button type="button" onClick={openTradeChatRoom} disabled={loading}>거래 채팅 열기</button>
                <button type="button" className="secondary-button" onClick={() => navigate("/trades")} disabled={loading}>거래 관리로 이동</button>
              </div>
            </>
          ) : (
            <p className="meta">이 상품에 대한 내 거래 이력이 아직 없습니다.</p>
          )}
          {isSold ? <p className="meta">거래완료 상품은 추가 거래 요청이 불가능합니다.</p> : null}
          {isReserved && !myTradeForProduct ? <p className="meta">다른 사용자의 거래가 진행 중인 예약 상품입니다.</p> : null}
        </div>
      ) : null}

      {product && !isSeller && canActAsUser && !isSold && !isReserved ? (
        <div className="card">
          <h2>구매신청</h2>
          <p className="meta">거래금액: {product.price.toLocaleString()}원</p>
          <p className="meta">결제방식: 마일리지(상품가 전액 예약)</p>
          {mileageAccount ? (
            <p className="meta">사용 가능 마일리지: {Number(mileageAccount.availableBalance ?? 0).toLocaleString()}P</p>
          ) : null}

          {hasActiveTrade ? <p className="meta">진행중 거래가 있어 새 요청을 보낼 수 없습니다.</p> : null}

          <div className="actions">
            <button onClick={requestTrade} disabled={loading || hasActiveTrade}>구매신청</button>
          </div>
        </div>
      ) : null}

      {product ? (
        <div className="card">
          <h2>상품 Q&A</h2>
          {!isSeller && canActAsUser ? (
            <>
              <textarea
                className="textarea"
                value={questionText}
                onChange={(e) => setQuestionText(e.target.value)}
                placeholder="상품에 대해 궁금한 점을 남겨주세요."
                disabled={loading}
              />
              <label className="inline-check">
                <input
                  type="checkbox"
                  checked={questionSecret}
                  onChange={(e) => setQuestionSecret(e.target.checked)}
                  disabled={loading}
                />
                비밀글
              </label>
              <div className="actions">
                <button onClick={createQuestion} disabled={loading}>문의 등록</button>
              </div>
            </>
          ) : null}

          {questions.length === 0 ? (
            <p className="meta">등록된 상품 문의가 없습니다.</p>
          ) : (
            <ul className="list">
              {questions.map((question) => (
                <li className="list-item" key={question.id}>
                  <strong>{question.secret ? "[비밀글] " : ""}{question.question}</strong>
                  <span className="meta">작성자: {question.questionerNickname}</span>
                  {question.answer ? (
                    <p className="answer-box">답변: {question.answer}</p>
                  ) : (
                    <p className="meta">아직 답변이 없습니다.</p>
                  )}
                  {isSeller && question.visible ? (
                    <>
                      <textarea
                        className="textarea"
                        value={answerDrafts[question.id] || ""}
                        onChange={(e) => setAnswerDrafts((prev) => ({ ...prev, [question.id]: e.target.value }))}
                        placeholder="판매자 답변"
                        disabled={loading}
                      />
                      <button onClick={() => answerQuestion(question.id)} disabled={loading}>답변 저장</button>
                    </>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : null}

      {showDeleteModal ? (
        <ConfirmModal
          title="상품 삭제 확인"
          message="정말 삭제하시겠습니까?"
          detail="삭제 후에는 상품 상세와 목록에서 사라지며 복구할 수 없습니다."
          confirmLabel="삭제 진행"
          loading={loading}
          onCancel={() => setShowDeleteModal(false)}
          onConfirm={deleteProduct}
        />
      ) : null}

      {showMileageModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>마일리지 부족</h2>
            <p>마일리지가 부족합니다. 충전 후 구매신청해주세요.</p>
            <div className="actions">
              <button type="button" className="secondary-button" onClick={() => setShowMileageModal(false)}>
                닫기
              </button>
              <button type="button" onClick={() => navigate("/mileage")}>
                마일리지 충전하러 가기
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {viewerOpen ? (
        <ImageViewerModal
          images={viewerImages}
          activeIndex={activeImageIndex}
          title={product?.title ?? "상품 이미지"}
          onChange={setActiveImageIndex}
          onClose={() => setViewerOpen(false)}
        />
      ) : null}
    </main>
  );
}

export default ProductDetailPage;
