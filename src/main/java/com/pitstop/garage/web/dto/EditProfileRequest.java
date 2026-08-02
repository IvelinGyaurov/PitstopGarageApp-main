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
            message = "{validation.firstName.pattern}"
    )
    private String firstName;

    @Pattern(
            regexp = "^$|^[\\p{L}][\\p{L}\\s'-]{0,18}$",
            message = "{validation.lastName.pattern}"
    )
    private String lastName;

    @Pattern(
            regexp = "^$|^\\+?[0-9][0-9\\s-]{5,18}$",
            message = "{validation.phone.pattern}"
    )
    private String phoneNumber;

    @Pattern(
            regexp = "^$|^https?://.+",
            message = "{validation.picture.pattern}"
    )
    private String profilePictureURL;

}
