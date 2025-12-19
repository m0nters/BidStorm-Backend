package com.taitrinh.online_auction.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    // NULL = top-level question, otherwise it's a reply to another comment
    private Long parentId;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;
}
