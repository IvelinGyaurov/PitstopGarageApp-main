package com.pitstop.garage.repair.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.exceptions.RepairNotFoundException;
import com.pitstop.garage.exceptions.RepairNotFoundExceptionMessage;
import com.pitstop.garage.exceptions.RepairStatusException;
import com.pitstop.garage.exceptions.RepairStatusExceptionMessage;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.RequestRepairRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RepairService {

    private final ServiceRepairRepository serviceRepairRepository;
    private final UserService userService;
    private final CarService carService;

    @Autowired
    public RepairService(ServiceRepairRepository serviceRepairRepository, UserService userService, CarService carService) {
        this.serviceRepairRepository = serviceRepairRepository;
        this.userService = userService;
        this.carService = carService;
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
    }

    public void acceptRepairByMechanic(UUID mechanicId, UUID repairId) {
        User mechanic = userService.getById(mechanicId);
        ServiceRepair repair = getPendingUnassignedRepair(repairId);

        repair.setMechanic(mechanic);
        repair.setStatus(RepairStatus.ACCEPTED);
        repair.setAcceptedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
    }

    public void rejectRepairByMechanic(UUID mechanicId, UUID repairId) {
        User mechanic = userService.getById(mechanicId);
        ServiceRepair repair = getPendingUnassignedRepair(repairId);

        repair.setMechanic(mechanic);
        repair.setStatus(RepairStatus.CANCELLED);
        repair.setRejectedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
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
    }

    public void completeRepairByMechanic(UUID mechanicId, UUID repairId) {
        ServiceRepair repair = getRepairAssignedToMechanic(mechanicId, repairId);

        if (repair.getStatus() != RepairStatus.IN_PROGRESS) {
            throw new RepairStatusException(RepairStatusExceptionMessage.REPAIR_NOT_IN_PROGRESS);
        }

        repair.setStatus(RepairStatus.COMPLETED);
        repair.setCompletedAt(LocalDateTime.now());
        serviceRepairRepository.save(repair);
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

    public List<ServiceRepair> getMyRepairHistory(UUID clientId) {
        User client = userService.getById(clientId);
        List<ServiceRepair> repairs = serviceRepairRepository.findAllByClientAndStatusIn(
                client,
                List.of(RepairStatus.COMPLETED,
                        RepairStatus.USER_CANCELLED,
                        RepairStatus.CANCELLED)
        );

        return repairs.stream()
                .sorted(Comparator.comparing(this::historyDate, Comparator.reverseOrder()))
                .toList();
    }

    private LocalDateTime historyDate(ServiceRepair repair) {
        if (repair.getCompletedAt() != null) {
            return repair.getCompletedAt();
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

    public ServiceRepair getRepairForClient(UUID userId, UUID id) {
        User client = userService.getById(userId);
        return serviceRepairRepository.findByIdAndClient(id, client)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));
    }

    public List<ServiceRepair> getPendingRepairsForMechanics() {
        return serviceRepairRepository
                .findAllByStatusAndMechanicIsNullOrderByCreatedOnDesc(RepairStatus.PENDING);
    }
}
