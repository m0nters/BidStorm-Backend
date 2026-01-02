package com.taitrinh.online_auction.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update an existing review")
public class UpdateReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = -1, message = "Rating must be -1 or 1")
    @Max(value = 1, message = "Rating must be -1 or 1")
    @Schema(description = "Rating: 1 for positive, -1 for negative", example = "1", allowableValues = { "-1", "1" })
    private Short rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    @Schema(description = "Review comment", example = "Great seller, fast shipping!")
    private String comment;
}
