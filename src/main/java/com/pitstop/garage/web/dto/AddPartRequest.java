package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddPartRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal unitPrice;

    @Min(0)
    private int quantityInStock;
}