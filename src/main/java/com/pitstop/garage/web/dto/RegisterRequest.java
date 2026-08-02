package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotNull(message = "{validation.username.required}")
    @NotBlank(message = "{validation.username.blank}")
    @Size(min = 3, max = 20, message = "{validation.username.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{validation.username.pattern}")
    private String username;

    @NotNull(message = "{validation.password.required}")
    @NotBlank(message = "{validation.password.blank}")
    @Size(min = 4, max = 20, message = "{validation.password.size}")
    private String password;

    @NotNull(message = "{validation.email.required}")
    @NotBlank(message = "{validation.email.blank}")
    @Email(message = "{validation.email.format}")
    @Size(max = 100, message = "{validation.email.size}")
    private String email;

}
