import { useId, useState } from "react";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const imageSrc = (url) => (url?.startsWith("/uploads/") ? `${API_BASE_URL}${url}` : url);

export default function ImageDropUploader({
  images,
  inputName,
  loading,
  onUpload,
  onRemove,
  onSetThumbnail,
}) {
  const inputId = useId();
  const [dragging, setDragging] = useState(false);
  const visibleImages = images.filter((image) => image.imageUrl?.trim());

  const uploadFiles = (fileList) => {
    const files = Array.from(fileList || []).filter((file) => file.type.startsWith("image/"));
    if (files.length > 0) {
      onUpload(files);
    }
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setDragging(false);
    if (!loading) {
      uploadFiles(event.dataTransfer.files);
    }
  };

  return (
    <div className="image-drop-uploader">
      {visibleImages.length === 0 ? (
        <label
          className={`image-drop-zone${dragging ? " dragging" : ""}${loading ? " disabled" : ""}`}
          htmlFor={inputId}
          onDragEnter={(event) => {
            event.preventDefault();
            setDragging(true);
          }}
          onDragOver={(event) => event.preventDefault()}
          onDragLeave={(event) => {
            event.preventDefault();
            setDragging(false);
          }}
          onDrop={handleDrop}
        >
          <span>이미지를 업로드하려면 여기에 이미지를 끌어놓거나 클릭하세요</span>
        </label>
      ) : (
        <div
          className={`image-preview-drop-area${dragging ? " dragging" : ""}`}
          onDragEnter={(event) => {
            event.preventDefault();
            setDragging(true);
          }}
          onDragOver={(event) => event.preventDefault()}
          onDragLeave={(event) => {
            event.preventDefault();
            setDragging(false);
          }}
          onDrop={handleDrop}
        >
          <div className="image-preview-grid">
            {visibleImages.map((image) => {
              const originalIndex = images.indexOf(image);
              return (
                <div className="image-preview-tile" key={`${image.imageUrl}-${originalIndex}`}>
                  <button
                    type="button"
                    className="image-preview-thumb-button"
                    onClick={() => onSetThumbnail(originalIndex)}
                    disabled={loading}
                    title="대표 이미지로 설정"
                  >
                    <img src={imageSrc(image.imageUrl)} alt={`등록 이미지 ${originalIndex + 1}`} />
                  </button>
                  {image.thumbnail ? <span className="image-thumbnail-dot" title="대표 이미지" /> : null}
                  <button
                    type="button"
                    className="image-remove-button"
                    onClick={() => onRemove(originalIndex)}
                    disabled={loading}
                    aria-label="이미지 삭제"
                  >
                    x
                  </button>
                </div>
              );
            })}
            <label className="image-preview-add-tile" htmlFor={inputId}>
              +
            </label>
          </div>
        </div>
      )}
      <input
        id={inputId}
        name={inputName}
        className="image-file-input"
        type="file"
        accept="image/*"
        multiple
        onChange={(event) => {
          uploadFiles(event.target.files);
          event.target.value = "";
        }}
        disabled={loading}
      />
    </div>
  );
}
