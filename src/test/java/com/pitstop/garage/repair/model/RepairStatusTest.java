package com.pitstop.garage.repair.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepairStatusTest {

    @Test
    void eachStatus_exposesDisplayName() {
        assertEquals("Pending", RepairStatus.PENDING.getDisplayName());
        assertEquals("Accepted", RepairStatus.ACCEPTED.getDisplayName());
        assertEquals("In progress", RepairStatus.IN_PROGRESS.getDisplayName());
        assertEquals("Completed", RepairStatus.COMPLETED.getDisplayName());
        assertEquals("Cancelled", RepairStatus.CANCELLED.getDisplayName());
        assertEquals("Cancelled by user", RepairStatus.USER_CANCELLED.getDisplayName());
        assertEquals("Expired", RepairStatus.EXPIRED.getDisplayName());
    }
}
