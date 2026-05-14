package com.jmarket.admin.controller;

import com.jmarket.admin.dto.AdminAuditLogResponse;
import com.jmarket.admin.dto.AdminAuctionResponse;
import com.jmarket.admin.dto.AdminCategoryRequest;
import com.jmarket.admin.dto.AdminCategoryResponse;
import com.jmarket.admin.dto.AdminCategoryUpdateRequest;
import com.jmarket.admin.dto.AdminDashboardResponse;
import com.jmarket.admin.dto.AdminProductResponse;
import com.jmarket.admin.dto.AdminRestrictionCreateRequest;
import com.jmarket.admin.dto.AdminRestrictionResponse;
import com.jmarket.admin.dto.AdminUserBanRequest;
import com.jmarket.admin.dto.AdminUserResponse;
import com.jmarket.admin.dto.AdminUserRoleRequest;
import com.jmarket.admin.dto.AdminUserUpdateRequest;
import com.jmarket.admin.service.AdminAuditService;
import com.jmarket.admin.service.AdminService;
import com.jmarket.mileage.dto.MileageAccountResponse;
import com.jmarket.mileage.dto.MileageAdminAdjustmentRequest;
import com.jmarket.mileage.dto.MileageAdminAdjustmentType;
import com.jmarket.mileage.dto.MileageWithdrawalRejectRequest;
import com.jmarket.mileage.dto.MileageWithdrawalResponse;
import com.jmarket.mileage.service.MileageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminAuditService auditService;
    private final MileageService mileageService;

    public AdminController(AdminService adminService, AdminAuditService auditService, MileageService mileageService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.mileageService = mileageService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> getUsers(Principal principal) {
        auditService.log(principal.getName(), "USER_LIST_VIEW", "USER", null, "회원 목록 조회");
        return adminService.getUsers();
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserResponse updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleRequest request,
            Principal principal
    ) {
        return adminService.updateUserRole(userId, request, principal.getName());
    }

    @PatchMapping("/users/{userId}")
    public AdminUserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request,
            Principal principal
    ) {
        return adminService.updateUser(userId, request, principal.getName());
    }

    @PatchMapping("/users/{userId}/ban")
    public AdminUserResponse banUser(
            @PathVariable Long userId,
            @RequestBody AdminUserBanRequest request,
            Principal principal
    ) {
        return adminService.banUser(userId, request, principal.getName());
    }

    @PatchMapping("/users/{userId}/unban")
    public AdminUserResponse unbanUser(@PathVariable Long userId, Principal principal) {
        return adminService.unbanUser(userId, principal.getName());
    }

    @PostMapping("/users/{userId}/mileage-adjustments")
    public MileageAccountResponse adjustUserMileage(
            @PathVariable Long userId,
            @Valid @RequestBody MileageAdminAdjustmentRequest request,
            Principal principal
    ) {
        MileageAccountResponse response = mileageService.adjustMileageByAdmin(userId, request);
        String action = request.type() == MileageAdminAdjustmentType.GRANT
                ? "MILEAGE_ADMIN_GRANT"
                : "MILEAGE_ADMIN_DEDUCT";
        auditService.log(principal.getName(), action, "USER", userId, request.amount() + "P · " + request.reason());
        return response;
    }

    @GetMapping("/restrictions")
    public List<AdminRestrictionResponse> getRestrictions() {
        return adminService.getRestrictions();
    }

    @PostMapping("/users/{userId}/restrictions")
    public AdminRestrictionResponse createRestriction(
            @PathVariable Long userId,
            @Valid @RequestBody AdminRestrictionCreateRequest request,
            Principal principal
    ) {
        return adminService.createRestriction(userId, request, principal.getName());
    }

    @PatchMapping("/restrictions/{restrictionId}/deactivate")
    public void deactivateRestriction(@PathVariable Long restrictionId, Principal principal) {
        adminService.deactivateRestriction(restrictionId, principal.getName());
    }

    @GetMapping("/categories")
    public List<AdminCategoryResponse> getCategories() {
        return adminService.getCategories();
    }

    @PostMapping("/categories")
    public AdminCategoryResponse createCategory(
            @Valid @RequestBody AdminCategoryRequest request,
            Principal principal
    ) {
        return adminService.createCategory(request, principal.getName());
    }

    @PatchMapping("/categories/{categoryId}")
    public AdminCategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody AdminCategoryUpdateRequest request,
            Principal principal
    ) {
        return adminService.updateCategory(categoryId, request, principal.getName());
    }

    @DeleteMapping("/categories/{categoryId}")
    public void deleteCategory(@PathVariable Long categoryId, Principal principal) {
        adminService.deleteCategory(categoryId, principal.getName());
    }

    @GetMapping("/products")
    public List<AdminProductResponse> getProducts() {
        return adminService.getProducts();
    }

    @DeleteMapping("/products/{productId}")
    public void deleteProduct(@PathVariable Long productId, Principal principal) {
        adminService.deleteProduct(productId, principal.getName());
    }

    @GetMapping("/auctions")
    public List<AdminAuctionResponse> getAuctions() {
        return adminService.getAuctions();
    }

    @GetMapping("/mileage/withdrawals")
    public List<MileageWithdrawalResponse> getMileageWithdrawals() {
        return mileageService.getAllWithdrawalsForAdmin();
    }

    @PatchMapping("/mileage/withdrawals/{withdrawalId}/complete")
    public MileageWithdrawalResponse completeMileageWithdrawal(@PathVariable Long withdrawalId, Principal principal) {
        MileageWithdrawalResponse response = mileageService.completeWithdrawal(withdrawalId);
        auditService.log(principal.getName(), "MILEAGE_WITHDRAWAL_COMPLETE", "MILEAGE_WITHDRAWAL", withdrawalId, "mock transfer complete");
        return response;
    }

    @PatchMapping("/mileage/withdrawals/{withdrawalId}/reject")
    public MileageWithdrawalResponse rejectMileageWithdrawal(
            @PathVariable Long withdrawalId,
            @Valid @RequestBody MileageWithdrawalRejectRequest request,
            Principal principal
    ) {
        MileageWithdrawalResponse response = mileageService.rejectWithdrawal(withdrawalId, request.reason());
        auditService.log(principal.getName(), "MILEAGE_WITHDRAWAL_REJECT", "MILEAGE_WITHDRAWAL", withdrawalId, request.reason());
        return response;
    }

    @PatchMapping("/auctions/{auctionId}/cancel")
    public AdminAuctionResponse cancelAuction(@PathVariable Long auctionId, Principal principal) {
        return adminService.cancelAuction(auctionId, principal.getName());
    }

    @GetMapping("/audit-logs")
    public List<AdminAuditLogResponse> getAuditLogs() {
        return auditService.getRecentLogs();
    }
}
