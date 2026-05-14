export const getTradeStatusTone = (status) => ({
  REQUESTED: "warning",
  ACCEPTED: "success",
  COMPLETED: "success",
  CANCELED: "muted",
  CANCELLED: "muted",
}[status] ?? "muted");

export const getSupportStatusTone = (status) => ({
  WAITING: "warning",
  ANSWERED: "success",
  CLOSED: "muted",
}[status] ?? "muted");

export const getWithdrawalStatusTone = (status) => ({
  REQUESTED: "warning",
  COMPLETED: "success",
  REJECTED: "danger",
}[status] ?? "muted");
