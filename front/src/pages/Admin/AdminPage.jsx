import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import ConfirmModal from "../../components/ConfirmModal";
import { api } from "../../lib/api";
import { canManageRoles } from "../../lib/permissions";
import AdminAuditSection from "./AdminAuditSection";
import AdminCategoriesSection from "./AdminCategoriesSection";
import AdminDashboardSection from "./AdminDashboardSection";
import AdminOperationsSection from "./AdminOperationsSection";
import AdminRestrictionsSection from "./AdminRestrictionsSection";
import AdminTabs from "./AdminTabs";
import AdminUserDetailModal from "./AdminUserDetailModal";
import AdminUsersSection from "./AdminUsersSection";
import AdminWithdrawalsSection from "./AdminWithdrawalsSection";
import { RESTRICTION_LABELS, ROLE_OPTIONS, STATUS_OPTIONS, toDateTimeLocal } from "./adminUtils";

import "../../css/pages/AdminPage.css";
const ADMIN_TABS = new Set(["dashboard", "users", "restrictions", "categories", "operations", "withdrawals", "audit"]);
const USER_SEARCH_FIELDS = new Set(["email", "nickname", "name", "phoneNumber"]);

const readAdminFilters = (searchParams) => ({
  tab: ADMIN_TABS.has(searchParams.get("tab")) ? searchParams.get("tab") : "dashboard",
  userSearch: {
    field: USER_SEARCH_FIELDS.has(searchParams.get("userField")) ? searchParams.get("userField") : "email",
    keyword: searchParams.get("userKeyword") || "",
    role: ROLE_OPTIONS.includes(searchParams.get("userRole")) ? searchParams.get("userRole") : "ALL",
    status: STATUS_OPTIONS.includes(searchParams.get("userStatus")) ? searchParams.get("userStatus") : "ALL",
  },
});

const buildAdminSearchParams = (tab, userSearch) => {
  const params = new URLSearchParams();
  if (tab !== "dashboard") {
    params.set("tab", tab);
  }
  if (userSearch.field !== "email") {
    params.set("userField", userSearch.field);
  }
  if (userSearch.keyword.trim()) {
    params.set("userKeyword", userSearch.keyword.trim());
  }
  if (userSearch.role !== "ALL") {
    params.set("userRole", userSearch.role);
  }
  if (userSearch.status !== "ALL") {
    params.set("userStatus", userSearch.status);
  }
  return params;
};

