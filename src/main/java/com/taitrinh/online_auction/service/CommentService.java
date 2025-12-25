package com.taitrinh.online_auction.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.comment.CommentResponse;
import com.taitrinh.online_auction.dto.comment.CreateCommentRequest;
import com.taitrinh.online_auction.entity.Comment;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.CommentNotFoundException;
import com.taitrinh.online_auction.exception.InvalidCommentStateException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.exception.UnauthorizedCommentActionException;
import com.taitrinh.online_auction.mapper.CommentMapper;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.CommentRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final CommentMapper commentMapper;
    private final CommentNotificationService notificationService;
    private final EmailService emailService;

    /**
     * Create a new comment (question or reply)
     */
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, Long userId) {
        log.debug("Creating comment for product: {} by user: {}", request.getProductId(), userId);

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", request.getProductId()));

        // Validate user exists and is active
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));

        // Validate product is not ended if it's a new question
        if (request.getParentId() == null && product.getIsEnded()) {
            throw new InvalidCommentStateException("Không thể đặt câu hỏi cho sản phẩm đã kết thúc");
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            // It's a reply - validate parent comment exists
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CommentNotFoundException(request.getParentId()));

            // Validate parent belongs to the same product
            if (!parent.getProduct().getId().equals(request.getProductId())) {
                throw new InvalidCommentStateException("Comment cha không thuộc về sản phẩm này");
            }

            // Only seller can reply to questions about their product
            if (!product.getSeller().getId().equals(userId)) {
                throw new UnauthorizedCommentActionException("Chỉ người bán mới có thể trả lời câu hỏi");
            }
        }

        // Create comment
        Comment comment = Comment.builder()
                .product(product)
                .user(user)
                .parent(parent)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);

        log.info("Comment created successfully with id: {}", savedComment.getId());

        // Determine if comment author is the product seller
        boolean isAuthorSeller = product.getSeller() != null && userId.equals(product.getSeller().getId());

        // 1. Broadcast to PUBLIC channel - masked names (viewerId=null, isSeller=false)
        CommentResponse publicResponse = commentMapper.toResponseWithViewer(savedComment, null, false);
        notificationService.notifyNewComment(product.getId(), publicResponse);

        // 2. Broadcast to SELLER channel - unmasked names (viewerId=null,
        // isSeller=true)
        CommentResponse sellerResponse = commentMapper.toResponseWithViewer(savedComment, null, true);
        notificationService.notifyNewCommentToSeller(product.getId(), sellerResponse);

        // 3. Return personalized response to HTTP client (author sees their own
        // unmasked name)
        CommentResponse personalizedResponse = commentMapper.toResponseWithViewer(savedComment, userId, isAuthorSeller);

        // 4. Send email notifications
        if (parent == null) {
            // New question - send email to seller
            if (product.getSeller() != null && product.getSeller().getEmail() != null) {
                emailService.sendNewQuestionToSeller(
                        product.getSeller().getEmail(),
                        product.getSeller().getFullName(),
                        product.getTitle(),
                        user.getFullName(),
                        request.getContent(),
                        product.getSlug(),
                        savedComment.getId());
                log.info("Sent new question email to seller for product: {}", product.getId());
            }
        } else {
            // Reply from seller - send email to all question askers and bidders
            java.util.Set<Long> emailedUserIds = new java.util.HashSet<>();

            // Get the ID of the user who asked the specific question being replied to
            Long originalAskerId = parent.getUser() != null ? parent.getUser().getId() : null;

            // Get all users who have asked questions on this product
            List<User> questionAskers = commentRepository.findDistinctQuestionAskersByProductId(product.getId());
            for (User asker : questionAskers) {
                // Don't send to seller themselves
                if (asker != null && asker.getEmail() != null && !asker.getId().equals(userId)) {
                    // Check if this user is the original asker of the question being replied to
                    boolean isOriginalAsker = originalAskerId != null && asker.getId().equals(originalAskerId);

                    if (isOriginalAsker) {
                        // Send personalized "your question was answered" email
                        emailService.sendSellerReplyNotification(
                                asker.getEmail(),
                                asker.getFullName(),
                                product.getTitle(),
                                request.getContent(),
                                product.getSlug(),
                                savedComment.getId());
                    } else {
                        // Send general "new activity on product you're interested in" email
                        emailService.sendProductActivityNotification(
                                asker.getEmail(),
                                asker.getFullName(),
                                product.getTitle(),
                                request.getContent(),
                                product.getSlug(),
                                savedComment.getId());
                    }
                    emailedUserIds.add(asker.getId());
                }
            }
            log.info("Sent seller reply emails to {} question askers for product: {}", emailedUserIds.size(),
                    product.getId());

            // Get all bidders who have bid on this product
            List<User> bidders = bidHistoryRepository.findDistinctBiddersByProductId(product.getId());
            int bidderEmailCount = 0;
            for (User bidder : bidders) {
                // Don't send to seller themselves or users who already received email
                if (bidder != null && bidder.getEmail() != null &&
                        !bidder.getId().equals(userId) &&
                        !emailedUserIds.contains(bidder.getId())) {
                    // Bidders who haven't asked questions get general activity notification
                    emailService.sendProductActivityNotification(
                            bidder.getEmail(),
                            bidder.getFullName(),
                            product.getTitle(),
                            request.getContent(),
                            product.getSlug(),
                            savedComment.getId());
                    bidderEmailCount++;
                }
            }
            log.info("Sent seller reply emails to {} bidders for product: {}", bidderEmailCount, product.getId());
        }

        return personalizedResponse;
    }

    /**
     * Get all comments (Q&A) for a product in threaded format
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getProductComments(Long productId, Long viewerId) {
        log.debug("Getting comments for product: {}", productId);

        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        // Get all top-level comments (questions)
        List<Comment> topLevelComments = commentRepository.findTopLevelCommentsByProductId(productId);

        // Check if viewer is the seller
        boolean isSeller = viewerId != null &&
                product.getSeller() != null &&
                product.getSeller().getId().equals(viewerId);

        // Map to response with unified logic
        return topLevelComments.stream()
                .map(comment -> commentMapper.toResponseWithViewer(comment, viewerId, isSeller))
                .collect(Collectors.toList());
    }

    /**
     * Delete a comment (only by the author)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.debug("Deleting comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        // Only the author can delete their own comment
        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedCommentActionException("Bạn không có quyền xóa comment này");
        }

        Long productId = comment.getProduct().getId();

        // Broadcast real-time notification before deleting
        notificationService.notifyDeleteComment(productId, commentId);

        // Note: Deleting a parent comment will cascade delete all replies
        commentRepository.delete(comment);

        log.info("Comment deleted successfully: {}", commentId);
    }
}
