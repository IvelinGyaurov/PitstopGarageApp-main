package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCarRequest {

    @NotBlank(message = "{validation.vin.required}")
    @Size(min = 17, max = 17, message = "{validation.vin.size}")
    @Pattern(
            regexp = "^[A-HJ-NPR-Z0-9]{17}$",
            message = "{validation.vin.pattern}"
    )
    private String vin;

    @NotBlank(message = "{validation.plate.required}")
    @Size(max = 10, message = "{validation.plate.size}")
    @Pattern(
            regexp = "^[\\p{L}0-9\\s-]+$",
            message = "{validation.plate.pattern}"
    )
    private String plateNumber;

    @NotBlank(message = "{validation.brand.required}")
    @Size(min = 2, max = 50, message = "{validation.brand.size}")
    private String brand;

    @NotBlank(message = "{validation.model.required}")
    @Size(min = 1, max = 50, message = "{validation.model.size}")
    private String model;

    @NotBlank(message = "{validation.engine.required}")
    @Pattern(
            regexp = "^(PETROL|DIESEL|ELECTRIC|HYBRID|LPG)$",
            message = "{validation.engine.pattern}"
    )
    private String engineType;

    @NotBlank(message = "{validation.transmission.required}")
    @Pattern(
            regexp = "^(MANUAL|AUTOMATIC)$",
            message = "{validation.transmission.pattern}"
    )
    private String transmission;

    @NotNull(message = "{validation.year.required}")
    @Min(value = 1900, message = "{validation.year.min}")
    @Max(value = 2030, message = "{validation.year.max}")
    private Integer year;

    @NotNull(message = "{validation.mileage.required}")
    @Min(value = 0, message = "{validation.mileage.min}")
    @Max(value = 2_000_000, message = "{validation.mileage.max}")
    private Integer mileage;

}
