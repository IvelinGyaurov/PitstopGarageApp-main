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

    @Scheduled(fixedDelay = 86_400_000, initialDelay = 60_000)
    public void releaseStaleAcceptedRepairs() {
        log.info("fixedDelay job started: releasing ACCEPTED repairs not started within 7 days");
        int released = repairService.releaseStaleAcceptedRepairs(7);
        log.info("fixedDelay job finished: released {} repair(s) back to queue", released);
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void expirePendingRepairs() {
        log.info("Monthly cron started: expiring PENDING repairs older than 30 days");
        int expired = repairService.expireStalePendingRepairs(30);
        log.info("Monthly cron finished: expired {} repair(s)", expired);
    }
}
