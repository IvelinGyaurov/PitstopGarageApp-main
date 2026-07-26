package com.pitstop.garage.repair.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceRepairTest {

    @Test
    void onCreate_setsDefaultsWhenMissing() throws Exception {
        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription("Engine noise problem description")
                .build();

        Method onCreate = ServiceRepair.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(repair);

        assertNotNull(repair.getCreatedOn());
        assertEquals(RepairStatus.PENDING, repair.getStatus());
    }

    @Test
    void onCreate_keepsExistingCreatedOnAndStatus() throws Exception {
        LocalDateTime existing = LocalDateTime.of(2024, 3, 10, 12, 0);
        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription("Engine noise problem description")
                .createdOn(existing)
                .status(RepairStatus.ACCEPTED)
                .build();

        Method onCreate = ServiceRepair.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(repair);

        assertEquals(existing, repair.getCreatedOn());
        assertEquals(RepairStatus.ACCEPTED, repair.getStatus());
    }
}
