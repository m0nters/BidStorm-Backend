package com.taitrinh.online_auction.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.admin.AdminStatisticsOverviewResponse;
import com.taitrinh.online_auction.dto.admin.BasicStatisticsResponse;
import com.taitrinh.online_auction.dto.admin.CategoryRevenueResponse;
import com.taitrinh.online_auction.dto.admin.ChangeUserRoleRequest;
import com.taitrinh.online_auction.dto.admin.LeaderboardEntryResponse;
import com.taitrinh.online_auction.dto.admin.PendingPaymentsResponse;
import com.taitrinh.online_auction.dto.admin.SystemConfigResponse;
import com.taitrinh.online_auction.dto.admin.UpdateSystemConfigRequest;
import com.taitrinh.online_auction.dto.admin.UpgradeRequestResponse;
import com.taitrinh.online_auction.dto.admin.UserDetailResponse;
import com.taitrinh.online_auction.dto.admin.UserListResponse;
import com.taitrinh.online_auction.entity.SystemConfig;
import com.taitrinh.online_auction.enums.TimePeriod;
import com.taitrinh.online_auction.mapper.SystemConfigMapper;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.AdminStatisticsService;
import com.taitrinh.online_auction.service.ConfigService;
import com.taitrinh.online_auction.service.UpgradeRequestService;
import com.taitrinh.online_auction.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "APIs for admin-only operations (user management, upgrade requests, statistics, configurations)")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final UserService userService;
    private final UpgradeRequestService upgradeRequestService;
    private final ConfigService configService;
    private final SystemConfigMapper systemConfigMapper;
    private final AdminStatisticsService adminStatisticsService;

    // ===== User Management =====

    @GetMapping("/users")
    @Operation(summary = "List all users (ADMIN only)", description = "Get paginated list of users with optional filters by role and active status, sorted by creation date")
    public ResponseEntity<ApiResponse<Page<UserListResponse>>> getAllUsers(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,

            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") @Min(1) Integer size,

            @Parameter(description = "Filter by role ID (1=ADMIN, 2=SELLER, 3=BIDDER)", example = "3") @RequestParam(required = false) Short roleId,

            @Parameter(description = "Filter by active status", example = "true") @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Sort direction (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<UserListResponse> users = userService.getAllUsers(page, size, roleId, isActive, sortDirection);
        return ResponseEntity.ok(ApiResponse.ok(users, "Danh sách người dùng đã được lấy thành công"));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user details (ADMIN only)", description = "Get detailed information about a specific user")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(
            @Parameter(description = "User ID", example = "1") @PathVariable Long id) {

        UserDetailResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user, "Chi tiết người dùng đã được lấy thành công"));
    }

    @PatchMapping("/users/{id}/ban")
    @Operation(summary = "Ban user (ADMIN only)", description = "Ban a user by setting is_active = false")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @Parameter(description = "User ID to ban", example = "5") @PathVariable Long id) {

        userService.banUser(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Người dùng đã bị cấm thành công"));
    }

    @PatchMapping("/users/{id}/unban")
    @Operation(summary = "Unban user (ADMIN only)", description = "Unban a user by setting is_active = true")
    public ResponseEntity<ApiResponse<Void>> unbanUser(
            @Parameter(description = "User ID to unban", example = "5") @PathVariable Long id) {

        userService.unbanUser(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Người dùng đã được mở cấm thành công"));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change user role (ADMIN only)", description = "Change a user's role (1=ADMIN, 2=SELLER, 3=BIDDER)")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @Parameter(description = "User ID", example = "5") @PathVariable Long id,
            @Valid @RequestBody ChangeUserRoleRequest request) {

        userService.changeUserRole(id, request.getRoleId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Vai trò người dùng đã được thay đổi thành công"));
    }

    // ===== Upgrade Request Management =====

    @GetMapping("/upgrade-requests")
    @Operation(summary = "List all upgrade requests (ADMIN only)", description = "Get all upgrade requests ordered by creation date")
    public ResponseEntity<ApiResponse<List<UpgradeRequestResponse>>> getAllUpgradeRequests() {
        List<UpgradeRequestResponse> requests = upgradeRequestService.getAllUpgradeRequests();
        return ResponseEntity.ok(ApiResponse.ok(requests, "Danh sách yêu cầu nâng cấp đã được lấy thành công"));
    }

    @GetMapping("/upgrade-requests/pending")
    @Operation(summary = "List pending upgrade requests (ADMIN only)", description = "Get all pending upgrade requests ordered by creation date")
    public ResponseEntity<ApiResponse<List<UpgradeRequestResponse>>> getPendingUpgradeRequests() {
        List<UpgradeRequestResponse> requests = upgradeRequestService.getPendingUpgradeRequests();
        return ResponseEntity
                .ok(ApiResponse.ok(requests, "Danh sách yêu cầu nâng cấp đang chờ đã được lấy thành công"));
    }

    @PatchMapping("/upgrade-requests/{id}/approve")
    @Operation(summary = "Approve upgrade request (ADMIN only)", description = "Approve a bidder's request to become a seller. Sets user role to SELLER with temporary permission based on system config.")
    public ResponseEntity<ApiResponse<Void>> approveUpgradeRequest(
            @Parameter(description = "Upgrade request ID", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        upgradeRequestService.approveUpgradeRequest(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Yêu cầu nâng cấp đã được phê duyệt thành công"));
    }

    @PatchMapping("/upgrade-requests/{id}/reject")
    @Operation(summary = "Reject upgrade request (ADMIN only)", description = "Reject a bidder's request to become a seller")
    public ResponseEntity<ApiResponse<Void>> rejectUpgradeRequest(
            @Parameter(description = "Upgrade request ID", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        upgradeRequestService.rejectUpgradeRequest(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Yêu cầu nâng cấp đã bị từ chối"));
    }

    // ===== Statistics =====

    @GetMapping("/statistics/overview")
    @Operation(summary = "Get statistics overview (ADMIN only)", description = "Get complete statistics overview with all metrics")
    public ResponseEntity<ApiResponse<AdminStatisticsOverviewResponse>> getStatisticsOverview(
            @Parameter(description = "Time period for statistics", example = "LAST_30_DAYS") @RequestParam(defaultValue = "LAST_30_DAYS") TimePeriod timePeriod,
            @Parameter(description = "Leaderboard limit", example = "10") @RequestParam(defaultValue = "10") int leaderboardLimit) {

        AdminStatisticsOverviewResponse stats = adminStatisticsService.getOverviewStatistics(timePeriod,
                leaderboardLimit);
        return ResponseEntity.ok(ApiResponse.ok(stats, "Tổng quan thống kê đã được lấy thành công"));
    }

    @GetMapping("/statistics/basic")
    @Operation(summary = "Get basic statistics (ADMIN only)", description = "Get basic count statistics (new auctions, users, ​upgrades, zero-bid products)")
    public ResponseEntity<ApiResponse<BasicStatisticsResponse>> getBasicStatistics(
            @Parameter(description = "Time period for statistics", example = "LAST_30_DAYS") @RequestParam(defaultValue = "LAST_30_DAYS") TimePeriod timePeriod) {

        BasicStatisticsResponse stats = adminStatisticsService.getBasicStatistics(timePeriod);
        return ResponseEntity.ok(ApiResponse.ok(stats, "Thống kê cơ bản đã được lấy thành công"));
    }

    @GetMapping("/statistics/financial")
    @Operation(summary = "Get financial statistics (ADMIN only)", description = "Get total revenue, revenue by category, and pending payments")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getFinancialStatistics() {
        com.taitrinh.online_auction.dto.admin.RevenueStatisticsResponse totalRevenue = adminStatisticsService
                .getRevenueStatistics();
        List<CategoryRevenueResponse> categoryRevenue = adminStatisticsService.getRevenueByCategory();
        PendingPaymentsResponse pendingPayments = adminStatisticsService.getPendingPayments();

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("categoryRevenue", categoryRevenue);
        response.put("pendingPayments", pendingPayments);

        return ResponseEntity.ok(ApiResponse.ok(response, "Thống kê tài chính đã được lấy thành công"));
    }

    @GetMapping("/statistics/leaderboards")
    @Operation(summary = "Get leaderboards (ADMIN only)", description = "Get top bidders and sellers leaderboards")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getLeaderboards(
            @Parameter(description = "Leaderboard limit", example = "10") @RequestParam(defaultValue = "10") int limit) {

        List<LeaderboardEntryResponse> topBidders = adminStatisticsService.getTopBidders(limit);
        List<LeaderboardEntryResponse> topSellers = adminStatisticsService.getTopSellers(limit);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("topBidders", topBidders);
        response.put("topSellers", topSellers);

        return ResponseEntity.ok(ApiResponse.ok(response, "Bảng xếp hạng đã được lấy thành công"));
    }

    // ===== System Configuration =====

    @GetMapping("/configs")
    @Operation(summary = "List all system configurations (ADMIN only)", description = "Get all system configuration entries")
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> getAllConfigs() {
        List<SystemConfig> configs = configService.getAllConfigs();
        List<SystemConfigResponse> response = configs.stream()
                .map(systemConfigMapper::toSystemConfigResponse)
                .sorted(Comparator.comparing(SystemConfigResponse::getKey))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response, "Danh sách cấu hình hệ thống đã được lấy thành công"));
    }

    @GetMapping("/configs/{key}")
    @Operation(summary = "Get system configuration by key (ADMIN only)", description = "Get a specific system configuration entry")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfigByKey(
            @Parameter(description = "Configuration key", example = "auto_extend_trigger_min") @PathVariable String key) {

        SystemConfig config = configService.getConfigByKey(key);
        return ResponseEntity.ok(ApiResponse.ok(systemConfigMapper.toSystemConfigResponse(config),
                "Cấu hình hệ thống đã được lấy thành công"));
    }

    @PutMapping("/configs/{key}")
    @Operation(summary = "Update system configuration (ADMIN only)", description = "Update a system configuration value")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> updateConfig(
            @Parameter(description = "Configuration key", example = "auto_extend_trigger_min") @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request) {

        SystemConfig config = configService.updateConfig(key, request.getValue());
        return ResponseEntity.ok(ApiResponse.ok(systemConfigMapper.toSystemConfigResponse(config),
                "Cấu hình hệ thống đã được cập nhật thành công"));
    }

}
