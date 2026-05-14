export default function ConfirmModal({
  title,
  message,
  detail,
  confirmLabel = "확인",
  cancelLabel = "취소",
  tone = "danger",
  loading = false,
  onConfirm,
  onCancel,
}) {
  const confirmButtonClass = tone === "danger" ? "danger-button" : undefined;

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className={`modal-card confirm-modal confirm-modal-${tone}`}>
        <div className="confirm-modal-mark" aria-hidden="true">!</div>
        <h2>{title}</h2>
        <p>{message}</p>
        {detail ? <p className="meta">{detail}</p> : null}
        <div className="actions confirm-modal-actions">
          <button type="button" className="secondary-button" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button type="button" className={confirmButtonClass} onClick={onConfirm} disabled={loading}>
            {loading ? "처리 중..." : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
