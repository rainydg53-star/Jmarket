export function formatKoreanDateTime(value) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
}

export function parseRestrictionMessage(message) {
  const featureMatch = message.match(/기능:\s*(.*?)(?:\s*사유:|$)/);
  const reasonMatch = message.match(/사유:\s*(.*?)(?:\s*해제 예정:|$)/);
  const untilMatch = message.match(/해제 예정:\s*(.*)$/);
  return {
    feature: featureMatch?.[1]?.trim() || "제한된 기능",
    reason: reasonMatch?.[1]?.trim() || "-",
    restrictedUntil: formatKoreanDateTime(untilMatch?.[1]?.trim()) || null,
  };
}
