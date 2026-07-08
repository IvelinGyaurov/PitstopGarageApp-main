package com.pitstop.garage.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePartRequest {

    private String name;
    private String sku;
    private BigDecimal unitPrice;
    private int quantityInStock;
}