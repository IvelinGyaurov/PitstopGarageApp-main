package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestRepairRequest {

    @NotBlank(message = "{validation.problem.required}")
    @Size(
            min = 10,
            max = 2000,
            message = "{validation.problem.size}"
    )
    private String problemDescription;

}