export default function AdminPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialFilters = readAdminFilters(searchParams);
  const [activeTab, setActiveTab] = useState(initialFilters.tab);
  const [dashboard, setDashboard] = useState(null);
  const [users, setUsers] = useState([]);
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [auctions, setAuctions] = useState([]);
  const [restrictions, setRestrictions] = useState([]);
  const [reports, setReports] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [withdrawals, setWithdrawals] = useState([]);
  const [categoryForm, setCategoryForm] = useState({ code: "", name: "", displayOrder: 10, active: true });
  const [restrictionModal, setRestrictionModal] = useState(null);
  const [confirmModal, setConfirmModal] = useState(null);
  const [resultModal, setResultModal] = useState(null);
  const [withdrawalRejectModal, setWithdrawalRejectModal] = useState(null);
  const [mileageAdjustModal, setMileageAdjustModal] = useState(null);
  const [userEditModal, setUserEditModal] = useState(null);
  const [userSearch, setUserSearch] = useState(initialFilters.userSearch);
  const [currentAdmin, setCurrentAdmin] = useState(null);
  const [message, setMessage] = useState("관리자 데이터를 불러오는 중입니다.");
  const [loading, setLoading] = useState(false);

  const showResult = (title, body) => {
    setResultModal({ title, body });
  };

  const filteredUsers = useMemo(() => {
    const keyword = userSearch.keyword.trim().toLowerCase();
    return users.filter((user) => {
      if (userSearch.role !== "ALL" && user.role !== userSearch.role) {
        return false;
      }
      if (userSearch.status === "NORMAL" && user.banned) {
        return false;
      }
      if (userSearch.status === "BANNED" && !user.banned) {
        return false;
      }
      if (!keyword) {
        return true;
      }
      const valueByField = {
        email: user.email,
        nickname: user.nickname,
        name: user.name,
        phoneNumber: user.phoneNumber,
      };
      return String(valueByField[userSearch.field] ?? "").toLowerCase().includes(keyword);
    });
  }, [users, userSearch]);

  const loadAdminData = async ({ silent = false } = {}) => {
    setLoading(true);
    try {
      const [meRes, dashboardRes, usersRes, categoriesRes, productsRes, auctionsRes, restrictionsRes, reportsRes, logsRes, withdrawalsRes] = await Promise.all([
        api("/api/auth/me"),
        api("/api/admin/dashboard"),
        api("/api/admin/users"),
        api("/api/admin/categories"),
        api("/api/admin/products"),
        api("/api/admin/auctions"),
        api("/api/admin/restrictions"),
        api("/api/admin/reports"),
        api("/api/admin/audit-logs"),
        api("/api/admin/mileage/withdrawals"),
      ]);
      setCurrentAdmin(meRes);
      setDashboard(dashboardRes);
      setUsers(usersRes);
      setCategories(categoriesRes);
      setProducts(productsRes);
      setAuctions(auctionsRes);
      setRestrictions(restrictionsRes);
      setReports(reportsRes);
      setAuditLogs(logsRes);
      setWithdrawals(withdrawalsRes);
      setMessage("관리자 데이터가 갱신되었습니다.");
      if (!silent) {
        showResult("새로고침 완료", "관리자 데이터가 갱신되었습니다.");
      }
    } catch (error) {
      const body = `관리자 데이터 조회 실패: ${error.message}`;
      setMessage(body);
      if (!silent) {
        showResult("조회 실패", body);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData({ silent: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const nextFilters = readAdminFilters(searchParams);
    setActiveTab(nextFilters.tab);
    setUserSearch(nextFilters.userSearch);
  }, [searchParams]);

  const updateActiveTab = (nextTab) => {
    setActiveTab(nextTab);
    setSearchParams(buildAdminSearchParams(nextTab, userSearch));
  };

  const updateUserSearch = (updater) => {
    setUserSearch((prev) => {
      const next = typeof updater === "function" ? updater(prev) : updater;
      setSearchParams(buildAdminSearchParams(activeTab, next));
      return next;
    });
  };

  const refreshAfterAction = async (successTitle, successBody) => {
    await loadAdminData({ silent: true });
    showResult(successTitle, successBody);
  };

  const openUserEditModal = (user) => {
    setUserEditModal({
      id: user.id,
      email: user.email,
      name: user.name ?? "",
      nickname: user.nickname ?? "",
      phoneNumber: user.phoneNumber ?? "",
      role: user.role,
      banned: user.banned,
      bannedUntil: toDateTimeLocal(user.bannedUntil),
      banReason: user.banReason ?? "",
      createdAt: user.createdAt,
      mileageBalance: user.mileageBalance ?? 0,
      reservedMileage: user.reservedMileage ?? 0,
      availableMileage: user.availableMileage ?? 0,
    });
  };

  const closeConfirmModal = () => {
    if (!loading) {
      setConfirmModal(null);
    }
  };

  const runConfirmedAction = async () => {
    if (confirmModal?.onConfirm) {
      await confirmModal.onConfirm();
    }
  };

  const submitUserEditModal = async (event) => {
    event.preventDefault();
    if (!userEditModal) {
      return;
    }
    setLoading(true);
    try {
      await api(`/api/admin/users/${userEditModal.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          name: userEditModal.name,
          nickname: userEditModal.nickname,
          phoneNumber: userEditModal.phoneNumber,
          role: userEditModal.role,
          banned: userEditModal.banned,
          bannedUntil: userEditModal.banned ? userEditModal.bannedUntil || null : null,
          banReason: userEditModal.banned ? userEditModal.banReason : null,
        }),
      });
      setUserEditModal(null);
      await refreshAfterAction("회원 정보 수정 완료", "회원 정보가 저장되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("회원 정보 수정 실패", error.message);
    }
  };

  const updateUserRole = async (userId, role) => {
    setLoading(true);
    try {
      await api(`/api/admin/users/${userId}/role`, {
        method: "PATCH",
        body: JSON.stringify({ role }),
      });
      await refreshAfterAction("권한 변경 완료", "회원 권한이 변경되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("권한 변경 실패", error.message);
    }
  };

  const openLoginBanModal = (user) => {
    setRestrictionModal({
      mode: "LOGIN_BAN",
      userId: user.id,
      userNickname: user.nickname,
      title: "로그인 차단",
      reason: "운영 정책 위반",
      until: "",
    });
  };

  const openFeatureRestrictionModal = (user, type) => {
    setRestrictionModal({
      mode: "FEATURE_RESTRICTION",
      type,
      userId: user.id,
      userNickname: user.nickname,
      title: RESTRICTION_LABELS[type] ?? "기능 제한",
      reason: "운영 정책 위반",
      until: "",
    });
  };

  const openMileageAdjustModal = (user) => {
    setMileageAdjustModal({
      userId: user.id,
      userNickname: user.nickname,
      availableMileage: user.availableMileage ?? 0,
      type: "GRANT",
      amount: "",
      reason: "사기 피해 보상",
    });
  };

  const submitMileageAdjustModal = async () => {
    if (!mileageAdjustModal) {
      return;
    }
    const amount = Number(mileageAdjustModal.amount);
    const reason = mileageAdjustModal.reason.trim();
    if (!Number.isFinite(amount) || amount <= 0) {
      showResult("입력 확인", "마일리지 금액은 1P 이상 입력해주세요.");
      return;
    }
    if (!reason) {
      showResult("입력 확인", "마일리지 조정 사유를 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api(`/api/admin/users/${mileageAdjustModal.userId}/mileage-adjustments`, {
        method: "POST",
        body: JSON.stringify({
          type: mileageAdjustModal.type,
          amount: Math.floor(amount),
          reason,
        }),
      });
      setMileageAdjustModal(null);
      setUserEditModal(null);
      await refreshAfterAction("마일리지 조정 완료", "회원 마일리지가 조정되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("마일리지 조정 실패", error.message);
    }
  };

  const submitRestrictionModal = async () => {
    if (!restrictionModal) {
      return;
    }
    setLoading(true);
    try {
      if (restrictionModal.mode === "LOGIN_BAN") {
        await api(`/api/admin/users/${restrictionModal.userId}/ban`, {
          method: "PATCH",
          body: JSON.stringify({
            reason: restrictionModal.reason,
            bannedUntil: restrictionModal.until || null,
          }),
        });
      } else {
        await api(`/api/admin/users/${restrictionModal.userId}/restrictions`, {
          method: "POST",
          body: JSON.stringify({
            type: restrictionModal.type,
            reason: restrictionModal.reason,
            restrictedUntil: restrictionModal.until || null,
          }),
        });
      }
      setRestrictionModal(null);
      await refreshAfterAction("제재 처리 완료", "회원 제재가 적용되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("제재 처리 실패", error.message);
    }
  };

  const createCategory = async (event) => {
    event.preventDefault();
    setLoading(true);
    try {
      await api("/api/admin/categories", {
        method: "POST",
        body: JSON.stringify(categoryForm),
      });
      setCategoryForm({ code: "", name: "", displayOrder: 10, active: true });
      await refreshAfterAction("카테고리 생성 완료", "카테고리가 생성되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("카테고리 생성 실패", error.message);
    }
  };

  const updateCategory = async (category, patch) => {
    setLoading(true);
    try {
      await api(`/api/admin/categories/${category.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          name: patch.name ?? category.name,
          displayOrder: patch.displayOrder ?? category.displayOrder,
          active: patch.active ?? category.active,
        }),
      });
      await refreshAfterAction("카테고리 수정 완료", "카테고리가 수정되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("카테고리 수정 실패", error.message);
    }
  };

  const confirmUnbanUser = (user) => {
    setConfirmModal({
      title: "로그인 차단 해제",
      message: `${user.nickname}님의 로그인 차단을 해제할까요?`,
      detail: "해제 후 해당 사용자는 다시 로그인할 수 있습니다.",
      confirmLabel: "해제",
      tone: "success",
      onConfirm: async () => {
        setLoading(true);
        try {
          await api(`/api/admin/users/${user.id}/unban`, { method: "PATCH" });
          setConfirmModal(null);
          await refreshAfterAction("제재 해제 완료", "로그인 차단이 해제되었습니다.");
        } catch (error) {
          setLoading(false);
          showResult("제재 해제 실패", error.message);
        }
      },
    });
  };

  const confirmDeactivateRestriction = (restriction) => {
    setConfirmModal({
      title: "기능 제한 해제",
      message: `${restriction.userNickname}님의 ${restriction.typeLabel} 제한을 해제할까요?`,
      detail: "해제 후 해당 기능을 다시 사용할 수 있습니다.",
      confirmLabel: "해제",
      tone: "success",
      onConfirm: async () => {
        setLoading(true);
        try {
          await api(`/api/admin/restrictions/${restriction.id}/deactivate`, { method: "PATCH" });
          setConfirmModal(null);
          await refreshAfterAction("기능 제한 해제 완료", "기능 제한이 해제되었습니다.");
        } catch (error) {
          setLoading(false);
          showResult("기능 제한 해제 실패", error.message);
        }
      },
    });
  };

  const confirmDeleteCategory = (category) => {
    setConfirmModal({
      title: "카테고리 삭제",
      message: `${category.name} 카테고리를 삭제할까요?`,
      detail: "관리자 카테고리 목록에서 제거됩니다.",
      confirmLabel: "삭제",
      onConfirm: async () => {
        setLoading(true);
        try {
          await api(`/api/admin/categories/${category.id}`, { method: "DELETE" });
          setConfirmModal(null);
          await refreshAfterAction("카테고리 삭제 완료", "카테고리가 삭제되었습니다.");
        } catch (error) {
          setLoading(false);
          showResult("카테고리 삭제 실패", error.message);
        }
      },
    });
  };

  const confirmDeleteProduct = (product) => {
    setConfirmModal({
      title: "상품 강제 삭제",
      message: `${product.title} 상품을 강제로 삭제할까요?`,
      detail: "삭제 후 사용자 상품 목록과 상세 페이지에서 사라집니다.",
      confirmLabel: "삭제",
      onConfirm: async () => {
        setLoading(true);
        try {
          await api(`/api/admin/products/${product.id}`, { method: "DELETE" });
          setConfirmModal(null);
          await refreshAfterAction("상품 삭제 완료", "상품이 삭제되었습니다.");
        } catch (error) {
          setLoading(false);
          showResult("상품 삭제 실패", error.message);
        }
      },
    });
  };

  const confirmCancelAuction = (auction) => {
    setConfirmModal({
      title: "경매 강제 마감",
      message: `${auction.productTitle} 경매를 강제로 마감할까요?`,
      detail: "진행 중인 입찰은 더 이상 접수되지 않습니다.",
      confirmLabel: "마감",
      onConfirm: async () => {
        setLoading(true);
        try {
          await api(`/api/admin/auctions/${auction.id}/cancel`, { method: "PATCH" });
          setConfirmModal(null);
          await refreshAfterAction("경매 마감 완료", "경매가 마감 처리되었습니다.");
        } catch (error) {
          setLoading(false);
          showResult("경매 마감 실패", error.message);
        }
      },
    });
  };

  const confirmCompleteWithdrawal = (withdrawal) => {
    setConfirmModal({
      title: "출금 완료 처리",
      message: `${withdrawal.userNickname}님의 ${Number(withdrawal.amount ?? 0).toLocaleString()}P 출금을 완료 처리할까요?`,
      detail: "현재 기능은 Mock 처리이며 실제 송금 API는 호출하지 않습니다.",
      confirmLabel: "완료 처리",
      tone: "success",
      onConfirm: async () => {
        setLoading(true);
        try {
          await api(`/api/admin/mileage/withdrawals/${withdrawal.id}/complete`, { method: "PATCH" });
          setConfirmModal(null);
          await refreshAfterAction("출금 완료 처리 완료", "출금 요청이 Mock 송금 완료로 처리되었습니다.");
        } catch (error) {
          setLoading(false);
          showResult("출금 완료 처리 실패", error.message);
        }
      },
    });
  };

  const openWithdrawalRejectModal = (withdrawal) => {
    setWithdrawalRejectModal({
      id: withdrawal.id,
      userNickname: withdrawal.userNickname,
      amount: withdrawal.amount ?? 0,
      reason: "계좌 정보 확인 필요",
    });
  };

  const submitWithdrawalRejectModal = async () => {
    if (!withdrawalRejectModal) {
      return;
    }
    const reason = withdrawalRejectModal.reason.trim();
    if (!reason) {
      showResult("입력 확인", "출금 반려 사유를 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      await api(`/api/admin/mileage/withdrawals/${withdrawalRejectModal.id}/reject`, {
        method: "PATCH",
        body: JSON.stringify({ reason }),
      });
      setWithdrawalRejectModal(null);
      await refreshAfterAction("출금 반려 완료", "출금 요청이 반려 처리되었습니다.");
    } catch (error) {
      setLoading(false);
      showResult("출금 반려 실패", error.message);
    }
  };

  return (
    <main className="container admin-container">
      <h1>관리자 대시보드</h1>
      <div className="card admin-toolbar-card">
        <AdminTabs activeTab={activeTab} onChange={updateActiveTab} />
        <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
          {loading ? "요청 처리 중입니다." : message}
        </p>
        <div className="actions">
          <button type="button" className="secondary-button" onClick={() => loadAdminData()} disabled={loading}>새로고침</button>
        </div>
      </div>

      {activeTab === "dashboard" ? <AdminDashboardSection dashboard={dashboard} /> : null}

      {activeTab === "users" ? <AdminUsersSection users={users} filteredUsers={filteredUsers} userSearch={userSearch} setUserSearch={updateUserSearch} loading={loading} updateUserRole={updateUserRole} openUserEditModal={openUserEditModal} unbanUser={confirmUnbanUser} openLoginBanModal={openLoginBanModal} canManageRoles={canManageRoles(currentAdmin)} /> : null}

      {activeTab === "restrictions" ? <AdminRestrictionsSection restrictions={restrictions} loading={loading} deactivateRestriction={confirmDeactivateRestriction} /> : null}

      {activeTab === "categories" ? <AdminCategoriesSection categories={categories} categoryForm={categoryForm} setCategoryForm={setCategoryForm} loading={loading} createCategory={createCategory} updateCategory={updateCategory} deleteCategory={confirmDeleteCategory} /> : null}

      {activeTab === "operations" ? <AdminOperationsSection products={products} auctions={auctions} loading={loading} deleteProduct={confirmDeleteProduct} cancelAuction={confirmCancelAuction} /> : null}

      {activeTab === "withdrawals" ? <AdminWithdrawalsSection withdrawals={withdrawals} loading={loading} completeWithdrawal={confirmCompleteWithdrawal} rejectWithdrawal={openWithdrawalRejectModal} /> : null}

      {activeTab === "audit" ? <AdminAuditSection dashboard={dashboard} auditLogs={auditLogs} /> : null}

      {userEditModal ? (
        <AdminUserDetailModal
          userEditModal={userEditModal}
          setUserEditModal={setUserEditModal}
          loading={loading}
          onSubmit={submitUserEditModal}
          onClose={() => setUserEditModal(null)}
          onRestrict={openFeatureRestrictionModal}
          onAdjustMileage={openMileageAdjustModal}
          products={products}
          auctions={auctions}
          restrictions={restrictions}
          reports={reports}
          auditLogs={auditLogs}
          canManageRoles={canManageRoles(currentAdmin)}
        />
      ) : null}

      {mileageAdjustModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>마일리지 조정</h2>
            <p className="meta">
              대상: {mileageAdjustModal.userNickname} #{mileageAdjustModal.userId} · 사용 가능 {Number(mileageAdjustModal.availableMileage).toLocaleString()}P
            </p>
            <label>조정 유형</label>
            <select
              value={mileageAdjustModal.type}
              onChange={(e) => setMileageAdjustModal((prev) => ({ ...prev, type: e.target.value }))}
              disabled={loading}
            >
              <option value="GRANT">지급</option>
              <option value="DEDUCT">차감</option>
            </select>
            <label>금액</label>
            <input
              type="number"
              min="1"
              step="1"
              value={mileageAdjustModal.amount}
              onChange={(e) => setMileageAdjustModal((prev) => ({ ...prev, amount: e.target.value }))}
              disabled={loading}
              placeholder="예: 10000"
            />
            <label>사유</label>
            <textarea
              className="textarea"
              value={mileageAdjustModal.reason}
              onChange={(e) => setMileageAdjustModal((prev) => ({ ...prev, reason: e.target.value }))}
              disabled={loading}
              maxLength={500}
            />
            <p className="meta">조정 내역은 회원 마일리지 내역과 관리자 감사 로그에 남습니다.</p>
            <div className="actions">
              <button type="button" className="secondary-button" onClick={() => setMileageAdjustModal(null)} disabled={loading}>
                취소
              </button>
              <button
                type="button"
                className={mileageAdjustModal.type === "DEDUCT" ? "danger-button" : undefined}
                onClick={submitMileageAdjustModal}
                disabled={loading}
              >
                {loading ? "처리 중..." : mileageAdjustModal.type === "DEDUCT" ? "차감 처리" : "지급 처리"}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {restrictionModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>{restrictionModal.title}</h2>
            <p className="meta">대상: {restrictionModal.userNickname} #{restrictionModal.userId}</p>
            <label>제재 사유</label>
            <textarea
              className="textarea"
              value={restrictionModal.reason}
              onChange={(e) => setRestrictionModal((prev) => ({ ...prev, reason: e.target.value }))}
              disabled={loading}
              maxLength={500}
            />
            <label>종료일시</label>
            <input
              type="datetime-local"
              value={restrictionModal.until}
              onChange={(e) => setRestrictionModal((prev) => ({ ...prev, until: e.target.value }))}
              disabled={loading}
            />
            <p className="meta">종료일시를 비워두면 무기한으로 적용됩니다.</p>
            <div className="actions">
              <button type="button" onClick={submitRestrictionModal} disabled={loading}>적용</button>
              <button type="button" className="secondary-button" onClick={() => setRestrictionModal(null)} disabled={loading}>취소</button>
            </div>
          </div>
        </div>
      ) : null}

      {withdrawalRejectModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card">
            <h2>출금 반려</h2>
            <p className="meta">
              대상: {withdrawalRejectModal.userNickname} / 금액: {Number(withdrawalRejectModal.amount).toLocaleString()}P
            </p>
            <label>반려 사유</label>
            <textarea
              className="textarea"
              value={withdrawalRejectModal.reason}
              onChange={(e) => setWithdrawalRejectModal((prev) => ({ ...prev, reason: e.target.value }))}
              disabled={loading}
              maxLength={500}
            />
            <div className="actions">
              <button type="button" className="secondary-button" onClick={() => setWithdrawalRejectModal(null)} disabled={loading}>
                취소
              </button>
              <button type="button" className="danger-button" onClick={submitWithdrawalRejectModal} disabled={loading}>
                {loading ? "처리 중..." : "반려 처리"}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {confirmModal ? (
        <ConfirmModal
          title={confirmModal.title}
          message={confirmModal.message}
          detail={confirmModal.detail}
          confirmLabel={confirmModal.confirmLabel}
          tone={confirmModal.tone}
          loading={loading}
          onCancel={closeConfirmModal}
          onConfirm={runConfirmedAction}
        />
      ) : null}

      {resultModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card signup-alert-modal">
            <h2>{resultModal.title}</h2>
            <p>{resultModal.body}</p>
            <div className="actions">
              <button type="button" onClick={() => setResultModal(null)}>확인</button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
} 
