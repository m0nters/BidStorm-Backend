package com.taitrinh.online_auction.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.taitrinh.online_auction.dto.comment.CommentResponse;
import com.taitrinh.online_auction.entity.Comment;
import com.taitrinh.online_auction.entity.User;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "productId", source = "comment.product.id")
    @Mapping(target = "userId", source = "comment.user.id")
    @Mapping(target = "userName", source = "comment.user", qualifiedByName = "maskUserName")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "replies", ignore = true) // Handle manually to avoid recursion issues
    CommentResponse toResponse(Comment comment);

    // Map replies recursively (manual handling to avoid MapStruct recursion issues)
    default List<CommentResponse> mapReplies(List<Comment> replies) {
        if (replies == null || replies.isEmpty()) {
            return List.of();
        }
        return replies.stream()
                .map(reply -> {
                    CommentResponse response = toResponse(reply);
                    // Unmask seller names in replies too
                    if (isSeller(reply)) {
                        response.setUserName(reply.getUser().getFullName());
                    }
                    response.setReplies(mapReplies(reply.getReplies()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    // Helper functions
    default boolean isSeller(Comment comment) {
        return comment != null &&
                comment.getUser() != null &&
                comment.getProduct() != null &&
                comment.getProduct().getSeller() != null &&
                comment.getUser().getId().equals(comment.getProduct().getSeller().getId());
    }

    default boolean isYourself(Comment comment, Long viewerId) {
        return comment != null &&
                comment.getUser() != null &&
                viewerId != null &&
                viewerId.equals(comment.getUser().getId());
    }

    // Mask user name for privacy (show last 4 characters only)
    // Unmask if viewerId matches userId (viewing own comment)
    @Named("maskUserName")
    default String maskUserName(User user) {
        if (user == null || user.getFullName() == null) {
            return null;
        }
        String fullName = user.getFullName();
        if (fullName.length() <= 4) {
            return "****" + fullName;
        }
        String visiblePart = fullName.substring(fullName.length() - 4);
        return "****" + visiblePart;
    }

    // Map with conditional unmasking based on viewer context
    // - Seller names are ALWAYS unmasked (they're public figures)
    // - Product sellers viewing their own product see all names
    // - Users see their own names
    // - Others see masked names
    default CommentResponse toResponseWithViewer(Comment comment, Long viewerId, boolean isProductSeller) {
        CommentResponse response = toResponse(comment);

        // Calculate flags for frontend
        boolean isCommentBySeller = isSeller(comment);
        boolean isCommentByYourself = isYourself(comment, viewerId);

        // Set helper flags
        response.setIsYourself(isCommentByYourself);
        response.setIsProductSeller(isCommentBySeller);

        if (comment.getUser() != null) {
            if (isProductSeller || isCommentBySeller || isCommentByYourself) {
                response.setUserName(comment.getUser().getFullName());
            }
        }

        // Recursively handle replies with same logic
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            response.setReplies(comment.getReplies().stream()
                    .map(reply -> toResponseWithViewer(reply, viewerId, isProductSeller))
                    .collect(Collectors.toList()));
        }

        return response;
    }
}
