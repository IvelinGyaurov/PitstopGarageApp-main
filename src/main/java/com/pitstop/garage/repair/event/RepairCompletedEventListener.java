package com.pitstop.garage.repair.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RepairCompletedEventListener {

    @EventListener
    public void onRepairCompleted(RepairCompletedEvent event) {
        log.info("Repair completed event received: repairId={}, clientId={}, mechanicId={}, laborCost={} EUR, parts={}",
                event.repairId(),
                event.clientId(),
                event.mechanicId(),
                event.laborCost(),
                event.partsCount());
    }
}
