package com.pitstop.garage.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DeductedPartResponse {

    private UUID partId;
    private String partName;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
}