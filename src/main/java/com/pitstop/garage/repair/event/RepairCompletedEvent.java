package com.pitstop.garage.repair.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RepairCompletedEvent(
        UUID repairId,
        UUID clientId,
        UUID mechanicId,
        BigDecimal laborCost,
        int partsCount,
        LocalDateTime completedAt
) {
}
