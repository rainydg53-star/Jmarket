import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { clearAccessToken, getAccessToken } from "../lib/auth";
import { getChatRoomTypeLabel } from "../lib/chatRoomType";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

function ChatRoomPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const isPopup = new URLSearchParams(location.search).get("popup") === "1";
  const clientRef = useRef(null);
  const messagesEndRef = useRef(null);
  const [me, setMe] = useState(null);
  const [room, setRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("채팅방을 불러오는 중...");
  const isTradeCompleted = room?.roomType === "PRODUCT_TRADE" && room?.tradeStatus === "COMPLETED";

  const handleUnauthorized = useCallback(() => {
    clearAccessToken();
    navigate("/login", { replace: true });
  }, [navigate]);

  const loadRoomAndMessages = useCallback(async () => {
    setLoading(true);
    try {
      const [meRes, roomRes, msgRes] = await Promise.all([
        api("/api/auth/me"),
        api(`/api/chat/rooms/${roomId}`),
        api(`/api/chat/rooms/${roomId}/messages`),
      ]);
      setMe(meRes);
      setRoom(roomRes);
      setMessages(msgRes);
      await api(`/api/chat/rooms/${roomId}/read`, { method: "PATCH" }).catch(() => null);
      setMessage("채팅방 조회 성공");
    } catch (error) {
      if (error.status === 401) {
        handleUnauthorized();
        return;
      }
      setMessage(`채팅방 조회 실패: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }, [handleUnauthorized, roomId]);

  useEffect(() => {
    loadRoomAndMessages();
  }, [loadRoomAndMessages]);

  useEffect(() => {
    if (!room) {
      return;
    }
    if (isTradeCompleted) {
      return;
    }

    const token = getAccessToken();
    if (!token) {
      return;
    }

    const wsBaseUrl = API_BASE_URL.replace(/^http/, "ws");
    const client = new Client({
      brokerURL: `${wsBaseUrl}/ws-chat`,
      reconnectDelay: 3000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/chat.rooms.${roomId}`, (frame) => {
          const payload = JSON.parse(frame.body);
          setMessages((prev) => [...prev, payload]);
          if (payload.senderId !== me?.id) {
            api(`/api/chat/rooms/${roomId}/read`, { method: "PATCH" }).catch(() => null);
          }
        });
      },
      onStompError: (frame) => {
        setMessage(`STOMP 오류: ${frame.headers?.message ?? "unknown error"}`);
      },
      onWebSocketClose: () => {
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [roomId, room, isTradeCompleted, me?.id]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length]);

  const formatMessageTime = (value) => {
    if (!value) {
      return "";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    }).format(new Date(value));
  };

  const otherNickname = room && me
    ? room.participantAId === me.id ? room.participantBNickname : room.participantANickname
    : "";

  const sendMessage = () => {
    if (isTradeCompleted) {
      setMessage("거래가 완료되어 채팅 전송이 비활성화되었습니다.");
      return;
    }
    const text = content.trim();
    if (!text) {
      return;
    }
    if (!clientRef.current || !connected) {
      setMessage("실시간 연결이 아직 준비되지 않았습니다.");
      return;
    }
    clientRef.current.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({
        roomId: Number(roomId),
        content: text,
      }),
    });
    setContent("");
  };

  return (
    <main className={`container chat-page${isPopup ? " chat-popup-page" : ""}`}>
      <div className="chat-room-shell">
        <div className="chat-room-header">
          <div>
            <p className="meta"><Link to={`/chat/rooms${isPopup ? "?popup=1" : ""}`}>채팅방 목록으로</Link></p>
            <h1>{otherNickname || "채팅방"}</h1>
            <p className="meta">
              {room ? `${room.participantANickname} / ${room.participantBNickname}` : "참여자 정보를 불러오는 중"}
            </p>
          </div>
          <span className={`status-badge ${connected ? "success" : "muted"}`}>
            {connected ? "실시간 연결" : "연결 대기"}
          </span>
        </div>
        <p className="chat-room-message">{loading ? "요청 처리 중..." : message}</p>
        {room ? (
          <div className="chat-room-actions">
            <span className="meta">
              {room.roomType === "AUCTION" || room.roomType === "AUCTION_BID"
                ? `경매 #${room.auctionId}`
                : room.tradeId
                  ? `${getChatRoomTypeLabel(room.roomType)} #${room.tradeId}`
                  : getChatRoomTypeLabel(room.roomType)}
            </span>
            <div className="actions compact-actions">
              <button
                type="button"
                onClick={() => navigate(`/reports?targetType=CHAT_ROOM&targetId=${room.id}&reason=${encodeURIComponent("채팅 신고")}`)}
                disabled={loading}
              >
                채팅 신고
              </button>
            </div>
          </div>
        ) : null}
        {isTradeCompleted ? <p className="meta">거래 완료 상태라 채팅은 읽기 전용입니다.</p> : null}

      <section className="chat-conversation" aria-label="메시지">
        {messages.length === 0 ? (
          <p className="chat-empty">메시지가 아직 없습니다.</p>
        ) : (
          <div className="chat-message-list">
            {messages.map((msg) => {
              const mine = me?.id === msg.senderId;
              return (
                <div key={`${msg.id}-${msg.sentAt}`} className={`chat-message-row${mine ? " mine" : " theirs"}`}>
                  {!mine ? <div className="chat-sender-name">{msg.senderNickname}</div> : null}
                  <div className="chat-bubble-line">
                    {mine ? <span className="chat-message-time">{formatMessageTime(msg.sentAt)}</span> : null}
                    <div className="chat-bubble">{msg.content}</div>
                    {!mine ? <span className="chat-message-time">{formatMessageTime(msg.sentAt)}</span> : null}
                  </div>
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </section>

      <div className="chat-composer">
        <input
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              sendMessage();
            }
          }}
          maxLength={2000}
          disabled={!connected || isTradeCompleted}
          placeholder={isTradeCompleted ? "거래 완료로 인해 채팅이 비활성화되었습니다." : "메시지를 입력하세요"}
        />
        <button onClick={sendMessage} disabled={!connected || isTradeCompleted || !content.trim()}>전송</button>
      </div>
      </div>
    </main>
  );
}

export default ChatRoomPage;
