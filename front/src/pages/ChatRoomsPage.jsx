import { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken } from "../lib/auth";
import { getChatRoomTypeLabel } from "../lib/chatRoomType";

function ChatRoomsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const isPopup = new URLSearchParams(location.search).get("popup") === "1";
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("채팅방 목록을 불러오는 중...");

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const loadRooms = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api("/api/chat/rooms/me");
      setRooms(response);
      setMessage("채팅방 목록 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`채팅방 목록 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized]);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  const formatDateTime = (value) => {
    if (!value) {
      return "";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(value));
  };

  const getOtherNickname = (room) => {
    if (room.lastMessageSenderNickname) {
      return room.lastMessageSenderNickname;
    }
    return `${room.participantANickname} / ${room.participantBNickname}`;
  };

  const getRoomTarget = (room) => {
    if (room.roomType === "AUCTION_BID" || room.roomType === "AUCTION") {
      return room.auctionId ? `경매 #${room.auctionId}` : "경매";
    }
    return room.tradeId ? `거래 #${room.tradeId}` : getChatRoomTypeLabel(room.roomType);
  };
  const shouldShowMessage = loading || message.includes("실패") || message.includes("불러오는 중");

  return (
    <main className={`container chat-page chat-rooms-page${isPopup ? " chat-popup-page chat-rooms-popup-page" : ""}`}>
      <h1>채팅방 목록</h1>
      <div className="card">
        <div className="actions">
          <button className="secondary-button" onClick={loadRooms} disabled={loading}>목록 새로고침</button>
        </div>
      </div>

      <div className="card">
        {shouldShowMessage ? (
          <p className={`page-message ${loading ? "loading" : message.includes("실패") ? "error" : ""}`}>
            {loading ? "요청 처리 중..." : message}
          </p>
        ) : null}
        {rooms.length === 0 ? (
          <p className="empty-box">참여 중인 채팅방이 없습니다.</p>
        ) : (
          <ul className="chat-room-list">
            {rooms.map((room) => (
              <li key={room.id} className={`chat-room-list-item${Number(room.unreadCount ?? 0) > 0 ? " unread" : ""}`}>
                <Link to={`/chat/rooms/${room.id}${isPopup ? "?popup=1" : ""}`} className="chat-room-list-link">
                  <div className="chat-room-list-main">
                    <div className="chat-room-list-head">
                      <strong>{getRoomTarget(room)}</strong>
                      <span>{formatDateTime(room.lastMessageAt ?? room.createdAt)}</span>
                    </div>
                    <p className="chat-room-list-preview">
                      {room.lastMessageContent ? `${getOtherNickname(room)}: ${room.lastMessageContent}` : "아직 메시지가 없습니다."}
                    </p>
                    <p className="meta">
                      {getChatRoomTypeLabel(room.roomType)} · 참여자 {room.participantANickname} / {room.participantBNickname}
                    </p>
                  </div>
                  {Number(room.unreadCount ?? 0) > 0 ? (
                    <span className="chat-unread-badge">{Number(room.unreadCount).toLocaleString()}</span>
                  ) : null}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}

export default ChatRoomsPage;
