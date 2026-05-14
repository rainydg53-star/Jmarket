import { formatAdminJoinDate, formatDateTime, formatNumber, ROLE_OPTIONS, STATUS_OPTIONS } from "./adminUtils";

export default function AdminUsersSection({ users, filteredUsers, userSearch, setUserSearch, loading, updateUserRole, openUserEditModal, unbanUser, openLoginBanModal }) {
  return (
        <section className="card">
          <div className="admin-section-head">
            <div>
              <h2>회원 조회/관리</h2>
              <p className="meta">총 {formatNumber(users.length)}명 · 표시 {formatNumber(filteredUsers.length)}명</p>
            </div>
          </div>
          <div className="admin-user-search">
            <select value={userSearch.field} onChange={(e) => setUserSearch((prev) => ({ ...prev, field: e.target.value }))}>
              <option value="email">이메일</option>
              <option value="nickname">닉네임</option>
              <option value="name">이름</option>
              <option value="phoneNumber">휴대폰 번호</option>
            </select>
            <input
              value={userSearch.keyword}
              onChange={(e) => setUserSearch((prev) => ({ ...prev, keyword: e.target.value }))}
              placeholder="검색어를 입력하세요."
            />
            <select value={userSearch.role} onChange={(e) => setUserSearch((prev) => ({ ...prev, role: e.target.value }))}>
              {ROLE_OPTIONS.map((role) => (
                <option key={role} value={role}>{role === "ALL" ? "전체 역할" : role}</option>
              ))}
            </select>
            <select value={userSearch.status} onChange={(e) => setUserSearch((prev) => ({ ...prev, status: e.target.value }))}>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status === "ALL" ? "전체 상태" : status === "NORMAL" ? "정상" : "차단"}
                </option>
              ))}
            </select>
            <button type="button" className="secondary-button" onClick={() => setUserSearch({ field: "email", keyword: "", role: "ALL", status: "ALL" })}>
              초기화
            </button>
          </div>
          <div className="admin-table-wrap">
            <table className="admin-user-table">
              <thead>
                <tr>
                  <th>이메일</th>
                  <th>이름/닉네임</th>
                  <th>휴대폰</th>
                  <th>역할</th>
                  <th>상태</th>
                  <th>가입일</th>
                  <th>마일리지</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td>{user.email}</td>
                    <td>{user.name ?? "-"} / {user.nickname}</td>
                    <td>{user.phoneNumber ?? "-"}</td>
                    <td>
                      <button
                        type="button"
                        className="mini-button"
                        onClick={() => updateUserRole(user.id, user.role === "ADMIN" ? "USER" : "ADMIN")}
                        disabled={loading}
                      >
                        {user.role}
                      </button>
                    </td>
                    <td>
                      <span className={`status-badge ${user.banned ? "danger" : "success"}`}>
                        {user.banned ? "차단" : "정상"}
                      </span>
                    </td>
                    <td title={formatDateTime(user.createdAt)}>{formatAdminJoinDate(user.createdAt)}</td>
                    <td>
                      <strong>{formatNumber(user.availableMileage)} P</strong>
                      <span className="meta">보유 {formatNumber(user.mileageBalance)} · 예약 {formatNumber(user.reservedMileage)}</span>
                    </td>
                    <td>
                      <div className="actions compact-actions">
                        <button type="button" onClick={() => openUserEditModal(user)} disabled={loading}>상세/수정</button>
                        {user.banned ? (
                          <button type="button" onClick={() => unbanUser(user)} disabled={loading}>해제</button>
                        ) : (
                          <button type="button" className="danger-button" onClick={() => openLoginBanModal(user)} disabled={loading}>차단</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="empty-table-cell">검색 결과가 없습니다.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>
  );
}
