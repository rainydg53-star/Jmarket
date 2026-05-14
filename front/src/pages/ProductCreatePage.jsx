import { useState } from "react";
import { useNavigate } from "react-router-dom";
import React from "react";
import { api } from "../lib/api";
import ImageDropUploader from "../components/ImageDropUploader";
import { clearAccessToken } from "../lib/auth";
import { loadCategoryOptions } from "../lib/categories";
import { parseRestrictionMessage } from "../lib/restriction";

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

function ProductCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("ETC");
  const [categoryOptions, setCategoryOptions] = useState(CATEGORY_OPTIONS);
  const [price, setPrice] = useState("");
  const [images, setImages] = useState([{ imageUrl: "", thumbnail: true }]);
  const [restrictionModal, setRestrictionModal] = useState(null);
  const [modalMessage, setModalMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleUnauthorized = () => {
    clearAccessToken();
    navigate("/login", { replace: true });
  };

  React.useEffect(() => {
    let active = true;
    loadCategoryOptions()
      .then((options) => {
        if (!active) return;
        setCategoryOptions(options);
        setCategory((currentCategory) => (
          options.length > 0 && !options.some((option) => option.value === currentCategory)
            ? options[0].value
            : currentCategory
        ));
      })
      .catch((error) => setModalMessage(`카테고리 조회 실패: ${error.message}`));
    return () => {
      active = false;
    };
  }, []);

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
    if (images.some((image) => image.imageUrl.trim() === "" && image.thumbnail)) {
      return "대표 이미지 URL을 입력하거나 다른 이미지를 대표로 지정해주세요.";
    }
    return null;
  };

  const formatDescription = (command, value = null) => {
    document.execCommand(command, false, value);
    const editor = document.querySelector("[data-product-editor='create']");
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
      if (next.length === 0) {
        return [{ imageUrl: "", thumbnail: true }];
      }
      if (!next.some((image) => image.thumbnail)) {
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
      setImages((prev) => {
        const existing = prev.filter((image) => image.imageUrl.trim());
        const next = [
          ...existing,
          ...uploaded.map((item, index) => ({
            imageUrl: item.imageUrl,
            thumbnail: existing.length === 0 && index === 0,
          })),
        ];
        return next.length > 0 ? next : [{ imageUrl: "", thumbnail: true }];
      });
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

  const createProduct = async () => {
    const validationMessage = validateProductInput();
    if (validationMessage) {
      setModalMessage(validationMessage);
      return;
    }

    setLoading(true);
    try {
      const createdProduct = await api("/api/products", {
        method: "POST",
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
      setTitle("");
      setDescription("");
      setCategory("ETC");
      setPrice("");
      setImages([{ imageUrl: "", thumbnail: true }]);
      navigate(`/products/${createdProduct.id}`, { replace: true });
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      if (error.code === "A007") {
        setRestrictionModal(parseRestrictionMessage(error.message));
        return;
      }
      setModalMessage(`상품 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container">
      <h1>상품등록</h1>
      <div className="card">
        <label>제목</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} disabled={loading} />

        <label>설명</label>
        <div className="editor-toolbar">
          <button type="button" onClick={() => formatDescription("bold")} disabled={loading}>굵게</button>
          <button type="button" onClick={() => formatDescription("foreColor", "#dc2626")} disabled={loading}>빨강</button>
          <button type="button" onClick={() => formatDescription("foreColor", "#2563eb")} disabled={loading}>파랑</button>
        </div>
        <div
          className="rich-editor"
          contentEditable={!loading}
          data-product-editor="create"
          onInput={(e) => setDescription(e.currentTarget.innerHTML)}
          suppressContentEditableWarning
        />

        <label>가격</label>
        <input
          type="number"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
          min="0"
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
          inputName="product-images"
          loading={loading}
          onUpload={uploadImageFiles}
          onRemove={removeImage}
          onSetThumbnail={setThumbnail}
        />

        <div className="actions">
          <button onClick={createProduct} disabled={loading}>등록</button>
          <button type="button" className="secondary-button" onClick={() => navigate("/products")}>상품 목록</button>
        </div>
      </div>

      {restrictionModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>이용이 제한된 기능입니다</h2>
            <p>{restrictionModal.feature} 기능은 현재 제한되어 있습니다.</p>
            <p>사유: {restrictionModal.reason}</p>
            {restrictionModal.restrictedUntil ? <p>해제 예정: {restrictionModal.restrictedUntil}</p> : null}
            <div className="actions">
              <button type="button" onClick={() => setRestrictionModal(null)}>확인</button>
            </div>
          </div>
        </div>
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

export default ProductCreatePage;
