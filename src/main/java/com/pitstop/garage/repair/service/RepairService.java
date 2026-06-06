package com.pitstop.garage.repair.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.repair.repository.UsedPartRepository;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.RequestRepairRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    public List<ServiceRepair> getMyRepairs(UUID clientId) {
        User client = userService.getById(clientId);
        return serviceRepairRepository.findAllByClientOrderByCreatedOnDesc(client);
    }
}
