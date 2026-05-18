export const formatNumber = (value) => Number(value ?? 0).toLocaleString();

export const formatShortDate = (value) => {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(`${value}T00:00:00`));
};

export const formatDateTime = (value) => {
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
  }).format(new Date(value));
};

export const formatAdminJoinDate = (value) => {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  const now = new Date();
  const sameYear = new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
  }).format(date) === new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
  }).format(now);

  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    ...(sameYear ? {} : { year: "numeric" }),
    month: "2-digit",
    day: "2-digit",
  }).format(date);
};

export const toDateTimeLocal = (value) => {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

export const ROLE_OPTIONS = ["ALL", "USER", "ADMIN", "SUPER_ADMIN"];
export const STATUS_OPTIONS = ["ALL", "NORMAL", "BANNED"];

export const RESTRICTION_LABELS = {
  PRODUCT_CREATE: "상품등록 제한",
  AUCTION_CREATE: "경매등록 제한",
  AUCTION_BID: "입찰 제한",
};

export const AUDIT_ACTION_LABELS = {
  USER_LIST_VIEW: "회원 목록 조회",
  USER_ROLE_UPDATE: "회원 권한 변경",
  USER_PROFILE_UPDATE: "회원 정보 수정",
  USER_BAN: "회원 로그인 차단",
  USER_UNBAN: "회원 로그인 차단 해제",
  USER_FEATURE_RESTRICT: "회원 기능 제한",
  USER_FEATURE_RESTRICTION_RELEASE: "회원 기능 제한 해제",
  CATEGORY_CREATE: "카테고리 생성",
  CATEGORY_UPDATE: "카테고리 수정",
  CATEGORY_DELETE: "카테고리 삭제",
  PRODUCT_DELETE: "상품 강제 삭제",
  AUCTION_CANCEL: "경매 강제 마감",
  AUCTION_HIDE: "경매 강제 삭제",
  MILEAGE_ADMIN_GRANT: "마일리지 관리자 지급",
  MILEAGE_ADMIN_DEDUCT: "마일리지 관리자 차감",
  MILEAGE_WITHDRAWAL_COMPLETE: "출금 완료 처리",
  MILEAGE_WITHDRAWAL_REJECT: "출금 반려 처리",
};

export const AUDIT_TARGET_LABELS = {
  USER: "회원",
  USER_RESTRICTION: "기능 제한",
  CATEGORY: "카테고리",
  PRODUCT: "상품",
  AUCTION: "경매",
  MILEAGE_WITHDRAWAL: "출금",
};

export const formatAuditAction = (action) => AUDIT_ACTION_LABELS[action] ?? action ?? "-";

export const formatAuditTarget = (targetType) => AUDIT_TARGET_LABELS[targetType] ?? targetType ?? "-";
