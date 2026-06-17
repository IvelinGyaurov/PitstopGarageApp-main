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

    public void acceptRepair(UUID userId, UUID id) {

        User client = userService.getById(userId);
        ServiceRepair repair = serviceRepairRepository.findByIdAndClient(id, client)
                .orElseThrow(() -> new RepairNotFoundException(RepairNotFoundExceptionMessage.REPAIR_NOT_FOUND));
        if (repair.getStatus() != RepairStatus.PENDING) {
            throw new RepairStatusException("Only pending repairs can be accepted.");
        }
        repair.setStatus(RepairStatus.ACCEPTED);
        repair.setAcceptedAt(LocalDateTime.now());
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
}
