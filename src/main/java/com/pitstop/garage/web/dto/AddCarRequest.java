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

    @NotBlank(message = "VIN is required")
    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    @Pattern(
            regexp = "^[A-HJ-NPR-Z0-9]{17}$",
            message = "VIN must contain only valid letters and digits (no I, O, Q)"
    )
    private String vin;

    @NotBlank(message = "Plate number is required")
    @Size(max = 10, message = "Plate number must be at most 10 characters")
    @Pattern(
            regexp = "^[\\p{L}0-9\\s-]+$",
            message = "Plate number contains invalid characters"
    )
    private String plateNumber;

    @NotBlank(message = "Brand is required")
    @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    private String model;

    @NotBlank(message = "Engine type is required")
    @Pattern(
            regexp = "^(PETROL|DIESEL|ELECTRIC|HYBRID|LPG)$",
            message = "Engine type must be PETROL, DIESEL, ELECTRIC, HYBRID or LPG"
    )
    private String engineType;

    @NotBlank(message = "Transmission is required")
    @Pattern(
            regexp = "^(MANUAL|AUTOMATIC)$",
            message = "Transmission must be MANUAL or AUTOMATIC"
    )
    private String transmission;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be 1900 or later")
    @Max(value = 2030, message = "Year is not valid")
    private Integer year;

    @NotNull(message = "Mileage is required")
    @Min(value = 0, message = "Mileage cannot be negative")
    @Max(value = 2_000_000, message = "Mileage is not valid")
    private Integer mileage;

}
