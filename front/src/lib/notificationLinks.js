const REPORT_ID_PATTERN = /신고\s*#?(\d+)|#(\d+)/;

const withReportMode = (link, notification) => {
  const rawText = `${notification?.title ?? ""} ${notification?.message ?? ""}`;
  const match = rawText.match(REPORT_ID_PATTERN);
  const reportId = match?.[1] ?? match?.[2];
  const params = new URLSearchParams({ mode: "list" });
  if (reportId) {
    params.set("reportId", reportId);
  }
  return `${link || "/reports"}?${params.toString()}`;
};

export const resolveNotificationLink = (notification) => {
  const link = notification?.link;

  if (notification?.type === "REPORT_RESOLVED") {
    return withReportMode(link, notification);
  }

  if (notification?.type === "USER_RESTRICTED") {
    return link || "/me";
  }

  if (notification?.type === "CHAT_MESSAGE") {
    return link || "/chat/rooms";
  }

  if (notification?.type === "MILEAGE_WITHDRAWAL_REQUESTED") {
    return link || "/admin";
  }

  if (notification?.type?.startsWith("AUCTION_")) {
    return link || "/auctions/products";
  }

  if (notification?.type?.startsWith("TRADE_")) {
    return link || "/trades";
  }

  if (notification?.type === "REVIEW_RECEIVED") {
    return link || "/me";
  }

  return link || null;
};

export const getNotificationLinkLabel = (notification) => {
  switch (notification?.type) {
    case "AUCTION_OUTBID":
    case "AUCTION_OUTBID_LOST":
    case "AUCTION_WON":
    case "AUCTION_SOLD":
      return "경매 상세로 이동";
    case "TRADE_REQUESTED":
    case "TRADE_COMPLETED":
    case "TRADE_REVIEW_REQUESTED":
      return "거래 내역으로 이동";
    case "REVIEW_RECEIVED":
      return "프로필로 이동";
    case "REPORT_RESOLVED":
      return "신고 처리 결과로 이동";
    case "USER_RESTRICTED":
      return "내 정보로 이동";
    case "MILEAGE_WITHDRAWAL_REQUESTED":
      return "관리자 출금 요청으로 이동";
    case "CHAT_MESSAGE":
      return "채팅방으로 이동";
    default:
      return "관련 페이지로 이동";
  }
};
