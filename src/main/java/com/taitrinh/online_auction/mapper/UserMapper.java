package com.taitrinh.online_auction.mapper;

import org.springframework.stereotype.Component;

import com.taitrinh.online_auction.dto.admin.UserDetailResponse;
import com.taitrinh.online_auction.dto.admin.UserListResponse;
import com.taitrinh.online_auction.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserMapper {

    /**
     * Map User entity to UserListResponse DTO
     */
    public UserListResponse toUserListResponse(User user) {
        return UserListResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleId(user.getRole().getId())
                .roleName(user.getRole().getName())
                .positiveRating(user.getPositiveRating())
                .negativeRating(user.getNegativeRating())
                .isActive(user.getIsActive())
                .sellerExpiresAt(user.getSellerExpiresAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Map User entity to UserDetailResponse DTO
     */
    public UserDetailResponse toUserDetailResponse(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .address(user.getAddress())
                .birthDate(user.getBirthDate())
                .roleId(user.getRole().getId())
                .roleName(user.getRole().getName())
                .positiveRating(user.getPositiveRating())
                .negativeRating(user.getNegativeRating())
                .emailVerified(user.getEmailVerified())
                .isActive(user.getIsActive())
                .sellerExpiresAt(user.getSellerExpiresAt())
                .sellerUpgradedBy(user.getSellerUpgradedBy() != null ? user.getSellerUpgradedBy().getId() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
