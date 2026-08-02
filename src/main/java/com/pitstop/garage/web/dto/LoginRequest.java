package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class LoginRequest {

    @NotBlank
    @Size(min = 3,max = 20, message = "{validation.username.size}")
    public String username;

    @NotBlank
    @Size(min = 4, max = 20, message = "{validation.password.size}")
    public String password;
}
