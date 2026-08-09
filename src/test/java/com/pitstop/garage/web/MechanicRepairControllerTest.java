package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.web.dto.CompleteRepairRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MechanicRepairControllerTest {

    @Mock
    private RepairService repairService;

    @Mock
    private MessageHelper messages;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private MechanicRepairController controller;

    @BeforeEach
    void stubMessages() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void repairQueue_returnsView() {
        when(repairService.getPendingRepairsForMechanics()).thenReturn(List.of());
        assertEquals("mechanic-repairs", controller.repairQueue().getViewName());
    }

    @Test
    void acceptedRepairs_loadsSeparatedListsFromService() {
        PitstopUserDetails user = principal();
        ServiceRepair inProgress = ServiceRepair.builder().id(UUID.randomUUID()).build();
        ServiceRepair accepted = ServiceRepair.builder().id(UUID.randomUUID()).build();
        when(repairService.getInProgressRepairsForMechanic(user.getUserId()))
                .thenReturn(List.of(inProgress));
        when(repairService.getWaitingAcceptedRepairsForMechanic(user.getUserId()))
                .thenReturn(List.of(accepted));

        ModelAndView mav = controller.acceptedRepairs(user);

        assertEquals("mechanic-repairs-accepted", mav.getViewName());
        assertEquals(List.of(inProgress), mav.getModel().get("inProgressRepairs"));
        assertEquals(List.of(accepted), mav.getModel().get("acceptedRepairs"));
    }

    @Test
    void repairHistory_returnsView() {
        PitstopUserDetails user = principal();
        when(repairService.getRejectedRepairsForMechanic(user.getUserId())).thenReturn(List.of());
        when(repairService.getCompletedRepairsForMechanic(user.getUserId())).thenReturn(List.of());
        when(repairService.getExpiredRepairsForMechanic()).thenReturn(List.of());
        assertEquals("mechanic-repairs-history", controller.repairHistory(user).getViewName());
    }

    @Test
    void acceptRepair_redirectsToAccepted() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        ModelAndView mav = controller.acceptRepair(id, user, redirectAttributes);
        verify(repairService).acceptRepairByMechanic(user.getUserId(), id);
        assertEquals("redirect:/mechanic/repairs/accepted", mav.getViewName());
    }

    @Test
    void rejectRepair_redirectsToQueue() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        ModelAndView mav = controller.rejectRepair(id, user, redirectAttributes);
        verify(repairService).rejectRepairByMechanic(user.getUserId(), id);
        assertEquals("redirect:/mechanic/repairs", mav.getViewName());
    }

    @Test
    void startRepair_redirectsToAccepted() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        ModelAndView mav = controller.startRepair(id, user, redirectAttributes);
        verify(repairService).startRepairByMechanic(user.getUserId(), id);
        assertEquals("redirect:/mechanic/repairs/accepted", mav.getViewName());
    }

    @Test
    void repairDetails_returnsDetails() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        ServiceRepair repair = ServiceRepair.builder().id(id).build();
        when(repairService.getRepairForMechanic(user.getUserId(), id)).thenReturn(repair);

        ModelAndView mav = controller.repairDetails(id, user);

        assertEquals("repair-details", mav.getViewName());
        assertEquals("MECHANIC", mav.getModel().get("detailsAudience"));
    }

    @Test
    void completeRepairForm_returnsForm() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        when(repairService.getInProgressRepairForMechanic(user.getUserId(), id))
                .thenReturn(ServiceRepair.builder().id(id).build());
        when(repairService.getCatalogParts()).thenReturn(List.of());
        when(repairService.buildCompleteRepairForm()).thenReturn(new CompleteRepairRequest());

        assertEquals("mechanic-complete-repair", controller.completeRepairForm(id, user).getViewName());
    }

    @Test
    void completeRepair_whenValid_redirects() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(new BigDecimal("100.00"));
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = controller.completeRepair(id, user, request, bindingResult, redirectAttributes);

        verify(repairService).completeRepairByMechanic(user.getUserId(), id, request.getLaborCost(), request);
        assertEquals("redirect:/mechanic/repairs/accepted", mav.getViewName());
    }

    @Test
    void completeRepair_whenSelectedPartQuantityInvalid_staysOnForm() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(new BigDecimal("50.00"));

        CompleteRepairRequest.PartUsageForm part = new CompleteRepairRequest.PartUsageForm();
        part.setPartId(UUID.randomUUID());
        part.setSelected(true);
        part.setQuantity(0);
        request.setParts(List.of(part));

        when(bindingResult.hasErrors()).thenReturn(true);
        when(repairService.getInProgressRepairForMechanic(user.getUserId(), id))
                .thenReturn(ServiceRepair.builder().id(id).build());
        when(repairService.getCatalogParts()).thenReturn(List.of());

        ModelAndView mav = controller.completeRepair(id, user, request, bindingResult, redirectAttributes);

        verify(repairService).rejectInvalidSelectedPartQuantities(request, bindingResult);
        assertEquals("mechanic-complete-repair", mav.getViewName());
        verify(repairService, never()).completeRepairByMechanic(any(), any(), any(), any());
    }

    @Test
    void completeRepair_whenInvalid_staysOnForm() {
        PitstopUserDetails user = principal();
        UUID id = UUID.randomUUID();
        CompleteRepairRequest request = new CompleteRepairRequest();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(repairService.getInProgressRepairForMechanic(user.getUserId(), id))
                .thenReturn(ServiceRepair.builder().id(id).build());
        when(repairService.getCatalogParts()).thenReturn(List.of());

        ModelAndView mav = controller.completeRepair(id, user, request, bindingResult, redirectAttributes);

        assertEquals("mechanic-complete-repair", mav.getViewName());
        verify(repairService, never()).completeRepairByMechanic(any(), any(), any(), any());
    }

    private PitstopUserDetails principal() {
        return new PitstopUserDetails(UUID.randomUUID(), "mech", "pass", UserRole.MECHANIC, true);
    }
}
