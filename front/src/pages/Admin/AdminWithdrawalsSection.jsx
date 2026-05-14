import { formatDateTime, formatNumber } from "./adminUtils";
import { getWithdrawalStatusTone } from "../../lib/statusTone";

const STATUS_LABELS = {
  REQUESTED: "요청",
  COMPLETED: "완료",
  REJECTED: "반려",
};

export default function AdminWithdrawalsSection({ withdrawals, loading, completeWithdrawal, rejectWithdrawal }) {
  return (
    <section className="card">
      <h2>출금 요청</h2>
      {withdrawals.length === 0 ? (
        <p className="meta">출금 요청이 없습니다.</p>
      ) : (
        <ul className="list">
          {withdrawals.map((item) => (
            <li key={item.id} className="list-item">
              <strong>
                <span className={`status-badge ${getWithdrawalStatusTone(item.status)}`}>{STATUS_LABELS[item.status] ?? item.status}</span>
              </strong>
              <span>{item.userNickname} ({item.userEmail})</span>
              <span>금액: {formatNumber(item.amount)}원</span>
              <span>계좌: {item.bankName} {item.accountNumberMasked} / {item.accountHolder}</span>
              <span className="meta">요청: {formatDateTime(item.requestedAt)}</span>
              {item.completedAt ? <span className="meta">완료: {formatDateTime(item.completedAt)}</span> : null}
              {item.rejectedAt ? <span className="meta">반려: {formatDateTime(item.rejectedAt)}</span> : null}
              {item.rejectReason ? <span className="meta">사유: {item.rejectReason}</span> : null}
              {item.status === "REQUESTED" ? (
                <div className="actions compact-actions">
                  <button type="button" onClick={() => completeWithdrawal(item)} disabled={loading}>Mock 송금 완료</button>
                  <button type="button" className="danger-button" onClick={() => rejectWithdrawal(item)} disabled={loading}>반려</button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
