package com.pitstop.garage.repair.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.client.PartsClient;
import com.pitstop.garage.client.dto.DeductPartItemRequest;
import com.pitstop.garage.client.dto.DeductPartsRequest;
import com.pitstop.garage.client.dto.DeductedPartResponse;
import com.pitstop.garage.client.dto.PartResponse;
import com.pitstop.garage.exceptions.RepairNotFoundException;
import com.pitstop.garage.exceptions.RepairNotFoundExceptionMessage;
import com.pitstop.garage.exceptions.RepairStatusException;
import com.pitstop.garage.exceptions.RepairStatusExceptionMessage;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.model.UsedPart;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.CompleteRepairRequest;
import com.pitstop.garage.web.dto.RequestRepairRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RepairService {

    private final ServiceRepairRepository serviceRepairRepository;
    private final UserService userService;
    private final CarService carService;
    private final PartsClient partsClient;

    @Autowired
    public RepairService(ServiceRepairRepository serviceRepairRepository, UserService userService, CarService carService, PartsClient partsClient) {
        this.serviceRepairRepository = serviceRepairRepository;
        this.userService = userService;
        this.carService = carService;
        this.partsClient = partsClient;
    }

    public void requestRepair(UUID clientId, UUID carId, RequestRepairRequest requestRepair) {
        User client = userService.getById(clientId);
        Car car = carService.getMyCar(clientId, carId);

        ServiceRepair repair = ServiceRepair.builder()
                .problemDescription(requestRepair.getProblemDescription())
                .car(car)
                .client(client)
                .build();

        serviceRepairRepository.save(repair);
        log.info("Repair requested by client {} for car {}", clientId, carId);
    }

    public void cancelRepairByClient(UUID userId, UUID repairId){

        User client = userService.getById(userId);
        ServiceRepair repair = serviceRepairRepository.findByIdAndClient(repairId,client)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));

        if (repair.getStatus() != RepairStatus.PENDING){
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_STATUS_UNAUTHORIZED);
        }

        repair.setStatus(RepairStatus.USER_CANCELLED);
        serviceRepairRepository.save(repair);
        log.info("Repair {} cancelled by client {}", repairId, userId);
    }

    public void acceptRepairByMechanic(UUID mechanicId, UUID repairId) {
        User mechanic = userService.getById(mechanicId);
        ServiceRepair repair = getPendingUnassignedRepair(repairId);

        repair.setMechanic(mechanic);
        repair.setStatus(RepairStatus.ACCEPTED);
        repair.setAcceptedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
        log.info("Repair {} accepted by mechanic {}", repairId, mechanicId);
    }

    public void rejectRepairByMechanic(UUID mechanicId, UUID repairId) {
        User mechanic = userService.getById(mechanicId);
        ServiceRepair repair = getPendingUnassignedRepair(repairId);

        repair.setMechanic(mechanic);
        repair.setStatus(RepairStatus.CANCELLED);
        repair.setRejectedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
        log.info("Repair {} rejected by mechanic {}", repairId, mechanicId);
    }

    private ServiceRepair getPendingUnassignedRepair(UUID repairId) {
        ServiceRepair repair = serviceRepairRepository.findById(repairId)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));

        if (repair.getStatus() != RepairStatus.PENDING) {
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_NOT_PENDING);
        }
        if (repair.getMechanic() != null) {
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_ALREADY_TAKEN);
        }

        return repair;
    }

    private ServiceRepair getRepairAssignedToMechanic(UUID mechanicId, UUID repairId) {

        ServiceRepair repair = serviceRepairRepository.findById(repairId)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));

        if (repair.getMechanic() == null || !repair.getMechanic().getId().equals(mechanicId)) {
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_NOT_ASSIGNED_TO_MECHANIC);
        }

        return repair;
    }

    public void startRepairByMechanic(UUID mechanicId, UUID repairId) {
        ServiceRepair repair = getRepairAssignedToMechanic(mechanicId, repairId);

        if (repair.getStatus() != RepairStatus.ACCEPTED) {
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_NOT_ACCEPTED);
        }

        repair.setStatus(RepairStatus.IN_PROGRESS);
        repair.setStartedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
        log.info("Repair {} started by mechanic {}", repairId, mechanicId);
    }

    @Transactional
    public void completeRepairByMechanic(UUID mechanicId, UUID repairId,
                                         BigDecimal laborCost,
                                         CompleteRepairRequest completeRepairRequest) {
        ServiceRepair repair = getInProgressRepairForMechanic(mechanicId, repairId);

        List<DeductPartItemRequest> deductItems = completeRepairRequest.getParts().stream()
                .filter(CompleteRepairRequest.PartUsageForm::isSelected)
                .filter(item -> item.getQuantity() > 0)
                .map(item -> {
                    DeductPartItemRequest deductItem = new DeductPartItemRequest();
                    deductItem.setPartId(item.getPartId());
                    deductItem.setQuantity(item.getQuantity());
                    return deductItem;
                })
                .toList();

        if (!deductItems.isEmpty()) {
            DeductPartsRequest deductRequest = new DeductPartsRequest();
            deductRequest.setItems(deductItems);

            List<DeductedPartResponse> deducted = partsClient.deductParts(deductRequest);

            for (DeductedPartResponse d : deducted) {
                UsedPart usedPart = UsedPart.builder()
                        .partId(d.getPartId())
                        .partName(d.getPartName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .serviceRepair(repair)
                        .build();
                repair.getUsedParts().add(usedPart);
            }
        }

        repair.setLaborCost(laborCost);
        repair.setStatus(RepairStatus.COMPLETED);
        repair.setCompletedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
        log.info("Repair {} completed by mechanic {} with {} part(s)",
                repairId, mechanicId, deductItems.size());
    }

    @Transactional
    public int expireStalePendingRepairs(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);

        List<ServiceRepair> staleRepairs = serviceRepairRepository
                .findAllByStatusAndCreatedOnBefore(RepairStatus.PENDING, cutoff);

        for (ServiceRepair repair : staleRepairs) {
            repair.setStatus(RepairStatus.CANCELLED);
        }

        serviceRepairRepository.saveAll(staleRepairs);
        return staleRepairs.size();
    }

    @Transactional
    public int releaseStaleAcceptedRepairs(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);

        List<ServiceRepair> staleRepairs = serviceRepairRepository
                .findAllByStatusAndStartedAtIsNullAndAcceptedAtBefore(
                        RepairStatus.ACCEPTED, cutoff);

        for (ServiceRepair repair : staleRepairs) {
            repair.setStatus(RepairStatus.PENDING);
            repair.setMechanic(null);
            repair.setAcceptedAt(null);
        }

        serviceRepairRepository.saveAll(staleRepairs);
        log.info("Released {} stale ACCEPTED repair(s) back to queue", staleRepairs.size());
        return staleRepairs.size();
    }

    public List<ServiceRepair> getAcceptedRepairsForMechanic(UUID mechanicId) {
        User mechanic = userService.getById(mechanicId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic,
                List.of(RepairStatus.ACCEPTED, RepairStatus.IN_PROGRESS)
        );

        return repairs.stream()
                .sorted(Comparator
                        .comparingInt((ServiceRepair repair) -> statusPriority(repair.getStatus()))
                        .thenComparing(this::mechanicActiveRepairDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<ServiceRepair> getInProgressRepairsForMechanic(UUID mechanicId) {
        User mechanic = userService.getById(mechanicId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic,
                List.of(RepairStatus.IN_PROGRESS)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(this::mechanicActiveRepairDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<ServiceRepair> getWaitingAcceptedRepairsForMechanic(UUID mechanicId) {
        User mechanic = userService.getById(mechanicId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic,
                List.of(RepairStatus.ACCEPTED)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(this::mechanicActiveRepairDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<ServiceRepair> getRejectedRepairsForMechanic(UUID mechanicId) {
        User mechanic = userService.getById(mechanicId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic,
                List.of(RepairStatus.CANCELLED)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(this::mechanicRejectedDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<ServiceRepair> getCompletedRepairsForMechanic(UUID mechanicId) {
        User mechanic = userService.getById(mechanicId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByMechanicAndStatusInOrderByCreatedOnDesc(
                mechanic,
                List.of(RepairStatus.COMPLETED)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(
                        ServiceRepair::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public ServiceRepair getInProgressRepairForMechanic(UUID mechanicId, UUID repairId) {
        ServiceRepair repair = getRepairAssignedToMechanic(mechanicId, repairId);
        if (repair.getStatus() != RepairStatus.IN_PROGRESS) {
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_NOT_IN_PROGRESS);
        }
        return repair;
    }

    public List<PartResponse> getCatalogParts() {
        return partsClient.getAllParts();
    }

    public CompleteRepairRequest buildCompleteRepairForm() {
        List<PartResponse> catalogParts = getCatalogParts();

        CompleteRepairRequest request = new CompleteRepairRequest();
        request.setLaborCost(BigDecimal.ZERO);
        request.setParts(catalogParts.stream().map(part -> {
            CompleteRepairRequest.PartUsageForm form = new CompleteRepairRequest.PartUsageForm();
            form.setPartId(part.getId());
            form.setQuantity(1);
            form.setSelected(false);
            return form;
        }).toList());

        return request;
    }


    public List<ServiceRepair> getMyRepairs(UUID clientId) {
        User client = userService.getById(clientId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByClientAndStatusIn(client,
                List.of(RepairStatus.PENDING,
                        RepairStatus.ACCEPTED,
                        RepairStatus.IN_PROGRESS));

        return repairs.stream()
                .sorted(Comparator
                        .comparingInt((ServiceRepair repair) -> statusPriority(repair.getStatus()))
                        .thenComparing(ServiceRepair::getCreatedOn, Comparator.reverseOrder()))
                .toList();
    }

    public List<ServiceRepair> getCompletedRepairsForClient(UUID clientId) {
        User client = userService.getById(clientId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByClientAndStatusIn(
                client,
                List.of(RepairStatus.COMPLETED)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(
                        ServiceRepair::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<ServiceRepair> getRejectedRepairsForClient(UUID clientId) {
        User client = userService.getById(clientId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByClientAndStatusIn(
                client,
                List.of(RepairStatus.CANCELLED, RepairStatus.USER_CANCELLED)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(this::clientRejectedDate, Comparator.reverseOrder()))
                .toList();
    }

    private LocalDateTime clientRejectedDate(ServiceRepair repair) {
        if (repair.getRejectedAt() != null) {
            return repair.getRejectedAt();
        }
        return repair.getCreatedOn();
    }

    private LocalDateTime mechanicActiveRepairDate(ServiceRepair repair) {
        if (repair.getStatus() == RepairStatus.IN_PROGRESS && repair.getStartedAt() != null) {
            return repair.getStartedAt();
        }
        if (repair.getAcceptedAt() != null) {
            return repair.getAcceptedAt();
        }
        return repair.getCreatedOn();
    }

    private LocalDateTime mechanicRejectedDate(ServiceRepair repair) {
        if (repair.getRejectedAt() != null) {
            return repair.getRejectedAt();
        }
        return repair.getCreatedOn();
    }

    private int statusPriority(RepairStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 0;
            case ACCEPTED -> 1;
            case PENDING -> 2;
            default -> 3;
        };
    }

    @Transactional(readOnly = true)
    public ServiceRepair getRepairForClient(UUID userId, UUID id) {
        User client = userService.getById(userId);
        return serviceRepairRepository.findByIdAndClient(id, client)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ServiceRepair getRepairForMechanic(UUID mechanicId, UUID repairId) {
        ServiceRepair repair = getRepairAssignedToMechanic(mechanicId, repairId);

        if (repair.getStatus() != RepairStatus.COMPLETED
                && repair.getStatus() != RepairStatus.CANCELLED) {
            throw new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND);
        }

        return repair;
    }

    public List<ServiceRepair> getPendingRepairsForMechanics() {
        return serviceRepairRepository
                .findAllByStatusAndMechanicIsNullOrderByCreatedOnDesc(RepairStatus.PENDING);
    }

    public List<ServiceRepair> getPendingRepairsForAdmin() {
        return serviceRepairRepository.findAllByStatusOrderByCreatedOnDesc(RepairStatus.PENDING);
    }
    public List<ServiceRepair> getAcceptedRepairsForAdmin() {
        List<ServiceRepair> repairs = serviceRepairRepository
                .findAllByStatusOrderByCreatedOnDesc(RepairStatus.ACCEPTED);
        return repairs.stream()
                .sorted(Comparator.comparing(this::mechanicActiveRepairDate, Comparator.reverseOrder()))
                .toList();
    }
    public List<ServiceRepair> getInProgressRepairsForAdmin() {
        List<ServiceRepair> repairs = serviceRepairRepository
                .findAllByStatusOrderByCreatedOnDesc(RepairStatus.IN_PROGRESS);
        return repairs.stream()
                .sorted(Comparator.comparing(this::mechanicActiveRepairDate, Comparator.reverseOrder()))
                .toList();
    }
    public List<ServiceRepair> getCompletedRepairsForAdmin() {
        List<ServiceRepair> repairs = serviceRepairRepository
                .findAllByStatusOrderByCreatedOnDesc(RepairStatus.COMPLETED);
        return repairs.stream()
                .sorted(Comparator.comparing(
                        ServiceRepair::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
    public List<ServiceRepair> getRejectedRepairsForAdmin() {
        List<ServiceRepair> repairs = serviceRepairRepository
                .findAllByStatusInOrderByCreatedOnDesc(
                        List.of(RepairStatus.CANCELLED, RepairStatus.USER_CANCELLED));
        return repairs.stream()
                .sorted(Comparator.comparing(this::clientRejectedDate, Comparator.reverseOrder()))
                .toList();
    }
    @Transactional(readOnly = true)
    public ServiceRepair getRepairForAdmin(UUID repairId) {
        return serviceRepairRepository.findById(repairId)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));
    }
}
