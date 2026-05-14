import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ConfirmModal from "../components/ConfirmModal";
import ImageDropUploader from "../components/ImageDropUploader";
import ImageViewerModal from "../components/ImageViewerModal";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { openChatWindow } from "../lib/chatWindow";
import { loadCategoryOptions } from "../lib/categories";
import { canUseUserActions } from "../lib/permissions";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const CATEGORY_OPTIONS = [
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
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("ETC");
  const [categoryOptions, setCategoryOptions] = useState(CATEGORY_OPTIONS);
  const [price, setPrice] = useState("");
  const [images, setImages] = useState([]);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [viewerOpen, setViewerOpen] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [questionText, setQuestionText] = useState("");
  const [questionSecret, setQuestionSecret] = useState(false);
  const [answerDrafts, setAnswerDrafts] = useState({});
  const [message, setMessage] = useState("상품을 불러오는 중...");
  const [loading, setLoading] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [myTradeForProduct, setMyTradeForProduct] = useState(null);

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

  const validateProductInput = () => {
    const trimmedTitle = title.trim();
    const trimmedDescription = description.replace(/<[^>]*>/g, "").trim();
    const numericPrice = Number(price);

    if (!trimmedTitle) {
      return "제목을 입력해주세요.";
    }
    if (!trimmedDescription) {
      return "설명을 입력해주세요.";
    }
    if (!Number.isFinite(numericPrice) || Number.isNaN(numericPrice)) {
      return "가격은 숫자로 입력해주세요.";
    }
    if (numericPrice < 0) {
      return "가격은 0 이상이어야 합니다.";
    }
    return null;
  };

  const sanitizeHtml = (html) => html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "")
    .replace(/\son\w+="[^"]*"/gi, "")
    .replace(/\son\w+='[^']*'/gi, "");

  const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

  const formatDescription = (command, value = null) => {
    document.execCommand(command, false, value);
    const editor = document.querySelector("[data-product-editor='detail']");
    if (editor) {
      setDescription(editor.innerHTML);
    }
  };

  const setThumbnail = (index) => {
    setImages((prev) => prev.map((image, itemIndex) => ({ ...image, thumbnail: itemIndex === index })));
  };

  const removeImage = (index) => {
    setImages((prev) => {
      const next = prev.filter((_, itemIndex) => itemIndex !== index);
      if (next.length > 0 && !next.some((image) => image.thumbnail)) {
        next[0] = { ...next[0], thumbnail: true };
      }
      return next;
    });
    setActiveImageIndex(0);
  };

  const uploadImageFiles = async (fileList) => {
    const selectedFiles = Array.from(fileList || []);
    if (selectedFiles.length === 0) {
      return;
    }
    const formData = new FormData();
    selectedFiles.forEach((file) => formData.append("files", file));
    setLoading(true);
    try {
      const uploaded = await api("/api/products/images", {
        method: "POST",
        body: formData,
      });
      setImages((prev) => {
        const next = [
          ...prev,
          ...uploaded.map((item, index) => ({
            imageUrl: item.imageUrl,
            thumbnail: prev.length === 0 && index === 0,
          })),
        ];
        return next;
      });
      setMessage("이미지 업로드 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`이미지 업로드 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const loadProduct = useCallback(async () => {
    try {
      const response = await api(`/api/products/${productId}`);
      setProduct(response);
      setTitle(response.title);
      setDescription(response.description);
      setCategory(response.category || "ETC");
      setImages(response.images || []);
      setActiveImageIndex(0);
      setPrice(String(response.price));
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

  const updateProduct = async () => {
    const validationMessage = validateProductInput();
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }

    setLoading(true);
    try {
      await api(`/api/products/${productId}`, {
        method: "PUT",
        body: JSON.stringify({
          title: title.trim(),
          description: description.trim(),
          category,
          price: Number(price),
          images: images
            .map((image) => ({ imageUrl: image.imageUrl.trim(), thumbnail: image.thumbnail }))
            .filter((image) => image.imageUrl),
        }),
      });
      setMessage("상품 수정 성공");
      await loadProduct();
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 수정 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

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
    loadProduct();
    loadMyTradeForProduct();
    loadQuestions();
    loadCategoryOptions()
      .then(setCategoryOptions)
      .catch((error) => setMessage(`카테고리 조회 실패: ${error.message}`));
  }, [loadMe, loadMyTradeForProduct, loadProduct, loadQuestions]);

  const isSeller = Boolean(me && product && me.id === product.sellerId);
  const canActAsUser = canUseUserActions(me);
  const isSold = Boolean(product?.sold || product?.tradeStatus === "COMPLETED");
  const isReserved = Boolean(product?.tradeStatus === "RESERVED");
  const hasActiveTrade = Boolean(
    myTradeForProduct && (myTradeForProduct.status === "REQUESTED" || myTradeForProduct.status === "ACCEPTED")
  );
  const visibleImages = images.filter((image) => image.imageUrl);
  const activeImage = visibleImages[activeImageIndex] || visibleImages[0];
  const viewerImages = visibleImages.map((image, index) => ({
    src: imageSrc(image.imageUrl),
    alt: `${product?.title ?? "상품"} ${index + 1}`,
  }));

  return (
    <main className="container">
      <h1>상품 상세</h1>
      <div className="card">
        <p className="meta">
          <Link to="/products">목록으로</Link>
        </p>
        <p>{message}</p>
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

          <label>제목</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} disabled={loading || !isSeller} />

          <label>설명</label>
          {isSeller ? (
            <>
              <div className="editor-toolbar">
                <button type="button" onClick={() => formatDescription("bold")} disabled={loading}>굵게</button>
                <button type="button" onClick={() => formatDescription("foreColor", "#dc2626")} disabled={loading}>빨강</button>
                <button type="button" onClick={() => formatDescription("foreColor", "#2563eb")} disabled={loading}>파랑</button>
              </div>
              <div
                className="rich-editor"
                contentEditable={!loading}
                data-product-editor="detail"
                dangerouslySetInnerHTML={{ __html: sanitizeHtml(description) }}
                onInput={(e) => setDescription(e.currentTarget.innerHTML)}
                suppressContentEditableWarning
              />
            </>
          ) : (
            <div
              className="product-description"
              dangerouslySetInnerHTML={{ __html: sanitizeHtml(product.description) }}
            />
          )}

          <label>가격</label>
          <input
            type="number"
            min="0"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            disabled={loading || !isSeller}
          />

          <p className="meta">
            판매자: <Link to={`/users/${product.sellerId}`}>{product.sellerNickname}</Link> · {sellerTrustLabel(product)}
          </p>
          <p className="meta">카테고리: {product.categoryLabel}</p>
          <p className="meta">조회수: {product.viewCount.toLocaleString()} · 찜: {product.favoriteCount.toLocaleString()}</p>
          {product.tradeStatusLabel ? <p className="meta">상태: {product.tradeStatusLabel}</p> : null}
          {!isSeller ? <p className="meta">판매자만 수정/삭제할 수 있습니다.</p> : null}

          {isSeller ? (
            <>
              <label>카테고리</label>
              <select
                className="select"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                disabled={loading}
              >
                {categoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>

              <label>상품 이미지</label>
              <ImageDropUploader
                images={images}
                inputName="product-detail-images"
                loading={loading}
                onUpload={uploadImageFiles}
                onRemove={removeImage}
                onSetThumbnail={setThumbnail}
              />
            </>
          ) : null}

          {isSeller ? (
            <div className="actions">
              <button onClick={updateProduct} disabled={loading}>수정</button>
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
