const CHAT_WINDOW_NAME = "jmarket-chat";
const CHAT_WINDOW_FEATURES = [
  "width=460",
  "height=760",
  "left=120",
  "top=80",
  "resizable=yes",
  "scrollbars=yes",
].join(",");

const withPopupParam = (path) => {
  const separator = path.includes("?") ? "&" : "?";
  return `${path}${separator}popup=1`;
};

export const getChatRoomPath = (roomId, popup = false) => {
  const path = roomId ? `/chat/rooms/${roomId}` : "/chat/rooms";
  return popup ? withPopupParam(path) : path;
};

export const openChatWindow = (roomId = null) => {
  const path = getChatRoomPath(roomId, true);
  const url = `${window.location.origin}${path}`;
  const popup = window.open(url, CHAT_WINDOW_NAME, CHAT_WINDOW_FEATURES);

  if (popup) {
    popup.focus();
    return;
  }

  window.location.href = path;
};
