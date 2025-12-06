package com.taitrinh.online_auction.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update/append product description")
public class UpdateProductDescriptionRequest {

    @NotBlank(message = "Additional description is required")
    @Size(min = 10, message = "Additional description must be at least 10 characters")
    @Schema(description = "Additional description to append (HTML supported)", example = "<p>Added warranty information: 1 year official warranty</p>")
    private String additionalDescription;
}
