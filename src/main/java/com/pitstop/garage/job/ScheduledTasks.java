package com.pitstop.garage.job;

import com.pitstop.garage.repair.service.RepairService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ScheduledTasks {

    private final RepairService repairService;
    public ScheduledTasks(RepairService repairService) {
        this.repairService = repairService;
    }

    //TODO implement fixedDelay
    @Scheduled(cron = "0 0 0 1 * *")
    public void expirePendingRepairs() {
        log.info("Monthly cron started: cancelling PENDING repairs older than 30 days");
        int expired = repairService.expireStalePendingRepairs(30);
        log.info("Monthly cron finished: cancelled {} repair(s)", expired);
    }
}
