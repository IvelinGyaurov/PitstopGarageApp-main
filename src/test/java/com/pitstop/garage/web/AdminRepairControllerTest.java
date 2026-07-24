package com.pitstop.garage.web;

import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRepairControllerTest {

    @Mock
    private RepairService repairService;

    @InjectMocks
    private AdminRepairController controller;

    @Test
    void activeRepairs_returnsView() {
        when(repairService.getPendingRepairsForAdmin()).thenReturn(List.of());
        when(repairService.getAcceptedRepairsForAdmin()).thenReturn(List.of());
        when(repairService.getInProgressRepairsForAdmin()).thenReturn(List.of());

        assertEquals("admin-repairs", controller.activeRepairs().getViewName());
    }

    @Test
    void repairHistory_returnsView() {
        when(repairService.getCompletedRepairsForAdmin()).thenReturn(List.of());
        when(repairService.getRejectedRepairsForAdmin()).thenReturn(List.of());

        assertEquals("admin-repairs-history", controller.repairHistory().getViewName());
    }

    @Test
    void repairDetails_returnsDetails() {
        UUID id = UUID.randomUUID();
        when(repairService.getRepairForAdmin(id)).thenReturn(ServiceRepair.builder().id(id).build());

        ModelAndView mav = controller.repairDetails(id);

        assertEquals("repair-details", mav.getViewName());
        assertEquals("ADMIN", mav.getModel().get("detailsAudience"));
    }
}
