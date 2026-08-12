package com.pitstop.garage.web;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.web.dto.RequestRepairRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairControllerTest {

    @Mock
    private CarService carService;

    @Mock
    private RepairService repairService;

    @Mock
    private MessageHelper messages;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private RepairController repairController;

    @BeforeEach
    void stubMessages() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void repairsPreview_returnsRepairsView() {
        PitstopUserDetails user = principal(UserRole.USER);
        when(repairService.getInProgressRepairsForClient(user.getUserId())).thenReturn(List.of());
        when(repairService.getWaitingAcceptedRepairsForClient(user.getUserId())).thenReturn(List.of());
        when(repairService.getPendingRepairsForClient(user.getUserId())).thenReturn(List.of());

        ModelAndView mav = repairController.repairsPreview(user);

        assertEquals("repairs", mav.getViewName());
        assertEquals(List.of(), mav.getModel().get("inProgressRepairs"));
        assertEquals(List.of(), mav.getModel().get("acceptedRepairs"));
        assertEquals(List.of(), mav.getModel().get("pendingRepairs"));
    }

    @Test
    void repairsPreview_loadsSeparatedListsFromService() {
        PitstopUserDetails user = principal(UserRole.USER);
        ServiceRepair inProgress = ServiceRepair.builder().id(UUID.randomUUID()).build();
        ServiceRepair accepted = ServiceRepair.builder().id(UUID.randomUUID()).build();
        ServiceRepair pending = ServiceRepair.builder().id(UUID.randomUUID()).build();

        when(repairService.getInProgressRepairsForClient(user.getUserId())).thenReturn(List.of(inProgress));
        when(repairService.getWaitingAcceptedRepairsForClient(user.getUserId())).thenReturn(List.of(accepted));
        when(repairService.getPendingRepairsForClient(user.getUserId())).thenReturn(List.of(pending));

        ModelAndView mav = repairController.repairsPreview(user);

        assertEquals(List.of(inProgress), mav.getModel().get("inProgressRepairs"));
        assertEquals(List.of(accepted), mav.getModel().get("acceptedRepairs"));
        assertEquals(List.of(pending), mav.getModel().get("pendingRepairs"));
    }

    @Test
    void repairRequestForm_withoutCarId_returnsForm() {
        ModelAndView mav = repairController.repairRequestForm(null, principal(UserRole.USER));

        assertEquals("repair-request", mav.getViewName());
        verify(carService, never()).getMyCar(any(), any());
    }

    @Test
    void repairRequestForm_withCarId_loadsCar() {
        PitstopUserDetails user = principal(UserRole.USER);
        UUID carId = UUID.randomUUID();
        Car car = Car.builder().id(carId).vin("WBA3A5C50EK123456").build();
        when(carService.getMyCar(user.getUserId(), carId)).thenReturn(car);

        ModelAndView mav = repairController.repairRequestForm(carId, user);

        assertEquals(car, mav.getModel().get("car"));
    }

    @Test
    void cancelRepair_redirectsToRepairs() {
        PitstopUserDetails user = principal(UserRole.USER);
        UUID repairId = UUID.randomUUID();

        ModelAndView mav = repairController.cancelRepair(repairId, user, redirectAttributes);

        verify(repairService).cancelRepairByClient(user.getUserId(), repairId);
        assertEquals("redirect:/repairs", mav.getViewName());
    }

    @Test
    void submitRepairRequest_whenValid_redirects() {
        PitstopUserDetails user = principal(UserRole.USER);
        UUID carId = UUID.randomUUID();
        RequestRepairRequest request = RequestRepairRequest.builder()
                .problemDescription("Engine noise is very loud")
                .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = repairController.submitRepairRequest(
                carId, request, bindingResult, user, redirectAttributes);

        verify(repairService).requestRepair(user.getUserId(), carId, request);
        assertEquals("redirect:/repairs", mav.getViewName());
    }

    @Test
    void submitRepairRequest_whenInvalid_staysOnForm() {
        PitstopUserDetails user = principal(UserRole.USER);
        UUID carId = UUID.randomUUID();
        RequestRepairRequest request = new RequestRepairRequest();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getModel()).thenReturn(new java.util.HashMap<>());
        when(carService.getMyCar(user.getUserId(), carId)).thenReturn(Car.builder().id(carId).build());

        ModelAndView mav = repairController.submitRepairRequest(
                carId, request, bindingResult, user, redirectAttributes);

        assertEquals("repair-request", mav.getViewName());
        verify(repairService, never()).requestRepair(any(), any(), any());
    }

    @Test
    void repairHistory_returnsHistoryView() {
        PitstopUserDetails user = principal(UserRole.USER);
        when(repairService.getCompletedRepairsForClient(user.getUserId())).thenReturn(List.of());
        when(repairService.getRejectedRepairsForClient(user.getUserId())).thenReturn(List.of());
        when(repairService.getExpiredRepairsForClient(user.getUserId())).thenReturn(List.of());

        ModelAndView mav = repairController.repairHistory(user);

        assertEquals("repairs-history", mav.getViewName());
        assertEquals(List.of(), mav.getModel().get("completedRepairs"));
        assertEquals(List.of(), mav.getModel().get("rejectedRepairs"));
        assertEquals(List.of(), mav.getModel().get("expiredRepairs"));
    }

    @Test
    void repairHistory_loadsListsFromService() {
        PitstopUserDetails user = principal(UserRole.USER);
        ServiceRepair completed = ServiceRepair.builder().id(UUID.randomUUID()).build();
        ServiceRepair rejected = ServiceRepair.builder().id(UUID.randomUUID()).build();
        ServiceRepair expired = ServiceRepair.builder().id(UUID.randomUUID()).build();

        when(repairService.getCompletedRepairsForClient(user.getUserId())).thenReturn(List.of(completed));
        when(repairService.getRejectedRepairsForClient(user.getUserId())).thenReturn(List.of(rejected));
        when(repairService.getExpiredRepairsForClient(user.getUserId())).thenReturn(List.of(expired));

        ModelAndView mav = repairController.repairHistory(user);

        assertEquals(List.of(completed), mav.getModel().get("completedRepairs"));
        assertEquals(List.of(rejected), mav.getModel().get("rejectedRepairs"));
        assertEquals(List.of(expired), mav.getModel().get("expiredRepairs"));
    }

    @Test
    void viewRepairDetails_returnsDetails() {
        PitstopUserDetails user = principal(UserRole.USER);
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = ServiceRepair.builder().id(repairId).build();
        when(repairService.getRepairForClient(user.getUserId(), repairId)).thenReturn(repair);

        ModelAndView mav = repairController.viewRepairDetails(repairId, user);

        assertEquals("repair-details", mav.getViewName());
        assertEquals("CLIENT", mav.getModel().get("detailsAudience"));
    }

    private PitstopUserDetails principal(UserRole role) {
        return new PitstopUserDetails(UUID.randomUUID(), "user", "pass", role, true);
    }
}
