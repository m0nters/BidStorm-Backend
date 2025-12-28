package com.taitrinh.online_auction.dto.product;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Description update log")
public class DescriptionLogResponse {

    @Schema(description = "Log ID", example = "1")
    private Long id;

    @Schema(description = "Updated content", example = "Added warranty information")
    private String updatedContent;

    @Schema(description = "Update time", example = "2025-12-02T10:00:00Z")
    private ZonedDateTime updatedAt;
}
