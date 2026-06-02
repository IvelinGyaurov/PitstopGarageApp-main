package com.pitstop.garage.car.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.repository.CarRepository;
import com.pitstop.garage.exceptions.VinAlreadyExistsException;
import com.pitstop.garage.exceptions.VinAlreadyExistsExceptionMessage;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.repository.UserRepository;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.AddCarRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final UserService userService;

    @Autowired
    public CarService(CarRepository carRepository, UserService userService) {
        this.carRepository = carRepository;
        this.userService = userService;
    }

    public void addCar(UUID id, AddCarRequest carRequest) {

        User owner = userService.getById(id);

        if (carRepository.existsByVinAndDeletedAtIsNull(carRequest.getVin())) {
            throw new VinAlreadyExistsException(VinAlreadyExistsExceptionMessage.VIN_ALREADY_EXISTS_EXCEPTION_MESSAGE);
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
    }

    public List<Car> getMyCars(UUID ownerId) {
        User owner = userService.getById(ownerId);
        return carRepository.findAllByOwnerAndDeletedAtIsNull(owner);
    }


}
