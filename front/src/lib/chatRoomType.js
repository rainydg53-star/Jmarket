const CHAT_ROOM_TYPE_LABELS = {
  PRODUCT_TRADE: "일반 거래",
  AUCTION: "경매",
  AUCTION_BID: "경매",
};

export const getChatRoomTypeLabel = (roomType) => CHAT_ROOM_TYPE_LABELS[roomType] ?? roomType ?? "-";
