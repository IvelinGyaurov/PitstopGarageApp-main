package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RestockPartForm {

    @Min(value = 1, message = "{validation.quantity.min}")
    private int quantityToAdd = 1;
}
