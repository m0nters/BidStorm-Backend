package com.taitrinh.online_auction.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.comment.CommentResponse;
import com.taitrinh.online_auction.dto.comment.CreateCommentRequest;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments (Q&A)", description = "APIs for product Q&A with threaded discussions")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get comments for a product", description = "Retrieve all Q&A comments for a product in threaded format. "
            + "Authentication is optional - sellers see unmasked commenter names on their products.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getProductComments(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long productId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long viewerId = userDetails != null ? userDetails.getUserId() : null;
        List<CommentResponse> comments = commentService.getProductComments(productId, viewerId);
        return ResponseEntity.ok(ApiResponse.ok(comments,
                "Danh sách câu hỏi và câu trả lời đã được lấy thành công"));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a comment (question or reply)", description = "Authenticated users can ask questions about products (parentId = null). "
            + "Only sellers can reply to questions on their products (parentId = question_id). "
            + "User ID is automatically extracted from authentication token.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        CommentResponse comment = commentService.createComment(request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(comment, "Comment đã được tạo thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a comment", description = "Users can delete their own comments. "
            + "Deleting a parent comment will cascade delete all replies. "
            + "User ID is automatically extracted from authentication token.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "Comment ID", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        commentService.deleteComment(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Comment đã được xóa thành công"));
    }
}
