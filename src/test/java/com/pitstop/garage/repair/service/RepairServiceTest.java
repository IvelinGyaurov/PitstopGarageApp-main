package com.pitstop.garage.repair.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.client.PartsClient;
import com.pitstop.garage.client.dto.DeductPartsRequest;
import com.pitstop.garage.client.dto.DeductedPartResponse;
import com.pitstop.garage.client.dto.PartResponse;
import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.exceptions.InsufficientPartStockException;
import com.pitstop.garage.exceptions.InsufficientPartStockExceptionMessage;
import com.pitstop.garage.exceptions.RepairNotFoundException;
import com.pitstop.garage.exceptions.RepairStatusException;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.model.UsedPart;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.CompleteRepairRequest;
import com.pitstop.garage.web.dto.RequestRepairRequest;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    @Mock
    private ServiceRepairRepository serviceRepairRepository;

    @Mock
    private UserService userService;

    @Mock
    private CarService carService;

    @Mock
    private PartsClient partsClient;

    @Mock
    private MessageHelper messages;

    @InjectMocks
    private RepairService repairService;

    @BeforeEach
    void stubMessages() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void requestRepair_savesPendingRepair() {
        UUID clientId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        Car car = Car.builder().id(carId).vin("WBA3A5C50EK123456").owner(client).build();
        RequestRepairRequest request = RequestRepairRequest.builder()
                .problemDescription("Engine makes strange noise")
                .build();

        when(userService.getById(clientId)).thenReturn(client);
        when(carService.getMyCar(clientId, carId)).thenReturn(car);
        when(serviceRepairRepository.save(any(ServiceRepair.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        repairService.requestRepair(clientId, carId, request);

        ArgumentCaptor<ServiceRepair> captor = ArgumentCaptor.forClass(ServiceRepair.class);
        verify(serviceRepairRepository).save(captor.capture());
        ServiceRepair saved = captor.getValue();
        assertEquals(request.getProblemDescription(), saved.getProblemDescription());
        assertEquals(client, saved.getClient());
        assertEquals(car, saved.getCar());
    }

    @Test
    void cancelRepairByClient_whenPending_cancels() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        ServiceRepair repair = pendingRepair(repairId, client);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client))
                .thenReturn(Optional.of(repair));

        repairService.cancelRepairByClient(clientId, repairId);

        assertEquals(RepairStatus.USER_CANCELLED, repair.getStatus());
        verify(serviceRepairRepository).save(repair);
    }

    @Test
    void cancelRepairByClient_whenNotPending_throws() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        ServiceRepair repair = pendingRepair(repairId, client);
        repair.setStatus(RepairStatus.ACCEPTED);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client))
                .thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.cancelRepairByClient(clientId, repairId));
        verify(serviceRepairRepository, never()).save(any());
    }

    @Test
    void cancelRepairByClient_whenMissing_throws() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client))
                .thenReturn(Optional.empty());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.cancelRepairByClient(clientId, repairId));
    }

    @Test
    void acceptRepairByMechanic_whenPendingUnassigned_accepts() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        repairService.acceptRepairByMechanic(mechanicId, repairId);

        assertEquals(RepairStatus.ACCEPTED, repair.getStatus());
        assertEquals(mechanic, repair.getMechanic());
        assertNotNull(repair.getAcceptedAt());
        verify(serviceRepairRepository).save(repair);
    }

    @Test
    void acceptRepairByMechanic_whenNotPending_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));
        repair.setStatus(RepairStatus.IN_PROGRESS);

        when(userService.getById(mechanicId)).thenReturn(user(mechanicId, UserRole.MECHANIC));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.acceptRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void acceptRepairByMechanic_whenAlreadyTaken_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));
        repair.setMechanic(user(UUID.randomUUID(), UserRole.MECHANIC));

        when(userService.getById(mechanicId)).thenReturn(user(mechanicId, UserRole.MECHANIC));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.acceptRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void rejectRepairByMechanic_whenPendingUnassigned_rejects() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        repairService.rejectRepairByMechanic(mechanicId, repairId);

        assertEquals(RepairStatus.CANCELLED, repair.getStatus());
        assertEquals(mechanic, repair.getMechanic());
        assertNotNull(repair.getRejectedAt());
        verify(serviceRepairRepository).save(repair);
    }

    @Test
    void rejectRepairByMechanic_whenNotPending_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));
        repair.setStatus(RepairStatus.ACCEPTED);

        when(userService.getById(mechanicId)).thenReturn(user(mechanicId, UserRole.MECHANIC));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.rejectRepairByMechanic(mechanicId, repairId));
        verify(serviceRepairRepository, never()).save(any());
    }

    @Test
    void rejectRepairByMechanic_whenAlreadyTaken_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));
        repair.setMechanic(user(UUID.randomUUID(), UserRole.MECHANIC));

        when(userService.getById(mechanicId)).thenReturn(user(mechanicId, UserRole.MECHANIC));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.rejectRepairByMechanic(mechanicId, repairId));
        verify(serviceRepairRepository, never()).save(any());
    }

    @Test
    void rejectRepairByMechanic_whenMissing_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();

        when(userService.getById(mechanicId)).thenReturn(user(mechanicId, UserRole.MECHANIC));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.empty());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.rejectRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void startRepairByMechanic_whenAccepted_starts() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.ACCEPTED);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        repairService.startRepairByMechanic(mechanicId, repairId);

        assertEquals(RepairStatus.IN_PROGRESS, repair.getStatus());
        assertNotNull(repair.getStartedAt());
        verify(serviceRepairRepository).save(repair);
    }

    @Test
    void startRepairByMechanic_whenNotAccepted_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.PENDING);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.startRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void startRepairByMechanic_whenNotAssigned_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User otherMechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, otherMechanic, RepairStatus.ACCEPTED);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.startRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void completeRepairByMechanic_withSelectedParts_deductsViaFeign() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.IN_PROGRESS);

        CompleteRepairRequest.PartUsageForm selected = new CompleteRepairRequest.PartUsageForm();
        selected.setPartId(partId);
        selected.setSelected(true);
        selected.setQuantity(2);

        CompleteRepairRequest.PartUsageForm ignored = new CompleteRepairRequest.PartUsageForm();
        ignored.setPartId(UUID.randomUUID());
        ignored.setSelected(false);
        ignored.setQuantity(5);

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(new BigDecimal("150.00"));
        request.setParts(List.of(selected, ignored));

        DeductedPartResponse deducted = new DeductedPartResponse();
        deducted.setPartId(partId);
        deducted.setPartName("Oil Filter");
        deducted.setQuantity(2);
        deducted.setUnitPrice(new BigDecimal("25.00"));

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));
        when(partsClient.deductParts(any(DeductPartsRequest.class))).thenReturn(List.of(deducted));

        repairService.completeRepairByMechanic(mechanicId, repairId, new BigDecimal("150.00"), request);

        assertEquals(RepairStatus.COMPLETED, repair.getStatus());
        assertEquals(new BigDecimal("150.00"), repair.getLaborCost());
        assertNotNull(repair.getCompletedAt());
        assertEquals(1, repair.getUsedParts().size());
        assertEquals(partId, repair.getUsedParts().get(0).getPartId());
        verify(partsClient).deductParts(any(DeductPartsRequest.class));
        verify(serviceRepairRepository).save(repair);
    }

    @Test
    void completeRepairByMechanic_whenInsufficientStock_throwsDomainException() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.IN_PROGRESS);

        CompleteRepairRequest.PartUsageForm selected = new CompleteRepairRequest.PartUsageForm();
        selected.setPartId(partId);
        selected.setSelected(true);
        selected.setQuantity(2);

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(new BigDecimal("150.00"));
        request.setParts(List.of(selected));

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));
        when(partsClient.deductParts(any(DeductPartsRequest.class)))
                .thenThrow(mock(FeignException.BadRequest.class));

        InsufficientPartStockException ex = assertThrows(
                InsufficientPartStockException.class,
                () -> repairService.completeRepairByMechanic(mechanicId, repairId, new BigDecimal("150.00"), request));

        assertEquals(repairId, ex.getRepairId());
        assertEquals(InsufficientPartStockExceptionMessage.INSUFFICIENT_STOCK, ex.getMessage());
        assertEquals(RepairStatus.IN_PROGRESS, repair.getStatus());
        verify(serviceRepairRepository, never()).save(repair);
    }

    @Test
    void completeRepairByMechanic_withoutParts_skipsFeign() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.IN_PROGRESS);

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(new BigDecimal("80.00"));
        request.setParts(List.of());

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        repairService.completeRepairByMechanic(mechanicId, repairId, new BigDecimal("80.00"), request);

        assertEquals(RepairStatus.COMPLETED, repair.getStatus());
        assertTrue(repair.getUsedParts().isEmpty());
        verify(partsClient, never()).deductParts(any());
        verify(serviceRepairRepository).save(repair);
    }

    @Test
    void completeRepairByMechanic_whenNotInProgress_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.ACCEPTED);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setParts(List.of());

        assertThrows(RepairStatusException.class,
                () -> repairService.completeRepairByMechanic(
                        mechanicId, repairId, BigDecimal.TEN, request));
        verify(partsClient, never()).deductParts(any());
    }

    @Test
    void completeRepairByMechanic_whenNotAssigned_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User otherMechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, otherMechanic, RepairStatus.IN_PROGRESS);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setParts(List.of());

        assertThrows(RepairStatusException.class,
                () -> repairService.completeRepairByMechanic(
                        mechanicId, repairId, BigDecimal.TEN, request));
        verify(partsClient, never()).deductParts(any());
    }

    @Test
    void completeRepairByMechanic_whenMissing_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.empty());

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setParts(List.of());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.completeRepairByMechanic(
                        mechanicId, repairId, BigDecimal.TEN, request));
        verify(partsClient, never()).deductParts(any());
    }

    @Test
    void getInProgressRepairForMechanic_whenInProgress_returns() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.IN_PROGRESS);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getInProgressRepairForMechanic(mechanicId, repairId).getId());
    }

    @Test
    void getInProgressRepairForMechanic_whenNotInProgress_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.ACCEPTED);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.getInProgressRepairForMechanic(mechanicId, repairId));
    }

    @Test
    void getInProgressRepairForMechanic_whenNotAssigned_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User otherMechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, otherMechanic, RepairStatus.IN_PROGRESS);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.getInProgressRepairForMechanic(mechanicId, repairId));
    }

    @Test
    void expireStalePendingRepairs_cancelsOldPending() {
        ServiceRepair stale = pendingRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.USER));
        when(serviceRepairRepository.findAllByStatusAndCreatedOnBefore(eq(RepairStatus.PENDING), any()))
                .thenReturn(List.of(stale));

        int count = repairService.expireStalePendingRepairs(7);

        assertEquals(1, count);
        assertEquals(RepairStatus.EXPIRED, stale.getStatus());
        verify(serviceRepairRepository).saveAll(List.of(stale));
    }

    @Test
    void releaseStaleAcceptedRepairs_returnsToPending() {
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        ServiceRepair stale = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        stale.setAcceptedAt(LocalDateTime.now().minusDays(10));

        when(serviceRepairRepository.findAllByStatusAndStartedAtIsNullAndAcceptedAtBefore(
                eq(RepairStatus.ACCEPTED), any()))
                .thenReturn(List.of(stale));

        int count = repairService.releaseStaleAcceptedRepairs(7);

        assertEquals(1, count);
        assertEquals(RepairStatus.PENDING, stale.getStatus());
        assertNull(stale.getMechanic());
        assertNull(stale.getAcceptedAt());
        verify(serviceRepairRepository).saveAll(List.of(stale));
    }

    @Test
    void getCatalogParts_delegatesToFeign() {
        PartResponse part = new PartResponse();
        part.setId(UUID.randomUUID());
        part.setName("Brake Pad");
        when(partsClient.getAllParts()).thenReturn(List.of(part));

        List<PartResponse> result = repairService.getCatalogParts();

        assertEquals(1, result.size());
        assertEquals("Brake Pad", result.get(0).getName());
    }

    @Test
    void buildCompleteRepairForm_mapsCatalogParts() {
        UUID partId = UUID.randomUUID();
        PartResponse part = new PartResponse();
        part.setId(partId);
        part.setName("Oil Filter");
        when(partsClient.getAllParts()).thenReturn(List.of(part));

        CompleteRepairRequest form = repairService.buildCompleteRepairForm();

        assertNull(form.getLaborCost());
        assertEquals(1, form.getParts().size());
        assertEquals(partId, form.getParts().get(0).getPartId());
        assertEquals(1, form.getParts().get(0).getQuantity());
        assertFalse(form.getParts().get(0).isSelected());
    }

    @Test
    void getMyRepairs_combinesActiveClientRepairsInOrder() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair pending = repairWithStatus(client, null, RepairStatus.PENDING, now.minusHours(1));
        ServiceRepair accepted = repairWithStatus(client, null, RepairStatus.ACCEPTED, now.minusHours(2));
        ServiceRepair inProgress = repairWithStatus(client, null, RepairStatus.IN_PROGRESS, now.minusHours(3));
        inProgress.setStartedAt(now.minusMinutes(30));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.IN_PROGRESS))))
                .thenReturn(new ArrayList<>(List.of(inProgress)));
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.ACCEPTED))))
                .thenReturn(new ArrayList<>(List.of(accepted)));
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.PENDING))))
                .thenReturn(new ArrayList<>(List.of(pending)));

        List<ServiceRepair> result = repairService.getMyRepairs(clientId);

        assertEquals(3, result.size());
        assertEquals(RepairStatus.IN_PROGRESS, result.get(0).getStatus());
        assertEquals(RepairStatus.ACCEPTED, result.get(1).getStatus());
        assertEquals(RepairStatus.PENDING, result.get(2).getStatus());
    }

    @Test
    void getPendingRepairsForClient_returnsOnlyPendingSortedByCreatedOnDesc() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair newer = repairWithStatus(client, null, RepairStatus.PENDING, now.minusHours(1));
        ServiceRepair older = repairWithStatus(client, null, RepairStatus.PENDING, now.minusHours(3));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.PENDING))))
                .thenReturn(new ArrayList<>(List.of(older, newer)));

        List<ServiceRepair> result = repairService.getPendingRepairsForClient(clientId);

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getInProgressAndWaitingAcceptedRepairsForClient_returnSeparateLists() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair inProgress = repairWithStatus(client, mechanic, RepairStatus.IN_PROGRESS, now.minusHours(2));
        inProgress.setStartedAt(now.minusMinutes(30));
        ServiceRepair accepted = repairWithStatus(client, mechanic, RepairStatus.ACCEPTED, now.minusHours(3));
        accepted.setAcceptedAt(now.minusHours(1));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.IN_PROGRESS))))
                .thenReturn(new ArrayList<>(List.of(inProgress)));
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.ACCEPTED))))
                .thenReturn(new ArrayList<>(List.of(accepted)));

        assertEquals(1, repairService.getInProgressRepairsForClient(clientId).size());
        assertEquals(RepairStatus.IN_PROGRESS,
                repairService.getInProgressRepairsForClient(clientId).get(0).getStatus());
        assertEquals(1, repairService.getWaitingAcceptedRepairsForClient(clientId).size());
        assertEquals(RepairStatus.ACCEPTED,
                repairService.getWaitingAcceptedRepairsForClient(clientId).get(0).getStatus());
    }

    @Test
    void getInProgressRepairsForClient_sortsByStartedAtDesc() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair older = repairWithStatus(client, mechanic, RepairStatus.IN_PROGRESS, now.minusHours(4));
        older.setStartedAt(now.minusHours(2));
        ServiceRepair newer = repairWithStatus(client, mechanic, RepairStatus.IN_PROGRESS, now.minusHours(5));
        newer.setStartedAt(now.minusMinutes(20));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.IN_PROGRESS))))
                .thenReturn(new ArrayList<>(List.of(older, newer)));

        List<ServiceRepair> result = repairService.getInProgressRepairsForClient(clientId);

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getWaitingAcceptedRepairsForClient_sortsByAcceptedAtDesc() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair older = repairWithStatus(client, mechanic, RepairStatus.ACCEPTED, now.minusHours(4));
        older.setAcceptedAt(now.minusHours(3));
        ServiceRepair newer = repairWithStatus(client, mechanic, RepairStatus.ACCEPTED, now.minusHours(5));
        newer.setAcceptedAt(now.minusHours(1));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.ACCEPTED))))
                .thenReturn(new ArrayList<>(List.of(older, newer)));

        List<ServiceRepair> result = repairService.getWaitingAcceptedRepairsForClient(clientId);

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getInProgressRepairsForClient_whenStartedAtMissing_usesAcceptedAtForSorting() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair withAcceptedAt = repairWithStatus(client, mechanic, RepairStatus.IN_PROGRESS, now.minusHours(5));
        withAcceptedAt.setStartedAt(null);
        withAcceptedAt.setAcceptedAt(now.minusHours(1));

        ServiceRepair fallbackCreatedOn = repairWithStatus(client, mechanic, RepairStatus.IN_PROGRESS, now.minusHours(2));
        fallbackCreatedOn.setStartedAt(null);
        fallbackCreatedOn.setAcceptedAt(null);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.IN_PROGRESS))))
                .thenReturn(new ArrayList<>(List.of(fallbackCreatedOn, withAcceptedAt)));

        List<ServiceRepair> result = repairService.getInProgressRepairsForClient(clientId);

        assertEquals(2, result.size());
        assertEquals(withAcceptedAt.getId(), result.get(0).getId());
        assertEquals(fallbackCreatedOn.getId(), result.get(1).getId());
    }

    @Test
    void getWaitingAcceptedRepairsForClient_whenAcceptedAtMissing_usesCreatedOnForSorting() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair newer = repairWithStatus(client, mechanic, RepairStatus.ACCEPTED, now.minusHours(1));
        newer.setAcceptedAt(null);
        ServiceRepair older = repairWithStatus(client, mechanic, RepairStatus.ACCEPTED, now.minusHours(3));
        older.setAcceptedAt(null);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.ACCEPTED))))
                .thenReturn(new ArrayList<>(List.of(older, newer)));

        List<ServiceRepair> result = repairService.getWaitingAcceptedRepairsForClient(clientId);

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getAcceptedRepairsForMechanic_usesDefaultStatusPriorityForUnknownStatuses() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair inProgress = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        inProgress.setStartedAt(now);
        ServiceRepair completed = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.COMPLETED);
        completed.setAcceptedAt(now.minusHours(1));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(eq(mechanic), any()))
                .thenReturn(new ArrayList<>(List.of(completed, inProgress)));

        List<ServiceRepair> result = repairService.getAcceptedRepairsForMechanic(mechanicId);

        assertEquals(RepairStatus.IN_PROGRESS, result.get(0).getStatus());
        assertEquals(RepairStatus.COMPLETED, result.get(1).getStatus());
    }

    @Test
    void getRepairForClient_whenFound_returns() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        ServiceRepair repair = pendingRepair(repairId, client);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client))
                .thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getRepairForClient(clientId, repairId).getId());
    }

    @Test
    void getRepairForMechanic_whenCompleted_returns() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.COMPLETED);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getRepairForMechanic(mechanicId, repairId).getId());
    }

    @Test
    void getRepairForMechanic_whenCancelled_returns() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.CANCELLED);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getRepairForMechanic(mechanicId, repairId).getId());
    }

    @Test
    void startRepairByMechanic_whenMechanicNull_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));
        repair.setStatus(RepairStatus.ACCEPTED);
        repair.setMechanic(null);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairStatusException.class,
                () -> repairService.startRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void getRepairForMechanic_whenStillActive_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.IN_PROGRESS);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertThrows(RepairNotFoundException.class,
                () -> repairService.getRepairForMechanic(mechanicId, repairId));
    }

    @Test
    void getRepairForAdmin_whenMissing_throws() {
        UUID repairId = UUID.randomUUID();
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.empty());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.getRepairForAdmin(repairId));
    }

    @Test
    void getPendingRepairsForMechanics_delegatesToRepository() {
        List<ServiceRepair> pending = List.of(pendingRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.USER)));
        when(serviceRepairRepository.findAllByStatusAndMechanicIsNullOrderByCreatedOnDesc(RepairStatus.PENDING))
                .thenReturn(pending);

        assertEquals(1, repairService.getPendingRepairsForMechanics().size());
    }

    @Test
    void getAcceptedRepairsForMechanic_returnsSortedList() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair accepted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        ServiceRepair inProgress = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        inProgress.setStartedAt(LocalDateTime.now());

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(eq(mechanic), any()))
                .thenReturn(new ArrayList<>(List.of(accepted, inProgress)));

        List<ServiceRepair> result = repairService.getAcceptedRepairsForMechanic(mechanicId);

        assertEquals(RepairStatus.IN_PROGRESS, result.get(0).getStatus());
    }

    @Test
    void getAcceptedRepairsForMechanic_sortsPendingAfterInProgressAndAccepted() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair pending = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.PENDING);
        ServiceRepair accepted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        accepted.setAcceptedAt(now.minusHours(2));
        ServiceRepair inProgress = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        inProgress.setStartedAt(now.minusMinutes(30));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(eq(mechanic), any()))
                .thenReturn(new ArrayList<>(List.of(pending, accepted, inProgress)));

        List<ServiceRepair> result = repairService.getAcceptedRepairsForMechanic(mechanicId);

        assertEquals(3, result.size());
        assertEquals(RepairStatus.IN_PROGRESS, result.get(0).getStatus());
        assertEquals(RepairStatus.ACCEPTED, result.get(1).getStatus());
        assertEquals(RepairStatus.PENDING, result.get(2).getStatus());
    }

    @Test
    void getInProgressAndWaitingAcceptedRepairsForMechanic_returnSeparateLists() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair inProgress = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        inProgress.setStartedAt(LocalDateTime.now());
        ServiceRepair accepted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic, List.of(RepairStatus.IN_PROGRESS)))
                .thenReturn(List.of(inProgress));
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic, List.of(RepairStatus.ACCEPTED)))
                .thenReturn(List.of(accepted));

        assertEquals(1, repairService.getInProgressRepairsForMechanic(mechanicId).size());
        assertEquals(RepairStatus.IN_PROGRESS,
                repairService.getInProgressRepairsForMechanic(mechanicId).get(0).getStatus());
        assertEquals(1, repairService.getWaitingAcceptedRepairsForMechanic(mechanicId).size());
        assertEquals(RepairStatus.ACCEPTED,
                repairService.getWaitingAcceptedRepairsForMechanic(mechanicId).get(0).getStatus());
    }

    @Test
    void getRejectedAndCompletedRepairsForMechanic_returnLists() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(eq(mechanic), any()))
                .thenReturn(List.of(assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.CANCELLED)))
                .thenReturn(List.of(assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.COMPLETED)));

        assertEquals(1, repairService.getRejectedRepairsForMechanic(mechanicId).size());
        assertEquals(1, repairService.getCompletedRepairsForMechanic(mechanicId).size());
    }

    @Test
    void getCompletedAndRejectedRepairsForClient_returnLists() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), any()))
                .thenReturn(List.of(pendingRepair(UUID.randomUUID(), client)))
                .thenReturn(List.of(pendingRepair(UUID.randomUUID(), client)))
                .thenReturn(List.of(pendingRepair(UUID.randomUUID(), client)));

        assertEquals(1, repairService.getCompletedRepairsForClient(clientId).size());
        assertEquals(1, repairService.getRejectedRepairsForClient(clientId).size());
        assertEquals(1, repairService.getExpiredRepairsForClient(clientId).size());
    }

    @Test
    void getAdminRepairLists_delegateAndSort() {
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.PENDING))
                .thenReturn(List.of(pendingRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.USER))));
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.ACCEPTED))
                .thenReturn(List.of(assignedRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.MECHANIC), RepairStatus.ACCEPTED)));
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.IN_PROGRESS))
                .thenReturn(List.of(assignedRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.MECHANIC), RepairStatus.IN_PROGRESS)));
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.COMPLETED))
                .thenReturn(List.of(assignedRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.MECHANIC), RepairStatus.COMPLETED)));
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.EXPIRED))
                .thenReturn(List.of(pendingRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.USER))));
        when(serviceRepairRepository.findAllByStatusInOrderByCreatedOnDesc(any()))
                .thenReturn(List.of(pendingRepair(UUID.randomUUID(), user(UUID.randomUUID(), UserRole.USER))));

        assertEquals(1, repairService.getPendingRepairsForAdmin().size());
        assertEquals(1, repairService.getAcceptedRepairsForAdmin().size());
        assertEquals(1, repairService.getInProgressRepairsForAdmin().size());
        assertEquals(1, repairService.getCompletedRepairsForAdmin().size());
        assertEquals(1, repairService.getRejectedRepairsForAdmin().size());
        assertEquals(1, repairService.getExpiredRepairsForAdmin().size());
        assertEquals(1, repairService.getExpiredRepairsForMechanic().size());
    }

    @Test
    void getRepairForAdmin_whenFound_returns() {
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = pendingRepair(repairId, user(UUID.randomUUID(), UserRole.USER));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getRepairForAdmin(repairId).getId());
    }

    @Test
    void acceptRepairByMechanic_whenMissing_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        when(userService.getById(mechanicId)).thenReturn(user(mechanicId, UserRole.MECHANIC));
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.empty());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.acceptRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void startRepairByMechanic_whenMissing_throws() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.empty());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.startRepairByMechanic(mechanicId, repairId));
    }

    @Test
    void getRepairForClient_whenMissing_throws() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client)).thenReturn(Optional.empty());

        assertThrows(RepairNotFoundException.class,
                () -> repairService.getRepairForClient(clientId, repairId));
    }

    @Test
    void rejectInvalidSelectedPartQuantities_whenPartsNull_noErrors() {
        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setParts(null);
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "completeRepairRequest");

        repairService.rejectInvalidSelectedPartQuantities(request, bindingResult);

        assertFalse(bindingResult.hasErrors());
    }

    @Test
    void rejectInvalidSelectedPartQuantities_whenSelectedQtyInvalid_addsFieldError() {
        CompleteRepairRequest request = new CompleteRepairRequest();
        CompleteRepairRequest.PartUsageForm invalid = new CompleteRepairRequest.PartUsageForm();
        invalid.setPartId(UUID.randomUUID());
        invalid.setSelected(true);
        invalid.setQuantity(0);

        CompleteRepairRequest.PartUsageForm unselected = new CompleteRepairRequest.PartUsageForm();
        unselected.setPartId(UUID.randomUUID());
        unselected.setSelected(false);
        unselected.setQuantity(0);

        request.setParts(List.of(invalid, unselected));
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "completeRepairRequest");

        repairService.rejectInvalidSelectedPartQuantities(request, bindingResult);

        assertTrue(bindingResult.hasFieldErrors("parts[0].quantity"));
        assertFalse(bindingResult.hasFieldErrors("parts[1].quantity"));
        assertEquals("validation.partQty.min", bindingResult.getFieldError("parts[0].quantity").getDefaultMessage());
    }

    @Test
    void rejectInvalidSelectedPartQuantities_whenSelectedQtyValid_noErrors() {
        CompleteRepairRequest request = new CompleteRepairRequest();
        CompleteRepairRequest.PartUsageForm valid = new CompleteRepairRequest.PartUsageForm();
        valid.setPartId(UUID.randomUUID());
        valid.setSelected(true);
        valid.setQuantity(2);
        request.setParts(List.of(valid));
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "completeRepairRequest");

        repairService.rejectInvalidSelectedPartQuantities(request, bindingResult);

        assertFalse(bindingResult.hasErrors());
    }

    @Test
    void completeRepairByMechanic_selectedWithZeroQuantity_skipsFeign() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        ServiceRepair repair = assignedRepair(repairId, mechanic, RepairStatus.IN_PROGRESS);

        CompleteRepairRequest.PartUsageForm selectedZero = new CompleteRepairRequest.PartUsageForm();
        selectedZero.setPartId(UUID.randomUUID());
        selectedZero.setSelected(true);
        selectedZero.setQuantity(0);

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(new BigDecimal("50.00"));
        request.setParts(List.of(selectedZero));

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        repairService.completeRepairByMechanic(mechanicId, repairId, new BigDecimal("50.00"), request);

        verify(partsClient, never()).deductParts(any());
        assertEquals(RepairStatus.COMPLETED, repair.getStatus());
    }

    @Test
    void getRejectedRepairsForClient_sortsByRejectedAtOrCreatedOn() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusHours(1);

        ServiceRepair withRejectedAt = pendingRepair(UUID.randomUUID(), client);
        withRejectedAt.setStatus(RepairStatus.CANCELLED);
        withRejectedAt.setCreatedOn(older);
        withRejectedAt.setRejectedAt(newer);

        ServiceRepair withoutRejectedAt = pendingRepair(UUID.randomUUID(), client);
        withoutRejectedAt.setStatus(RepairStatus.USER_CANCELLED);
        withoutRejectedAt.setCreatedOn(older.plusHours(1));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), any()))
                .thenReturn(new ArrayList<>(List.of(withoutRejectedAt, withRejectedAt)));

        List<ServiceRepair> result = repairService.getRejectedRepairsForClient(clientId);

        assertEquals(withRejectedAt.getId(), result.get(0).getId());
        assertEquals(withoutRejectedAt.getId(), result.get(1).getId());
    }

    @Test
    void getAcceptedRepairsForMechanic_sortsByActiveDateBranches() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime base = LocalDateTime.now().minusDays(1);

        ServiceRepair inProgressWithStart = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        inProgressWithStart.setAcceptedAt(null);
        inProgressWithStart.setStartedAt(base.plusHours(5));
        inProgressWithStart.setCreatedOn(base);

        ServiceRepair inProgressFallbackCreated = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        inProgressFallbackCreated.setAcceptedAt(null);
        inProgressFallbackCreated.setStartedAt(null);
        inProgressFallbackCreated.setCreatedOn(base.plusHours(2));

        ServiceRepair acceptedWithAcceptedAt = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        acceptedWithAcceptedAt.setAcceptedAt(base.plusHours(4));
        acceptedWithAcceptedAt.setCreatedOn(base);

        ServiceRepair acceptedFallbackCreated = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        acceptedFallbackCreated.setAcceptedAt(null);
        acceptedFallbackCreated.setCreatedOn(base.plusHours(1));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(eq(mechanic), any()))
                .thenReturn(new ArrayList<>(List.of(
                        acceptedFallbackCreated,
                        inProgressFallbackCreated,
                        acceptedWithAcceptedAt,
                        inProgressWithStart)));

        List<ServiceRepair> result = repairService.getAcceptedRepairsForMechanic(mechanicId);

        assertEquals(inProgressWithStart.getId(), result.get(0).getId());
        assertEquals(inProgressFallbackCreated.getId(), result.get(1).getId());
        assertEquals(acceptedWithAcceptedAt.getId(), result.get(2).getId());
        assertEquals(acceptedFallbackCreated.getId(), result.get(3).getId());
    }

    @Test
    void getRejectedRepairsForMechanic_sortsByRejectedAtOrCreatedOn() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime older = LocalDateTime.now().minusDays(3);
        LocalDateTime newer = LocalDateTime.now().minusHours(2);

        ServiceRepair withRejectedAt = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.CANCELLED);
        withRejectedAt.setRejectedAt(newer);
        withRejectedAt.setCreatedOn(older);

        ServiceRepair withoutRejectedAt = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.CANCELLED);
        withoutRejectedAt.setRejectedAt(null);
        withoutRejectedAt.setCreatedOn(older.plusDays(1));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(eq(mechanic), any()))
                .thenReturn(new ArrayList<>(List.of(withoutRejectedAt, withRejectedAt)));

        List<ServiceRepair> result = repairService.getRejectedRepairsForMechanic(mechanicId);

        assertEquals(withRejectedAt.getId(), result.get(0).getId());
        assertEquals(withoutRejectedAt.getId(), result.get(1).getId());
    }

    @Test
    void getCompletedRepairsForClient_sortsByCompletedAtDesc() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusHours(3);

        ServiceRepair olderCompleted = repairWithStatus(client, null, RepairStatus.COMPLETED, older);
        olderCompleted.setCompletedAt(older);

        ServiceRepair newerCompleted = repairWithStatus(client, null, RepairStatus.COMPLETED, newer);
        newerCompleted.setCompletedAt(newer);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.COMPLETED))))
                .thenReturn(new ArrayList<>(List.of(olderCompleted, newerCompleted)));

        List<ServiceRepair> result = repairService.getCompletedRepairsForClient(clientId);

        assertEquals(newerCompleted.getId(), result.get(0).getId());
        assertEquals(olderCompleted.getId(), result.get(1).getId());
    }

    @Test
    void getExpiredRepairsForClient_sortsByCreatedOnDesc() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        LocalDateTime older = LocalDateTime.now().minusDays(5);
        LocalDateTime newer = LocalDateTime.now().minusDays(1);

        ServiceRepair olderExpired = repairWithStatus(client, null, RepairStatus.EXPIRED, older);
        ServiceRepair newerExpired = repairWithStatus(client, null, RepairStatus.EXPIRED, newer);

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.EXPIRED))))
                .thenReturn(new ArrayList<>(List.of(olderExpired, newerExpired)));

        List<ServiceRepair> result = repairService.getExpiredRepairsForClient(clientId);

        assertEquals(newerExpired.getId(), result.get(0).getId());
        assertEquals(olderExpired.getId(), result.get(1).getId());
    }

    @Test
    void getInProgressRepairsForMechanic_sortsByMechanicActiveDate() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair newer = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        newer.setStartedAt(now.minusMinutes(20));
        ServiceRepair older = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        older.setStartedAt(now.minusHours(2));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                eq(mechanic), eq(List.of(RepairStatus.IN_PROGRESS))))
                .thenReturn(new ArrayList<>(List.of(older, newer)));

        List<ServiceRepair> result = repairService.getInProgressRepairsForMechanic(mechanicId);

        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getWaitingAcceptedRepairsForMechanic_whenAcceptedAtMissing_usesCreatedOnForSorting() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair newer = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        newer.setAcceptedAt(null);
        newer.setCreatedOn(now.minusHours(1));

        ServiceRepair older = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        older.setAcceptedAt(null);
        older.setCreatedOn(now.minusHours(4));

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                eq(mechanic), eq(List.of(RepairStatus.ACCEPTED))))
                .thenReturn(new ArrayList<>(List.of(older, newer)));

        List<ServiceRepair> result = repairService.getWaitingAcceptedRepairsForMechanic(mechanicId);

        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getCompletedRepairsForMechanic_sortsByCompletedAtWithNullsLast() {
        UUID mechanicId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        LocalDateTime completedAt = LocalDateTime.now().minusDays(1);

        ServiceRepair withCompletedAt = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.COMPLETED);
        withCompletedAt.setCompletedAt(completedAt);

        ServiceRepair withoutCompletedAt = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.COMPLETED);
        withoutCompletedAt.setCompletedAt(null);

        when(userService.getById(mechanicId)).thenReturn(mechanic);
        when(serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                eq(mechanic), eq(List.of(RepairStatus.COMPLETED))))
                .thenReturn(new ArrayList<>(List.of(withoutCompletedAt, withCompletedAt)));

        List<ServiceRepair> result = repairService.getCompletedRepairsForMechanic(mechanicId);

        assertEquals(withCompletedAt.getId(), result.get(0).getId());
        assertEquals(withoutCompletedAt.getId(), result.get(1).getId());
    }

    @Test
    void getAdminRepairLists_sortsByActiveAndCompletedDates() {
        LocalDateTime now = LocalDateTime.now();
        User mechanic = user(UUID.randomUUID(), UserRole.MECHANIC);

        ServiceRepair olderAccepted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        olderAccepted.setAcceptedAt(now.minusHours(4));
        ServiceRepair newerAccepted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.ACCEPTED);
        newerAccepted.setAcceptedAt(now.minusHours(1));

        ServiceRepair olderInProgress = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        olderInProgress.setStartedAt(now.minusHours(3));
        ServiceRepair newerInProgress = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.IN_PROGRESS);
        newerInProgress.setStartedAt(now.minusMinutes(30));

        ServiceRepair olderCompleted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.COMPLETED);
        olderCompleted.setCompletedAt(now.minusDays(2));
        ServiceRepair newerCompleted = assignedRepair(UUID.randomUUID(), mechanic, RepairStatus.COMPLETED);
        newerCompleted.setCompletedAt(now.minusHours(6));

        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.ACCEPTED))
                .thenReturn(new ArrayList<>(List.of(olderAccepted, newerAccepted)));
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.IN_PROGRESS))
                .thenReturn(new ArrayList<>(List.of(olderInProgress, newerInProgress)));
        when(serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.COMPLETED))
                .thenReturn(new ArrayList<>(List.of(olderCompleted, newerCompleted)));

        List<ServiceRepair> accepted = repairService.getAcceptedRepairsForAdmin();
        List<ServiceRepair> inProgress = repairService.getInProgressRepairsForAdmin();
        List<ServiceRepair> completed = repairService.getCompletedRepairsForAdmin();

        assertEquals(newerAccepted.getId(), accepted.get(0).getId());
        assertEquals(newerInProgress.getId(), inProgress.get(0).getId());
        assertEquals(newerCompleted.getId(), completed.get(0).getId());
    }

    @Test
    void getMyRepairs_excludesNonActiveStatuses() {
        UUID clientId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        LocalDateTime now = LocalDateTime.now();

        ServiceRepair pending = repairWithStatus(client, null, RepairStatus.PENDING, now.minusHours(1));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.IN_PROGRESS))))
                .thenReturn(new ArrayList<>());
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.ACCEPTED))))
                .thenReturn(new ArrayList<>());
        when(serviceRepairRepository.findAllByClientAndStatusIn(eq(client), eq(List.of(RepairStatus.PENDING))))
                .thenReturn(new ArrayList<>(List.of(pending)));

        List<ServiceRepair> result = repairService.getMyRepairs(clientId);

        assertEquals(1, result.size());
        assertEquals(RepairStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void getCompletedRepairForClientInvoice_whenCompleted_returns() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        ServiceRepair repair = completedInvoiceRepair(repairId, client, user(UUID.randomUUID(), UserRole.MECHANIC));
        UsedPart part = UsedPart.builder()
                .id(UUID.randomUUID())
                .partId(UUID.randomUUID())
                .partName("Oil filter")
                .quantity(1)
                .unitPrice(new BigDecimal("12.00"))
                .serviceRepair(repair)
                .build();
        repair.setUsedParts(new ArrayList<>(List.of(part)));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getCompletedRepairForClientInvoice(clientId, repairId).getId());
    }

    @Test
    void getCompletedRepairForAdminInvoice_whenMechanicMissing_stillInitializesGraph() {
        UUID repairId = UUID.randomUUID();
        User client = user(UUID.randomUUID(), UserRole.USER);
        ServiceRepair repair = completedInvoiceRepair(repairId, client, null);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getCompletedRepairForAdminInvoice(repairId).getId());
    }

    @Test
    void getCompletedRepairForClientInvoice_whenNotCompleted_throws() {
        UUID clientId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User client = user(clientId, UserRole.USER);
        ServiceRepair repair = pendingRepair(repairId, client);
        repair.setCar(car(client));

        when(userService.getById(clientId)).thenReturn(client);
        when(serviceRepairRepository.findByIdAndClient(repairId, client)).thenReturn(Optional.of(repair));

        assertThrows(RepairNotFoundException.class,
                () -> repairService.getCompletedRepairForClientInvoice(clientId, repairId));
    }

    @Test
    void getCompletedRepairForMechanicInvoice_whenCompleted_returns() {
        UUID mechanicId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User mechanic = user(mechanicId, UserRole.MECHANIC);
        User client = user(UUID.randomUUID(), UserRole.USER);
        ServiceRepair repair = completedInvoiceRepair(repairId, client, mechanic);

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getCompletedRepairForMechanicInvoice(mechanicId, repairId).getId());
    }

    @Test
    void getCompletedRepairForAdminInvoice_whenCompleted_returns() {
        UUID repairId = UUID.randomUUID();
        User client = user(UUID.randomUUID(), UserRole.USER);
        ServiceRepair repair = completedInvoiceRepair(repairId, client, user(UUID.randomUUID(), UserRole.MECHANIC));

        when(serviceRepairRepository.findById(repairId)).thenReturn(Optional.of(repair));

        assertEquals(repairId, repairService.getCompletedRepairForAdminInvoice(repairId).getId());
    }

    @Test
    void calculatePartsTotal_andGrandTotal_sumLaborAndParts() {
        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription("Brake noise problem description")
                .laborCost(new BigDecimal("80.00"))
                .usedParts(new ArrayList<>())
                .build();

        UsedPart pads = UsedPart.builder()
                .id(UUID.randomUUID())
                .partId(UUID.randomUUID())
                .partName("Brake pads")
                .quantity(2)
                .unitPrice(new BigDecimal("45.00"))
                .serviceRepair(repair)
                .build();
        repair.setUsedParts(List.of(pads));

        assertEquals(new BigDecimal("90.00"), repairService.calculatePartsTotal(repair));
        assertEquals(new BigDecimal("170.00"), repairService.calculateGrandTotal(repair));
    }

    @Test
    void calculateGrandTotal_whenNoParts_equalsLabor() {
        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription("Diagnostics only repair description")
                .laborCost(new BigDecimal("50.00"))
                .usedParts(new ArrayList<>())
                .build();

        assertEquals(new BigDecimal("0.00"), repairService.calculatePartsTotal(repair));
        assertEquals(new BigDecimal("50.00"), repairService.calculateGrandTotal(repair));
    }

    @Test
    void calculatePartsTotal_whenUsedPartsNull_returnsZero() {
        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription("Diagnostics only repair description")
                .laborCost(new BigDecimal("40.00"))
                .usedParts(null)
                .build();

        assertEquals(new BigDecimal("0.00"), repairService.calculatePartsTotal(repair));
        assertEquals(new BigDecimal("40.00"), repairService.calculateGrandTotal(repair));
    }

    @Test
    void calculateGrandTotal_whenLaborNull_usesZeroLabor() {
        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription("Legacy completed repair without labor")
                .laborCost(null)
                .usedParts(new ArrayList<>())
                .build();

        assertEquals(new BigDecimal("0.00"), repairService.calculateGrandTotal(repair));
    }

    private User user(UUID id, UserRole role) {
        return User.builder()
                .id(id)
                .username("user-" + id.toString().substring(0, 8))
                .email(id + "@mail.com")
                .password("encoded")
                .role(role)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    private ServiceRepair pendingRepair(UUID id, User client) {
        return ServiceRepair.builder()
                .id(id)
                .problemDescription("Strange noise from engine")
                .status(RepairStatus.PENDING)
                .client(client)
                .createdOn(LocalDateTime.now())
                .usedParts(new ArrayList<>())
                .build();
    }

    private ServiceRepair assignedRepair(UUID id, User mechanic, RepairStatus status) {
        ServiceRepair repair = pendingRepair(id, user(UUID.randomUUID(), UserRole.USER));
        repair.setMechanic(mechanic);
        repair.setStatus(status);
        repair.setAcceptedAt(LocalDateTime.now().minusHours(1));
        return repair;
    }

    private ServiceRepair repairWithStatus(User client, User mechanic, RepairStatus status, LocalDateTime createdOn) {
        return ServiceRepair.builder()
                .id(UUID.randomUUID())
                .problemDescription("Issue description long enough")
                .status(status)
                .client(client)
                .mechanic(mechanic)
                .createdOn(createdOn)
                .usedParts(new ArrayList<>())
                .build();
    }

    private ServiceRepair completedInvoiceRepair(UUID id, User client, User mechanic) {
        ServiceRepair repair = ServiceRepair.builder()
                .id(id)
                .problemDescription("Completed repair for invoice")
                .status(RepairStatus.COMPLETED)
                .laborCost(new BigDecimal("80.00"))
                .client(client)
                .mechanic(mechanic)
                .car(car(client))
                .createdOn(LocalDateTime.now().minusDays(2))
                .acceptedAt(LocalDateTime.now().minusDays(1))
                .startedAt(LocalDateTime.now().minusHours(5))
                .completedAt(LocalDateTime.now())
                .usedParts(new ArrayList<>())
                .build();
        return repair;
    }

    private Car car(User owner) {
        return Car.builder()
                .id(UUID.randomUUID())
                .vin("WVWZZZ1JZXW000099")
                .plateNumber("CA9999XX")
                .brand("VW")
                .model("Golf")
                .year(2018)
                .engineType("PETROL")
                .transmission("MANUAL")
                .owner(owner)
                .build();
    }
}
