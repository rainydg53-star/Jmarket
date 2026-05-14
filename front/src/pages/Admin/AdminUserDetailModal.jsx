import { useMemo, useState } from "react";
import { getAuctionStatusLabel, getAuctionStatusTone } from "../../lib/auctionStatus";
import { formatAuditAction, formatDateTime, formatNumber } from "./adminUtils";

const REPORT_STATUS_LABELS = {
  PENDING: "대기",
  RESOLVED: "처리완료",
  REJECTED: "반려",
};

const REPORT_TARGET_LABELS = {
  USER: "회원",
  PRODUCT: "상품",
  TRADE: "거래",
  AUCTION: "경매",
  CHAT: "채팅",
};

function EmptyState({ children }) {
  return <p className="meta admin-detail-empty">{children}</p>;
}

export default function AdminUserDetailModal({
  userEditModal,
  setUserEditModal,
  loading,
  onSubmit,
  onClose,
  onRestrict,
  onAdjustMileage,
  products,
  auctions,
  restrictions,
  reports,
  auditLogs,
}) {
  const [tab, setTab] = useState("basic");
  const userNickname = userEditModal.nickname;
  const userProducts = useMemo(
    () => products.filter((product) => product.sellerNickname === userNickname),
    [products, userNickname]
  );
  const userAuctions = useMemo(
    () => auctions.filter((auction) => auction.sellerNickname === userNickname),
    [auctions, userNickname]
  );
  const userRestrictions = useMemo(
    () => restrictions.filter((restriction) => restriction.userId === userEditModal.id),
    [restrictions, userEditModal.id]
  );
  const userReports = useMemo(
    () => reports.filter((report) => (
      report.reporterId === userEditModal.id
      || report.targetOwnerUserId === userEditModal.id
      || (report.targetType === "USER" && report.targetId === userEditModal.id)
    )),
    [reports, userEditModal.id]
  );
  const userAuditLogs = useMemo(
    () => auditLogs.filter((log) => log.targetType === "USER" && log.targetId === userEditModal.id),
    [auditLogs, userEditModal.id]
  );
  const soldProductCount = userProducts.filter((product) => product.sold).length;
  const openAuctionCount = userAuctions.filter((auction) => auction.status === "OPEN").length;
  const activeRestrictionCount = userRestrictions.filter((restriction) => restriction.active).length;
  const pendingReportCount = userReports.filter((report) => report.status === "PENDING").length;

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <form className="modal-card admin-user-detail-modal" onSubmit={onSubmit}>
        <div className="admin-detail-header">
          <div>
            <h2>회원 상세/수정</h2>
            <p className="meta">{userEditModal.email} · #{userEditModal.id}</p>
          </div>
          <span className={`status-badge ${userEditModal.banned ? "danger" : "success"}`}>
            {userEditModal.banned ? "차단" : "정상"}
          </span>
        </div>

        <div className="admin-detail-summary">
          <div>
            <span className="meta">사용 가능</span>
            <strong>{formatNumber(userEditModal.availableMileage)} P</strong>
          </div>
          <div>
            <span className="meta">등록 상품</span>
            <strong>{formatNumber(userProducts.length)}개</strong>
          </div>
          <div>
            <span className="meta">경매</span>
            <strong>{formatNumber(userAuctions.length)}개</strong>
          </div>
          <div>
            <span className="meta">미처리 신고</span>
            <strong>{formatNumber(pendingReportCount)}건</strong>
          </div>
          <div>
            <span className="meta">활성 제재</span>
            <strong>{formatNumber(activeRestrictionCount)}건</strong>
          </div>
        </div>

        <div className="pill-tabs admin-detail-tabs">
          {[
            ["basic", "기본정보"],
            ["activity", "활동요약"],
            ["reports", "신고"],
            ["restrictions", "제재/로그"],
          ].map(([key, label]) => (
            <button key={key} type="button" className={`pill-tab${tab === key ? " active" : ""}`} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </div>

        {tab === "basic" ? (
          <>
            <div className="admin-user-edit-grid">
              <label>
                이메일
                <input value={userEditModal.email} disabled />
              </label>
              <label>
                가입일
                <input value={formatDateTime(userEditModal.createdAt)} disabled />
              </label>
              <label>
                이름
                <input value={userEditModal.name} onChange={(e) => setUserEditModal((prev) => ({ ...prev, name: e.target.value }))} />
              </label>
              <label>
                닉네임
                <input required value={userEditModal.nickname} onChange={(e) => setUserEditModal((prev) => ({ ...prev, nickname: e.target.value }))} />
              </label>
              <label>
                휴대폰 번호
                <input value={userEditModal.phoneNumber} onChange={(e) => setUserEditModal((prev) => ({ ...prev, phoneNumber: e.target.value }))} />
              </label>
              <label>
                역할
                <select value={userEditModal.role} onChange={(e) => setUserEditModal((prev) => ({ ...prev, role: e.target.value }))}>
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </label>
            </div>
            <div className="admin-mileage-summary">
              <div>
                <span className="meta">사용 가능</span>
                <strong>{formatNumber(userEditModal.availableMileage)} P</strong>
              </div>
              <div>
                <span className="meta">총 보유</span>
                <strong>{formatNumber(userEditModal.mileageBalance)} P</strong>
              </div>
              <div>
                <span className="meta">예약</span>
                <strong>{formatNumber(userEditModal.reservedMileage)} P</strong>
              </div>
            </div>
            <div className="actions admin-mileage-actions">
              <button type="button" onClick={() => onAdjustMileage(userEditModal)} disabled={loading}>
                마일리지 조정
              </button>
            </div>
            <label className="inline-check">
              <input
                type="checkbox"
                checked={userEditModal.banned}
                onChange={(e) => setUserEditModal((prev) => ({ ...prev, banned: e.target.checked }))}
              />
              로그인 차단
            </label>
            {userEditModal.banned ? (
              <div className="admin-user-edit-grid">
                <label>
                  차단 종료일
                  <input
                    type="datetime-local"
                    value={userEditModal.bannedUntil}
                    onChange={(e) => setUserEditModal((prev) => ({ ...prev, bannedUntil: e.target.value }))}
                  />
                </label>
                <label>
                  차단 사유
                  <input
                    value={userEditModal.banReason}
                    onChange={(e) => setUserEditModal((prev) => ({ ...prev, banReason: e.target.value }))}
                  />
                </label>
              </div>
            ) : null}
          </>
        ) : null}

        {tab === "activity" ? (
          <div className="admin-detail-columns">
            <section>
              <h3>상품 활동</h3>
              <p className="meta">등록 {formatNumber(userProducts.length)}개 · 거래완료 {formatNumber(soldProductCount)}개</p>
              {userProducts.length === 0 ? <EmptyState>등록한 상품이 없습니다.</EmptyState> : (
                <ul className="list admin-detail-list">
                  {userProducts.slice(0, 5).map((product) => (
                    <li className="list-item" key={product.id}>
                      <strong>{product.title}</strong>
                      <span className="meta">{product.categoryLabel} · {formatNumber(product.price)}원 · {product.sold ? "거래완료" : "판매중"}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section>
              <h3>경매 활동</h3>
              <p className="meta">등록 {formatNumber(userAuctions.length)}개 · 진행중 {formatNumber(openAuctionCount)}개</p>
              {userAuctions.length === 0 ? <EmptyState>등록한 경매가 없습니다.</EmptyState> : (
                <ul className="list admin-detail-list">
                  {userAuctions.slice(0, 5).map((auction) => (
                    <li className="list-item" key={auction.id}>
                      <strong>{auction.productTitle}</strong>
                      <span className="meta">
                        <span className={`status-badge ${getAuctionStatusTone(auction.status)}`}>{getAuctionStatusLabel(auction.status)}</span>
                        {" "}현재가 {formatNumber(auction.currentPrice)}원 · 종료 {formatDateTime(auction.endAt)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        ) : null}

        {tab === "reports" ? (
          <section>
            <h3>신고 관련 내역</h3>
            {userReports.length === 0 ? <EmptyState>관련 신고가 없습니다.</EmptyState> : (
              <ul className="list admin-detail-list">
                {userReports.slice(0, 8).map((report) => (
                  <li className="list-item" key={report.id}>
                    <strong>{REPORT_TARGET_LABELS[report.targetType] ?? report.targetType} · {REPORT_STATUS_LABELS[report.status] ?? report.status}</strong>
                    <span className="meta">신고자: {report.reporterNickname} · 대상: {report.targetSummary ?? report.targetOwnerNickname ?? report.targetId}</span>
                    <span className="meta">사유: {report.reason} · {formatDateTime(report.createdAt)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        ) : null}

        {tab === "restrictions" ? (
          <div className="admin-detail-columns">
            <section>
              <h3>기능 제재</h3>
              {userRestrictions.length === 0 ? <EmptyState>제재 내역이 없습니다.</EmptyState> : (
                <ul className="list admin-detail-list">
                  {userRestrictions.map((restriction) => (
                    <li className="list-item" key={restriction.id}>
                      <strong>{restriction.typeLabel}</strong>
                      <span className="meta">{restriction.active ? "적용중" : "만료/해제"} · {restriction.reason ?? "-"}</span>
                      <span className="meta">등록 {formatDateTime(restriction.createdAt)} · 종료 {formatDateTime(restriction.restrictedUntil)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section>
              <h3>관리자 조치 로그</h3>
              {userAuditLogs.length === 0 ? <EmptyState>관리자 조치 로그가 없습니다.</EmptyState> : (
                <ul className="list admin-detail-list">
                  {userAuditLogs.slice(0, 8).map((log) => (
                    <li className="list-item" key={log.id}>
                      <strong>{formatAuditAction(log.action)}</strong>
                      <span className="meta">{log.adminNickname} · {formatDateTime(log.createdAt)}</span>
                      <span className="meta">{log.memo ?? "-"}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        ) : null}

        <div className="actions">
          <button type="submit" disabled={loading}>저장</button>
          <button type="button" className="secondary-button" onClick={onClose} disabled={loading}>취소</button>
          <button type="button" onClick={() => onRestrict(userEditModal, "PRODUCT_CREATE")} disabled={loading}>상품등록 제한</button>
          <button type="button" onClick={() => onRestrict(userEditModal, "AUCTION_CREATE")} disabled={loading}>경매등록 제한</button>
          <button type="button" onClick={() => onRestrict(userEditModal, "AUCTION_BID")} disabled={loading}>입찰 제한</button>
        </div>
      </form>
    </div>
  );
}
