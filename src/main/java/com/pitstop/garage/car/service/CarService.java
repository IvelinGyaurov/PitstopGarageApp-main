package com.pitstop.garage.car.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.repository.CarRepository;
import com.pitstop.garage.exceptions.CarHasActiveRepairException;
import com.pitstop.garage.exceptions.CarHasActiveRepairExceptionMessage;
import com.pitstop.garage.exceptions.CarNotFoundException;
import com.pitstop.garage.exceptions.CarNotFoundExceptionMessage;
import com.pitstop.garage.exceptions.VinAlreadyExistsException;
import com.pitstop.garage.exceptions.VinAlreadyExistsExceptionMessage;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.AddCarRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CarService {

    private static final List<RepairStatus> ACTIVE_REPAIR_STATUSES = List.of(
            RepairStatus.PENDING,
            RepairStatus.ACCEPTED,
            RepairStatus.IN_PROGRESS
    );

    private final CarRepository carRepository;
    private final UserService userService;
    private final ServiceRepairRepository serviceRepairRepository;

    @Autowired
    public CarService(CarRepository carRepository,
                      UserService userService,
                      ServiceRepairRepository serviceRepairRepository) {
        this.carRepository = carRepository;
        this.userService = userService;
        this.serviceRepairRepository = serviceRepairRepository;
    }

    public void addCar(UUID id, AddCarRequest carRequest) {

        User owner = userService.getById(id);

        if (carRepository.existsByVinAndDeletedAtIsNull(carRequest.getVin())) {
            throw new VinAlreadyExistsException(
                    VinAlreadyExistsExceptionMessage.VIN_ALREADY_EXISTS_EXCEPTION_MESSAGE);
        }

        if (carRepository.existsByVin(carRequest.getVin())) {
            throw new VinAlreadyExistsException(
                    VinAlreadyExistsExceptionMessage.VIN_BELONGS_TO_DELETED_CAR_EXCEPTION_MESSAGE);
        }

        Car car = Car.builder()
                .vin(carRequest.getVin())
                .plateNumber(carRequest.getPlateNumber())
                .brand(carRequest.getBrand())
                .model(carRequest.getModel())
                .engineType(carRequest.getEngineType())
                .transmission(carRequest.getTransmission())
                .year(carRequest.getYear())
                .mileage(carRequest.getMileage())
                .owner(owner)
                .build();

        carRepository.save(car);
        log.info("Car added with VIN {} for user {}", carRequest.getVin(), id);
    }

    public List<Car> getMyCars(UUID ownerId) {
        User owner = userService.getById(ownerId);
        return carRepository.findAllByOwnerAndDeletedAtIsNullOrderByBrandAscModelAsc(owner);
    }

    public Car getMyCar(UUID ownerId, UUID carId) {
        User owner = userService.getById(ownerId);
        return carRepository.findByIdAndOwnerAndDeletedAtIsNull(carId, owner)
                .orElseThrow(() -> new CarNotFoundException(CarNotFoundExceptionMessage.CAR_NOT_FOUND));
    }


    public void deleteCar(UUID userId, UUID id) {

        User owner = userService.getById(userId);

        Car car = carRepository.findByIdAndOwnerAndDeletedAtIsNull(id, owner)
                .orElseThrow(() -> new CarNotFoundException(CarNotFoundExceptionMessage.CAR_NOT_FOUND));

        if (serviceRepairRepository.existsByCarAndStatusIn(car, ACTIVE_REPAIR_STATUSES)) {
            throw new CarHasActiveRepairException(CarHasActiveRepairExceptionMessage.CAR_HAS_ACTIVE_REPAIR);
        }

        car.setDeletedAt(LocalDateTime.now());
        carRepository.save(car);
        log.info("Car {} soft-deleted for user {}", id, userId);
    }
}
