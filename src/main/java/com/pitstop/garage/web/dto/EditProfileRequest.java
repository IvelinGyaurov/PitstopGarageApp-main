package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditProfileRequest {

    @Pattern(
            regexp = "^$|^[\\p{L}][\\p{L}\\s'-]{0,18}$",
            message = "First name must be at most 20 characters and must contain only letters, spaces, hyphens or apostrophes"
    )
    private String firstName;

    @Pattern(
            regexp = "^$|^[\\p{L}][\\p{L}\\s'-]{0,18}$",
            message = "Last name must be at most 20 characters and must contain only letters, spaces, hyphens or apostrophes"
    )
    private String lastName;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9\\s-]{5,18}$",
            message = "Phone number must be at most 20 characters and must be a valid phone number"
    )
    private String phoneNumber;

    @Pattern(
            regexp = "^$|^https?://.+",
            message = "Picture must be at most 500 characters and must be a valid http or https URL"
    )
    private String profilePictureURL;

}
