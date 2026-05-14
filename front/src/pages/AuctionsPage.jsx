import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
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

function AuctionsPage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [auctionCategory, setAuctionCategory] = useState("ETC");
  const [categoryOptions, setCategoryOptions] = useState(CATEGORY_OPTIONS);
  const [startPrice, setStartPrice] = useState("");
  const [instantBuyPrice, setInstantBuyPrice] = useState("");
  const [images, setImages] = useState([{ imageUrl: "", thumbnail: true }]);
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [restrictionModal, setRestrictionModal] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("경매 정보를 입력한 뒤 등록해주세요.");

  const handleUnauthorized = () => {
    clearAccessToken();
    navigate("/login", { replace: true });
  };

  useEffect(() => {
    let active = true;
    loadCategoryOptions()
      .then((options) => {
        if (!active) return;
        setCategoryOptions(options);
        setAuctionCategory((currentCategory) => (
          options.length > 0 && !options.some((option) => option.value === currentCategory)
            ? options[0].value
            : currentCategory
        ));
      })
      .catch((error) => setMessage(`카테고리 조회 실패: ${error.message}`));
    return () => {
      active = false;
    };
  }, []);

  const validateAuctionInput = () => {
    const sp = Number(startPrice);
    const ibp = instantBuyPrice.trim() ? Number(instantBuyPrice) : null;

    if (!title.trim()) {
      return "상품명을 입력해주세요.";
    }
    if (!description.trim()) {
      return "상품 설명을 입력해주세요.";
    }
    if (images.some((image) => image.imageUrl.trim() === "" && image.thumbnail)) {
      return "대표 이미지를 업로드하거나 다른 이미지를 대표로 지정해주세요.";
    }
    if (!Number.isFinite(sp) || sp < 0) {
      return "최소입찰가는 0 이상 숫자여야 합니다.";
    }
    if (ibp !== null && (!Number.isFinite(ibp) || ibp < sp)) {
      return "즉시구매가는 최소입찰가 이상이어야 합니다.";
    }
    if (!startAt || !endAt) {
      return "시작/종료 시간을 모두 입력해주세요.";
    }
    if (new Date(startAt).getTime() >= new Date(endAt).getTime()) {
      return "시작 시간은 종료 시간보다 빨라야 합니다.";
    }
    return null;
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

  const createAuction = async () => {
    const validationMessage = validateAuctionInput();
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }

    setLoading(true);
    try {
      const createdAuction = await api("/api/auctions", {
        method: "POST",
        body: JSON.stringify({
          title: title.trim(),
          description: description.trim(),
          category: auctionCategory,
          startPrice: Number(startPrice),
          instantBuyPrice: instantBuyPrice.trim() ? Number(instantBuyPrice) : null,
          startAt: new Date(startAt).toISOString(),
          endAt: new Date(endAt).toISOString(),
          images: images
            .map((image) => ({ imageUrl: image.imageUrl.trim(), thumbnail: image.thumbnail }))
            .filter((image) => image.imageUrl),
        }),
      });
      setTitle("");
      setDescription("");
      setAuctionCategory("ETC");
      setStartPrice("");
      setInstantBuyPrice("");
      setImages([{ imageUrl: "", thumbnail: true }]);
      setStartAt("");
      setEndAt("");
      setMessage("경매 등록 성공");
      navigate(`/auctions/${createdAuction.id}`, { replace: true });
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      if (error.code === "A007") {
        setRestrictionModal(parseRestrictionMessage(error.message));
        setMessage("");
        return;
      }
      setMessage(`경매 등록 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container">
      <h1>경매등록</h1>
      {message ? (
        <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
          {loading ? "요청 처리 중..." : message}
        </p>
      ) : null}

      <div className="card">
        <h2>상품 + 경매 정보 입력</h2>
        <label>상품명</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} disabled={loading} />

        <label>상품 설명</label>
        <input value={description} onChange={(e) => setDescription(e.target.value)} disabled={loading} />

        <label>카테고리</label>
        <select
          className="select"
          value={auctionCategory}
          onChange={(e) => setAuctionCategory(e.target.value)}
          disabled={loading}
        >
          {categoryOptions.filter((option) => option.value).map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>

        <label>경매 이미지</label>
        <ImageDropUploader
          images={images}
          inputName="auction-images"
          loading={loading}
          onUpload={uploadImageFiles}
          onRemove={removeImage}
          onSetThumbnail={setThumbnail}
        />

        <label>최소입찰가</label>
        <input
          type="number"
          min="0"
          value={startPrice}
          onChange={(e) => setStartPrice(e.target.value)}
          disabled={loading}
        />

        <label>즉시구매가 (선택)</label>
        <input
          type="number"
          min="1"
          value={instantBuyPrice}
          onChange={(e) => setInstantBuyPrice(e.target.value)}
          disabled={loading}
          placeholder="미입력 시 즉시구매 없음"
        />

        <label>시작 시간</label>
        <input
          type="datetime-local"
          value={startAt}
          onChange={(e) => setStartAt(e.target.value)}
          disabled={loading}
        />

        <label>종료 시간</label>
        <input
          type="datetime-local"
          value={endAt}
          onChange={(e) => setEndAt(e.target.value)}
          disabled={loading}
        />

        <div className="actions">
          <button onClick={createAuction} disabled={loading}>경매 등록</button>
          <button type="button" className="secondary-button" onClick={() => navigate("/auctions/products")} disabled={loading}>경매 목록</button>
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
    </main>
  );
}

export default AuctionsPage;
