package com.taitrinh.online_auction.service;

import java.time.ZonedDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.admin.UserDetailResponse;
import com.taitrinh.online_auction.dto.admin.UserListResponse;
import com.taitrinh.online_auction.entity.Role;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.mapper.UserMapper;
import com.taitrinh.online_auction.repository.RefreshTokenRepository;
import com.taitrinh.online_auction.repository.RoleRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    /**
     * Get all users with pagination and optional filters
     */
    @Transactional(readOnly = true)
    public Page<UserListResponse> getAllUsers(Integer page, Integer size, Short roleId, Boolean isActive,
            String sortDirection) {
        // Determine sort direction (default to descending if invalid)
        Sort sort = sortDirection != null && sortDirection.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> users;
        if (roleId != null && isActive != null) {
            users = userRepository.findAllByRole_IdAndIsActive(roleId, isActive, pageable);
        } else if (roleId != null) {
            users = userRepository.findAllByRole_Id(roleId, pageable);
        } else if (isActive != null) {
            users = userRepository.findAllByIsActive(isActive, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(userMapper::toUserListResponse);
    }

    /**
     * Get detailed user information by ID
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return userMapper.toUserDetailResponse(user);
    }

    /**
     * Ban user (set is_active = false)
     * Also revokes all refresh tokens to immediately invalidate sessions
     */
    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsActive(false);
        userRepository.save(user);

        // Revoke all refresh tokens to invalidate sessions
        refreshTokenRepository.revokeAllByUserId(userId, ZonedDateTime.now());

        log.info("User {} has been banned and all tokens revoked", userId);
    }

    /**
     * Unban user (set is_active = true)
     */
    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsActive(true);
        userRepository.save(user);

        log.info("User {} has been unbanned", userId);
    }

    /**
     * Change user role (ADMIN only)
     * Revokes all tokens to force re-authentication with new role
     */
    @Transactional
    public void changeUserRole(Long userId, Short newRoleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Role newRole = roleRepository.findById(newRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", newRoleId.intValue()));

        user.setRole(newRole);
        user.setSellerExpiresAt(null);
        user.setSellerUpgradedBy(null);
        userRepository.save(user);

        // Revoke all refresh tokens to force re-authentication with new role
        refreshTokenRepository.revokeAllByUserId(userId, ZonedDateTime.now());

        log.info("User {} role changed to {} (roleId: {}) and all tokens revoked", userId, newRole.getName(),
                newRoleId);
    }
}
