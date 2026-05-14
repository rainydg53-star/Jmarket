const AUCTION_STATUS_LABELS = {
  OPEN: "진행중",
  CLOSED: "마감",
  CANCELLED: "취소",
  CANCELED: "취소",
};

const AUCTION_STATUS_TONES = {
  OPEN: "success",
  CLOSED: "danger",
  CANCELLED: "muted",
  CANCELED: "muted",
};

export const getAuctionStatusLabel = (status) => AUCTION_STATUS_LABELS[status] ?? status ?? "-";

export const getAuctionStatusTone = (status) => AUCTION_STATUS_TONES[status] ?? "muted";

export const isAuctionTimeExpired = (auction, now = Date.now()) => {
  if (!auction?.endAt || auction.status !== "OPEN") {
    return false;
  }
  const endTime = new Date(auction.endAt).getTime();
  return Number.isFinite(endTime) && endTime <= now;
};

export const getAuctionDisplayStatusInfo = (auction, now = Date.now()) => {
  if (isAuctionTimeExpired(auction, now)) {
    return { label: "마감 확인중", tone: "warning" };
  }
  return {
    label: getAuctionStatusLabel(auction?.status),
    tone: getAuctionStatusTone(auction?.status),
  };
};

export const getAuctionRemainingTimeInfo = (auction, now = Date.now()) => {
  if (!auction) {
    return { label: "-", tone: "muted" };
  }

  if (auction.status === "CLOSED") {
    return { label: "마감됨", tone: "muted" };
  }

  if (auction.status === "CANCELLED" || auction.status === "CANCELED") {
    return { label: "취소됨", tone: "muted" };
  }

  if (!auction.endAt) {
    return { label: "종료 시간 없음", tone: "muted" };
  }

  const endTime = new Date(auction.endAt).getTime();
  if (!Number.isFinite(endTime)) {
    return { label: "종료 시간 확인 필요", tone: "muted" };
  }

  const diffMs = endTime - now;
  if (diffMs <= 0) {
    return { label: "마감 확인중", tone: "warning" };
  }

  const totalMinutes = Math.ceil(diffMs / 60000);
  if (totalMinutes <= 1) {
    return { label: "곧 마감", tone: "danger" };
  }

  if (totalMinutes < 60) {
    return { label: `${totalMinutes}분 남음`, tone: "danger" };
  }

  const totalHours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (totalHours < 24) {
    return { label: `${totalHours}시간 ${minutes}분 남음`, tone: totalHours < 3 ? "danger" : "warning" };
  }

  const days = Math.floor(totalHours / 24);
  const hours = totalHours % 24;
  return { label: `${days}일 ${hours}시간 남음`, tone: days <= 1 ? "warning" : "success" };
};
