package com.pitstop.garage.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class DeductPartItemRequest {

    private UUID partId;
    private int quantity;
}