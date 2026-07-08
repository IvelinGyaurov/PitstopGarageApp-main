package com.pitstop.garage.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PartResponse {

    private UUID id;
    private String name;
    private String sku;
    private BigDecimal unitPrice;
    private int quantityInStock;
}