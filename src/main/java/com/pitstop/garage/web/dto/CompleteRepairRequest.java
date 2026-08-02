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

    @NotNull(message = "{validation.laborCost.required}")
    @DecimalMin(value = "0.00", message = "{validation.laborCost.min}")
    private BigDecimal laborCost;

    private List<PartUsageForm> parts = new ArrayList<>();

    @Data
    public static class PartUsageForm {
        private UUID partId;
        private boolean selected;

        @Min(value = 1, message = "{validation.partQty.min}")
        private int quantity;
    }
}
