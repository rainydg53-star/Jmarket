export function hasRestriction(user, restrictionType) {
  return Array.isArray(user?.activeRestrictions) && user.activeRestrictions.includes(restrictionType);
}

export function isAdmin(user) {
  return user?.role === "ADMIN" || user?.role === "SUPER_ADMIN";
}

export function canManageRoles(user) {
  return user?.role === "SUPER_ADMIN";
}

export function canCreateProduct(user) {
  return Boolean(user) && !isAdmin(user) && !hasRestriction(user, "PRODUCT_CREATE");
}

export function canCreateAuction(user) {
  return Boolean(user) && !isAdmin(user) && !hasRestriction(user, "AUCTION_CREATE");
}

export function canBidAuction(user) {
  return Boolean(user) && !isAdmin(user) && !hasRestriction(user, "AUCTION_BID");
}

export function canUseUserActions(user) {
  return Boolean(user) && !isAdmin(user);
}
