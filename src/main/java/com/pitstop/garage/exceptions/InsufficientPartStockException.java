package com.pitstop.garage.exceptions;

import java.util.UUID;

public class InsufficientPartStockException extends RuntimeException {

    private final UUID repairId;

    public InsufficientPartStockException(String message, UUID repairId) {
        super(message);
        this.repairId = repairId;
    }

    public UUID getRepairId() {
        return repairId;
    }
}
