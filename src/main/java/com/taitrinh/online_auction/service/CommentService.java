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

    /**
     * Create a new comment (question or reply)
     */
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, Long userId) {
        log.debug("Creating comment for product: {} by user: {}", request.getProductId(), userId);

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + request.getProductId()));

        // Validate user exists and is active
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hoặc người dùng không hoạt động"));

        // Validate product is not ended if it's a new question
        if (request.getParentId() == null && product.getIsEnded()) {
            throw new RuntimeException("Không thể đặt câu hỏi cho sản phẩm đã kết thúc");
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            // It's a reply - validate parent comment exists
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy comment với id: " + request.getParentId()));

            // Validate parent belongs to the same product
            if (!parent.getProduct().getId().equals(request.getProductId())) {
                throw new RuntimeException("Comment cha không thuộc về sản phẩm này");
            }

            // Only seller can reply to questions about their product
            if (!product.getSeller().getId().equals(userId)) {
                throw new RuntimeException("Chỉ người bán mới có thể trả lời câu hỏi");
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

        // TODO: Send email notification to seller if it's a new question
        // TODO: Send email notification to asker and other participants if it's a reply

        // Return unmasked response for now (can be enhanced to check if viewer is
        // seller)
        return commentMapper.toResponse(savedComment);
    }

    /**
     * Get all comments (Q&A) for a product in threaded format
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getProductComments(Long productId, Long viewerId) {
        log.debug("Getting comments for product: {}", productId);

        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

        // Get all top-level comments (questions)
        List<Comment> topLevelComments = commentRepository.findTopLevelCommentsByProductId(productId);

        // Check if viewer is the seller
        boolean isSeller = viewerId != null &&
                product.getSeller() != null &&
                product.getSeller().getId().equals(viewerId);

        // Map to response: sellers see all names, users see own names
        if (isSeller) {
            return topLevelComments.stream()
                    .map(commentMapper::toResponseUnmasked)
                    .collect(Collectors.toList());
        } else {
            return topLevelComments.stream()
                    .map(comment -> commentMapper.toResponseWithViewer(comment, viewerId))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Delete a comment (only by the author)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.debug("Deleting comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy comment với id: " + commentId));

        // Only the author can delete their own comment
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa comment này");
        }

        // Note: Deleting a parent comment will cascade delete all replies
        commentRepository.delete(comment);

        log.info("Comment deleted successfully: {}", commentId);
    }
}
