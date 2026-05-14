import { useEffect } from "react";

export default function ImageViewerModal({
  images,
  activeIndex,
  title,
  onChange,
  onClose,
}) {
  const activeImage = images[activeIndex] || images[0];
  const hasMultiple = images.length > 1;

  const move = (direction) => {
    if (!hasMultiple) {
      return;
    }
    onChange((activeIndex + direction + images.length) % images.length);
  };

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        onClose();
      }
      if (event.key === "ArrowLeft") {
        move(-1);
      }
      if (event.key === "ArrowRight") {
        move(1);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  });

  if (!activeImage) {
    return null;
  }

  return (
    <div className="image-viewer-backdrop" role="dialog" aria-modal="true" aria-label={`${title} 이미지 보기`}>
      <button type="button" className="image-viewer-close" onClick={onClose} aria-label="닫기">
        X
      </button>
      <div className="image-viewer-shell">
        <div className="image-viewer-stage">
          {hasMultiple ? (
            <button type="button" className="image-viewer-nav prev" onClick={() => move(-1)} aria-label="이전 이미지">
              {"<"}
            </button>
          ) : null}
          <button
            type="button"
            className="image-viewer-image-button"
            onClick={onClose}
            aria-label="이미지 보기 닫기"
          >
            <img src={activeImage.src} alt={activeImage.alt || title} />
          </button>
          {hasMultiple ? (
            <button type="button" className="image-viewer-nav next" onClick={() => move(1)} aria-label="다음 이미지">
              {">"}
            </button>
          ) : null}
        </div>
        <div className="image-viewer-footer">
          <strong>{title}</strong>
          <span>{activeIndex + 1} / {images.length}</span>
        </div>
        {hasMultiple ? (
          <div className="image-viewer-thumbs">
            {images.map((image, index) => (
              <button
                type="button"
                className={index === activeIndex ? "active" : ""}
                key={`${image.src}-${index}`}
                onClick={() => onChange(index)}
                aria-label={`${index + 1}번 이미지 보기`}
              >
                <img src={image.src} alt="" />
              </button>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
