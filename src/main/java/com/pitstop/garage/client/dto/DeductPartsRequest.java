package com.pitstop.garage.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeductPartsRequest {

    private List<DeductPartItemRequest> items;
}