import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ConfirmModal from "../components/ConfirmModal";
import ImageDropUploader from "../components/ImageDropUploader";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { loadCategoryOptions } from "../lib/categories";

import "../css/pages/ProductEditPage.css";
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

function ProductEditPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const editorRef = useRef(null);
  const [product, setProduct] = useState(null);
  const [me, setMe] = useState(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("ETC");
  const [categoryOptions, setCategoryOptions] = useState(CATEGORY_OPTIONS);
  const [price, setPrice] = useState("");
  const [images, setImages] = useState([]);
  const [message, setMessage] = useState("상품 정보를 불러오는 중...");
  const [modalMessage, setModalMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const sanitizeHtml = (html) => html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "")
    .replace(/\son\w+="[^"]*"/gi, "")
    .replace(/\son\w+='[^']*'/gi, "");

  const formatDescription = (command, value = null) => {
    document.execCommand(command, false, value);
    if (editorRef.current) {
      setDescription(editorRef.current.innerHTML);
    }
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
      setImages((prev) => [
        ...prev,
        ...uploaded.map((item, index) => ({
          imageUrl: item.imageUrl,
          thumbnail: prev.length === 0 && index === 0,
        })),
      ]);
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setModalMessage(`이미지 업로드 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const loadPageData = useCallback(async () => {
    setLoading(true);
    try {
      const [productResponse, meResponse] = await Promise.all([
        api(`/api/products/${productId}`),
        api("/api/auth/me"),
      ]);
      setProduct(productResponse);
      setMe(meResponse);
      setTitle(productResponse.title);
      setDescription(productResponse.description);
      setCategory(productResponse.category || "ETC");
      setPrice(String(productResponse.price));
      setImages(productResponse.images || []);
      setMessage("");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`상품 정보를 불러오지 못했습니다: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized, productId]);

  useEffect(() => {
    loadPageData();
    loadCategoryOptions()
      .then(setCategoryOptions)
      .catch((error) => setModalMessage(`카테고리 조회 실패: ${error.message}`));
  }, [loadPageData]);

  useEffect(() => {
    if (product && editorRef.current) {
      editorRef.current.innerHTML = sanitizeHtml(product.description || "");
    }
  }, [product]);

  const isSeller = Boolean(me && product && me.id === product.sellerId);

  const updateProduct = async () => {
    const validationMessage = validateProductInput();
    if (validationMessage) {
      setModalMessage(validationMessage);
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
      navigate(`/products/${productId}`, { replace: true });
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setModalMessage(`상품 수정 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const deleteProduct = async () => {
    setLoading(true);
    try {
      await api(`/api/products/${productId}`, { method: "DELETE" });
      setShowDeleteModal(false);
      navigate("/products", { replace: true });
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setShowDeleteModal(false);
      setModalMessage(`상품 삭제 실패: ${error.message}`);
      setLoading(false);
    }
  };

  return (
    <main className="container">
      <h1>상품 수정</h1>
      <div className="card">
        <p className="meta">
          <Link to={`/products/${productId}`}>상품 상세로</Link>
        </p>
        {loading && !product ? <p className="page-message loading">요청 처리 중...</p> : null}
        {message ? <p className="page-message error">{message}</p> : null}

        {product && !isSeller ? (
          <p className="empty-box">판매자만 상품을 수정할 수 있습니다.</p>
        ) : null}

        {product && isSeller ? (
          <>
            <label>제목</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} disabled={loading} />

            <label>설명</label>
            <div className="editor-toolbar">
              <button type="button" onClick={() => formatDescription("bold")} disabled={loading}>굵게</button>
              <button type="button" onClick={() => formatDescription("foreColor", "#dc2626")} disabled={loading}>빨강</button>
              <button type="button" onClick={() => formatDescription("foreColor", "#2563eb")} disabled={loading}>파랑</button>
            </div>
            <div
              ref={editorRef}
              className="rich-editor"
              contentEditable={!loading}
              data-product-editor="edit"
              onInput={(e) => setDescription(e.currentTarget.innerHTML)}
              suppressContentEditableWarning
            />

            <label>가격</label>
            <input
              type="number"
              min="0"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              disabled={loading}
            />

            <label>카테고리</label>
            <select className="select" value={category} onChange={(e) => setCategory(e.target.value)} disabled={loading}>
              {categoryOptions.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>

            <label>상품 이미지</label>
            <ImageDropUploader
              images={images}
              inputName="product-edit-images"
              loading={loading}
              onUpload={uploadImageFiles}
              onRemove={removeImage}
              onSetThumbnail={setThumbnail}
            />

            <div className="actions">
              <button onClick={updateProduct} disabled={loading}>수정 완료</button>
              <button type="button" className="secondary-button" onClick={() => navigate(`/products/${productId}`)} disabled={loading}>취소</button>
              <button className="danger-button" onClick={() => setShowDeleteModal(true)} disabled={loading}>삭제</button>
            </div>
          </>
        ) : null}
      </div>

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

      {modalMessage ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>확인</h2>
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

export default ProductEditPage;
