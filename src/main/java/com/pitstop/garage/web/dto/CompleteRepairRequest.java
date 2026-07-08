package com.pitstop.garage.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CompleteRepairRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal laborCost;

    private List<PartUsageForm> parts = new ArrayList<>();

    @Data
    public static class PartUsageForm {
        private UUID partId;
        private boolean selected;

        @Min(1)
        private int quantity;
    }
}