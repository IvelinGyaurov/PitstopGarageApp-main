package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddPartRequest {

    @NotBlank(message = "{validation.partName.required}")
    @Size(max = 100, message = "{validation.partName.size}")
    private String name;

    @NotBlank(message = "{validation.sku.required}")
    @Size(max = 50, message = "{validation.sku.size}")
    private String sku;

    @NotNull(message = "{validation.unitPrice.required}")
    @DecimalMin(value = "0.01", message = "{validation.unitPrice.min}")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "{validation.quantity.min}")
    private int quantityInStock;
}
