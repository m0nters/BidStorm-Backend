package com.taitrinh.online_auction.dto.comment;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;
    private Long productId;
    private Long userId;
    private String userName; // Masked for bidders, full for seller viewing their own product
    private Long parentId;
    private String content;
    private ZonedDateTime createdAt;
    private List<CommentResponse> replies; // Nested replies for threaded display
    private Boolean isQuestion; // true if parent_id is null
}
