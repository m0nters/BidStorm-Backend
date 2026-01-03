package com.taitrinh.online_auction.service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.admin.UpgradeRequestResponse;
import com.taitrinh.online_auction.entity.Role;
import com.taitrinh.online_auction.entity.UpgradeRequest;
import com.taitrinh.online_auction.entity.UpgradeRequest.UpgradeStatus;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.BadRequestException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.mapper.UpgradeRequestMapper;
import com.taitrinh.online_auction.repository.RoleRepository;
import com.taitrinh.online_auction.repository.UpgradeRequestRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpgradeRequestService {

        private final UpgradeRequestRepository upgradeRequestRepository;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final ConfigService configService;
        private final UpgradeRequestMapper upgradeRequestMapper;

        /**
         * Get all upgrade requests
         */
        @Transactional(readOnly = true)
        public List<UpgradeRequestResponse> getAllUpgradeRequests() {
                List<UpgradeRequest> requests = upgradeRequestRepository.findAllByOrderByCreatedAtDesc();
                return requests.stream()
                                .map(upgradeRequestMapper::toUpgradeRequestResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Get pending upgrade requests only
         */
        @Transactional(readOnly = true)
        public List<UpgradeRequestResponse> getPendingUpgradeRequests() {
                List<UpgradeRequest> requests = upgradeRequestRepository
                                .findAllByStatusOrderByCreatedAtDesc(UpgradeStatus.PENDING);
                return requests.stream()
                                .map(upgradeRequestMapper::toUpgradeRequestResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Approve upgrade request
         * - Set role to SELLER
         * - Set seller expiration date based on config
         * - Mark request as approved
         */
        @Transactional
        public void approveUpgradeRequest(Long requestId, Long adminId) {
                UpgradeRequest request = upgradeRequestRepository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("UpgradeRequest", requestId));

                User bidder = request.getBidder();
                User admin = userRepository.findById(adminId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

                // Get seller role
                Role sellerRole = roleRepository.findById(Role.SELLER)
                                .orElseThrow(() -> new RuntimeException("Seller role not found"));

                // Update user to seller with temporary permission
                Integer durationDays = configService.getSellerTempDurationDays();
                bidder.setRole(sellerRole);
                bidder.setSellerExpiresAt(ZonedDateTime.now().plusDays(durationDays));
                bidder.setSellerUpgradedBy(admin);
                userRepository.save(bidder);

                // Update request status
                request.setStatus(UpgradeStatus.APPROVED);
                request.setAdmin(admin);
                request.setReviewedAt(ZonedDateTime.now());
                upgradeRequestRepository.save(request);

                log.info("Upgrade request {} approved by admin {}. User {} upgraded to SELLER for {} days",
                                requestId, adminId, bidder.getId(), durationDays);
        }

        /**
         * Reject upgrade request
         */
        @Transactional
        public void rejectUpgradeRequest(Long requestId, Long adminId) {
                UpgradeRequest request = upgradeRequestRepository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("UpgradeRequest", requestId));

                User admin = userRepository.findById(adminId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

                request.setStatus(UpgradeStatus.REJECTED);
                request.setAdmin(admin);
                request.setReviewedAt(ZonedDateTime.now());
                upgradeRequestRepository.save(request);

                log.info("Upgrade request {} rejected by admin {}", requestId, adminId);
        }

        /**
         * Submit upgrade request (for bidder)
         */
        @Transactional
        public void submitUpgradeRequest(Long bidderId, String reason) {
                User bidder = userRepository.findById(bidderId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", bidderId));

                // Check if user already has a pending or approved request
                if (upgradeRequestRepository.findByBidder_IdAndStatus(bidderId, UpgradeStatus.PENDING).isPresent()
                                || upgradeRequestRepository.findByBidder_IdAndStatus(bidderId, UpgradeStatus.APPROVED)
                                                .isPresent()) {
                        throw new BadRequestException(
                                        "Bạn đã có yêu cầu nâng cấp tồn tại. Không thể gửi yêu cầu mới.");
                }

                // Check if there's denial within 15 minutes ago, if so cannot request new
                if (upgradeRequestRepository
                                .findByBidder_IdAndStatusAndCreatedAtAfter(bidderId, UpgradeStatus.REJECTED,
                                                ZonedDateTime.now().minusMinutes(15))
                                .isPresent()) {
                        throw new BadRequestException(
                                        "Bạn đã có yêu cầu nâng cấp bị từ chối gần đây. Hãy đợi thêm một khoảng thời gian trước khi gửi yêu cầu mới.");
                }

                UpgradeRequest request = UpgradeRequest.builder()
                                .bidder(bidder)
                                .reason(reason)
                                .status(UpgradeStatus.PENDING)
                                .build();

                upgradeRequestRepository.save(request);
                log.info("User {} submitted upgrade request", bidderId);
        }

        /**
         * Get user's own upgrade request status (for bidder)
         */
        @Transactional(readOnly = true)
        public UpgradeRequest getMyUpgradeRequest(Long bidderId) {
                return upgradeRequestRepository.findByBidder_Id(bidderId).orElse(null);
        }

}
