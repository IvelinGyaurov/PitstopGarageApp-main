package com.pitstop.garage.job;

import com.pitstop.garage.repair.service.RepairService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksTest {

    @Mock
    private RepairService repairService;

    @InjectMocks
    private ScheduledTasks scheduledTasks;

    @Test
    void releaseStaleAcceptedRepairs_delegatesWithSevenDays() {
        when(repairService.releaseStaleAcceptedRepairs(7)).thenReturn(2);

        scheduledTasks.releaseStaleAcceptedRepairs();

        verify(repairService).releaseStaleAcceptedRepairs(7);
    }

    @Test
    void expirePendingRepairs_delegatesWithThirtyDays() {
        when(repairService.expireStalePendingRepairs(30)).thenReturn(3);

        scheduledTasks.expirePendingRepairs();

        verify(repairService).expireStalePendingRepairs(30);
    }
}
