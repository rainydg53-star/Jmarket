import { useCallback, useEffect, useLayoutEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";

import "../css/pages/MileagePage.css";
const BANK_OPTIONS = [
  "계좌없음",
  "광주은행",
  "경남은행",
  "국민은행",
  "기업은행",
  "농협은행",
  "대구은행",
  "부산은행",
  "산업은행",
  "새마을금고",
  "수협은행",
  "신한은행",
  "신협",
  "우리은행",
  "우체국",
  "전북은행",
  "제주은행",
  "카카오뱅크",
  "케이뱅크",
  "토스뱅크",
  "하나은행",
  "한국씨티은행",
  "SC제일은행",
];

const LEDGER_TYPE_LABELS = {
  CHARGE: "충전",
  USE: "사용",
  RESERVE: "예약",
  RELEASE: "예약 해제",
  TRANSFER_OUT: "거래 출금",
  TRANSFER_IN: "거래 입금",
  WITHDRAW_REQUEST: "출금 요청",
  WITHDRAW_COMPLETE: "출금 완료",
  WITHDRAW_REJECT: "출금 반려",
  ADMIN_GRANT: "관리자 지급",
  ADMIN_DEDUCT: "관리자 차감",
};

function MileagePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [account, setAccount] = useState(null);
  const [ledger, setLedger] = useState([]);
  const [payments, setPayments] = useState([]);
  const [withdrawals, setWithdrawals] = useState([]);
  const [payAmount, setPayAmount] = useState("");
  const [withdrawForm, setWithdrawForm] = useState({
    amount: "",
    bankName: "",
    accountNumber: "",
    accountHolder: "",
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("마일리지 정보를 불러오는 중입니다.");
  const [modalState, setModalState] = useState({ open: false, title: "", body: "" });

  useLayoutEffect(() => {
    window.scrollTo({ top: 0, left: 0 });
  }, []);

  const formatNumber = (value) => Number(value ?? 0).toLocaleString();

  const formatDateTime = (value) => {
    if (!value) {
      return "-";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      timeZone: "Asia/Seoul",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(new Date(value));
  };

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const openModal = useCallback((title, body) => {
    setModalState({ open: true, title, body });
  }, []);

  const loadMileage = useCallback(async () => {
    setLoading(true);
    try {
      const [accountRes, ledgerRes, paymentRes, withdrawalRes] = await Promise.all([
        api("/api/mileage/me"),
        api("/api/mileage/me/ledger"),
        api("/api/payments/kakaopay/me"),
        api("/api/mileage/me/withdrawals"),
      ]);
      setAccount(accountRes);
      setLedger(ledgerRes);
      setPayments(paymentRes);
      setWithdrawals(withdrawalRes);
      setMessage("마일리지 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`마일리지 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  const validateAmount = (rawValue) => {
    const amount = Number(rawValue);
    if (!Number.isFinite(amount) || Number.isNaN(amount) || amount <= 0) {
      return null;
    }
    return Math.floor(amount);
  };


  const startKakaoPayCharge = async () => {
    const amount = validateAmount(payAmount);
    if (!amount) {
      openModal("입력 확인", "결제 금액은 1원 이상이어야 합니다.");
      return;
    }
    setLoading(true);
    try {
      const ready = await api("/api/payments/kakaopay/ready", {
        method: "POST",
        body: JSON.stringify({ amount }),
      });
      localStorage.setItem("kakaopay_order_id", ready.orderId);
      window.location.href = ready.redirectUrl;
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      openModal("결제 요청 실패", error.message);
      setLoading(false);
    }
  };


  const requestWithdrawal = async () => {
    const amount = validateAmount(withdrawForm.amount);
    if (!amount || amount < 1000) {
      openModal("입력 확인", "출금 금액은 1,000원 이상이어야 합니다.");
      return;
    }
    if (!withdrawForm.bankName.trim() || !withdrawForm.accountNumber.trim() || !withdrawForm.accountHolder.trim()) {
      openModal("입력 확인", "은행, 계좌번호, 예금주를 모두 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api("/api/mileage/withdrawals", {
        method: "POST",
        body: JSON.stringify({
          amount,
          bankName: withdrawForm.bankName,
          accountNumber: withdrawForm.accountNumber,
          accountHolder: withdrawForm.accountHolder,
        }),
      });
      setWithdrawForm({ amount: "", bankName: "", accountNumber: "", accountHolder: "" });
      await loadMileage();
      openModal("출금 요청 완료", "출금 요청이 접수되었습니다. 관리자 승인 후 Mock 송금 완료 처리됩니다.");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      openModal("출금 요청 실패", error.message);
    } finally {
      setLoading(false);
    }
  };

  const clearPaymentQueryParams = useCallback(() => {
    const next = new URLSearchParams(searchParams);
    next.delete("pg_token");
    next.delete("orderId");
    next.delete("status");
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);

  const processPaymentRedirect = useCallback(async () => {
    const pgToken = searchParams.get("pg_token");
    const orderIdFromQuery = searchParams.get("orderId");
    const status = searchParams.get("status");
    const storedOrderId = localStorage.getItem("kakaopay_order_id");
    const orderId = orderIdFromQuery || storedOrderId;

    if (!pgToken && !status) {
      return;
    }
    if (!orderId) {
      openModal("결제 처리 안내", "주문 정보를 찾을 수 없어 결제 상태를 확인할 수 없습니다.");
      clearPaymentQueryParams();
      return;
    }

    setLoading(true);
    try {
      if (pgToken) {
        await api("/api/payments/kakaopay/approve", {
          method: "POST",
          body: JSON.stringify({ orderId, pgToken }),
        });
        openModal("결제 성공", "카카오페이 결제가 승인되어 마일리지가 충전되었습니다.");
      } else if (status === "cancel") {
        await api("/api/payments/kakaopay/cancel", {
          method: "POST",
          body: JSON.stringify({ orderId, reason: "USER_CANCELED" }),
        });
        openModal("결제 취소", "결제가 취소되었습니다.");
      } else if (status === "fail") {
        await api("/api/payments/kakaopay/fail", {
          method: "POST",
          body: JSON.stringify({ orderId, reason: "PAYMENT_FAILED" }),
        });
        openModal("결제 실패", "결제 처리에 실패했습니다.");
      }
      localStorage.removeItem("kakaopay_order_id");
      clearPaymentQueryParams();
      await loadMileage();
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      openModal("결제 처리 오류", error.message || "결제 상태 처리 중 오류가 발생했습니다.");
      clearPaymentQueryParams();
    } finally {
      setLoading(false);
    }
  }, [clearPaymentQueryParams, handleUnauthorized, loadMileage, openModal, searchParams]);

  useEffect(() => {
    loadMileage();
  }, [loadMileage]);

  useEffect(() => {
    processPaymentRedirect();
  }, [processPaymentRedirect]);

  const paymentStatusLabel = (status) => ({
    PENDING: "결제 대기",
    APPROVED: "승인 완료",
    FAILED: "실패",
    CANCELED: "취소",
  }[status] ?? status);

  const withdrawalStatusLabel = (status) => ({
    REQUESTED: "요청",
    COMPLETED: "완료",
    REJECTED: "반려",
  }[status] ?? status);
  const shouldShowMessage = loading || message.includes("실패") || message.includes("오류") || message.includes("불러오는 중");

  return (
    <main className="container">
      <h1>마일리지</h1>

      <div className="card">
        <h2>잔액</h2>
        {account ? (
          <>
            <p className="meta">총 잔액: {formatNumber(account.balance)}원</p>
            <p className="meta">예약 잔액: {formatNumber(account.reservedBalance)}원</p>
            <p className="meta">출금 대기: {formatNumber(account.withdrawPendingBalance)}원</p>
            <p className="meta">사용 가능: {formatNumber(account.availableBalance)}원</p>
          </>
        ) : (
          <p className="meta">잔액 정보 없음</p>
        )}
        {shouldShowMessage ? <p>{loading ? "요청 처리 중..." : message}</p> : null}
      </div>


      <div className="card">
        <h2>카카오페이 충전</h2>
        <input type="number" min="1" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} disabled={loading} placeholder="결제 금액" />
        <div className="actions">
          <button onClick={startKakaoPayCharge} disabled={loading}>카카오페이 결제</button>
        </div>
      </div>


      <div className="card">
        <h2>출금 요청</h2>
        <input type="number" min="1000" value={withdrawForm.amount} onChange={(e) => setWithdrawForm((prev) => ({ ...prev, amount: e.target.value }))} disabled={loading} placeholder="출금 금액" />
        <select className="select" value={withdrawForm.bankName} onChange={(e) => setWithdrawForm((prev) => ({ ...prev, bankName: e.target.value }))} disabled={loading}>
          <option value="">은행선택</option>
          {BANK_OPTIONS.map((bankName) => (
            <option key={bankName} value={bankName}>{bankName}</option>
          ))}
        </select>
        <input value={withdrawForm.accountNumber} onChange={(e) => setWithdrawForm((prev) => ({ ...prev, accountNumber: e.target.value }))} disabled={loading} placeholder="계좌번호" />
        <input value={withdrawForm.accountHolder} onChange={(e) => setWithdrawForm((prev) => ({ ...prev, accountHolder: e.target.value }))} disabled={loading} placeholder="예금주" />
        <p className="meta">실제 송금은 발생하지 않으며, 관리자 승인 후 Mock 송금 완료로 처리됩니다.</p>
        <div className="actions">
          <button onClick={requestWithdrawal} disabled={loading}>출금 요청</button>
        </div>
      </div>

      <div className="card">
        <h2>출금 내역</h2>
        {withdrawals.length === 0 ? (
          <p className="empty-box">출금 내역이 없습니다.</p>
        ) : (
          <ul className="list">
            {withdrawals.map((item) => (
              <li key={item.id} className="list-item">
                <span>{withdrawalStatusLabel(item.status)}</span>
                <span>금액: {formatNumber(item.amount)}원</span>
                <span>계좌: {item.bankName} {item.accountNumberMasked}</span>
                <span className="meta">요청: {formatDateTime(item.requestedAt)}</span>
                {item.completedAt ? <span className="meta">완료: {formatDateTime(item.completedAt)}</span> : null}
                {item.rejectedAt ? <span className="meta">반려: {formatDateTime(item.rejectedAt)}</span> : null}
                {item.rejectReason ? <span className="meta">사유: {item.rejectReason}</span> : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card">
        <h2>마일리지 내역</h2>
        {ledger.length === 0 ? (
          <p className="empty-box">내역이 없습니다.</p>
        ) : (
          <ul className="list">
            {ledger.map((item) => (
              <li key={item.id} className="list-item">
                <span>{LEDGER_TYPE_LABELS[item.type] ?? item.type}</span>
                <span>금액: {formatNumber(item.amount)}원</span>
                <span>잔액: {formatNumber(item.balanceAfter)}원</span>
                <span>예약: {formatNumber(item.reservedAfter)}원</span>
                <span className="meta">참조: {item.refType} #{item.refId}</span>
                <span className="meta">{formatDateTime(item.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card">
        <h2>결제 내역</h2>
        {payments.length === 0 ? (
          <p className="empty-box">결제 내역이 없습니다.</p>
        ) : (
          <ul className="list">
            {payments.map((payment) => (
              <li key={payment.id} className="list-item">
                <span>{paymentStatusLabel(payment.status)}</span>
                <span>결제 금액: {formatNumber(payment.amount)}원</span>
                <span className="meta">주문번호: {payment.orderId}</span>
                <span className="meta">생성: {formatDateTime(payment.createdAt)}</span>
                {payment.approvedAt ? <span className="meta">승인: {formatDateTime(payment.approvedAt)}</span> : null}
                {payment.failedAt ? <span className="meta">실패: {formatDateTime(payment.failedAt)}</span> : null}
                {payment.canceledAt ? <span className="meta">취소: {formatDateTime(payment.canceledAt)}</span> : null}
                {payment.failReason ? <span className="meta">사유: {payment.failReason}</span> : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      {modalState.open ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>{modalState.title}</h2>
            <p>{modalState.body}</p>
            <div className="actions">
              <button onClick={() => setModalState({ open: false, title: "", body: "" })}>확인</button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

export default MileagePage;
