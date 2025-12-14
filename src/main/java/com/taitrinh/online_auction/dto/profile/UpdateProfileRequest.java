package com.taitrinh.online_auction.dto.profile;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to partially update user profile information. All fields are optional - only send fields you want to update.")
public class UpdateProfileRequest {

    @Email(message = "Email must be valid")
    @Schema(description = "User email address (optional)", example = "john.doe@example.com", nullable = true)
    private String email;

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "User full name (optional)", example = "John Doe", nullable = true)
    private String fullName;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "User address (optional)", example = "123 Main St, City, Country", nullable = true)
    private String address;

    @Schema(description = "User birth date (optional)", example = "1990-01-15", nullable = true)
    private LocalDate birthDate;
}
