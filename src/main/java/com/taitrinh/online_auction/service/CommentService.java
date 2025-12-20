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
    private final CommentMapper commentMapper;
    private final CommentNotificationService notificationService;

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

        // TODO: Send email notification to seller if it's a new question
        // TODO: Send email notification to asker and other participants if it's a reply

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
