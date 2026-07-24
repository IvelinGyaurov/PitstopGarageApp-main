package com.pitstop.garage.car.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.repository.CarRepository;
import com.pitstop.garage.exceptions.CarNotFoundException;
import com.pitstop.garage.exceptions.VinAlreadyExistsException;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.AddCarRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CarService carService;

    @Test
    void addCar_whenVinIsFree_savesCar() {
        UUID ownerId = UUID.randomUUID();
        User owner = owner(ownerId);
        AddCarRequest request = sampleRequest();

        when(userService.getById(ownerId)).thenReturn(owner);
        when(carRepository.existsByVinAndDeletedAtIsNull(request.getVin())).thenReturn(false);
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        carService.addCar(ownerId, request);

        ArgumentCaptor<Car> captor = ArgumentCaptor.forClass(Car.class);
        verify(carRepository).save(captor.capture());
        Car saved = captor.getValue();
        assertEquals(request.getVin(), saved.getVin());
        assertEquals(request.getPlateNumber(), saved.getPlateNumber());
        assertEquals(request.getBrand(), saved.getBrand());
        assertEquals(request.getModel(), saved.getModel());
        assertEquals(request.getEngineType(), saved.getEngineType());
        assertEquals(request.getTransmission(), saved.getTransmission());
        assertEquals(request.getYear(), saved.getYear());
        assertEquals(request.getMileage(), saved.getMileage());
        assertEquals(owner, saved.getOwner());
    }

    @Test
    void addCar_whenVinExists_throws() {
        UUID ownerId = UUID.randomUUID();
        AddCarRequest request = sampleRequest();

        when(userService.getById(ownerId)).thenReturn(owner(ownerId));
        when(carRepository.existsByVinAndDeletedAtIsNull(request.getVin())).thenReturn(true);

        assertThrows(VinAlreadyExistsException.class,
                () -> carService.addCar(ownerId, request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void getMyCars_returnsOwnerCars() {
        UUID ownerId = UUID.randomUUID();
        User owner = owner(ownerId);
        List<Car> cars = List.of(Car.builder().vin("VIN12345678901234").owner(owner).build());

        when(userService.getById(ownerId)).thenReturn(owner);
        when(carRepository.findAllByOwnerAndDeletedAtIsNull(owner)).thenReturn(cars);

        List<Car> result = carService.getMyCars(ownerId);

        assertEquals(1, result.size());
        assertEquals("VIN12345678901234", result.get(0).getVin());
    }

    @Test
    void getMyCar_whenFound_returnsCar() {
        UUID ownerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = owner(ownerId);
        Car car = Car.builder().id(carId).vin("VIN12345678901234").owner(owner).build();

        when(userService.getById(ownerId)).thenReturn(owner);
        when(carRepository.findByIdAndOwnerAndDeletedAtIsNull(carId, owner))
                .thenReturn(Optional.of(car));

        Car result = carService.getMyCar(ownerId, carId);

        assertEquals(carId, result.getId());
    }

    @Test
    void getMyCar_whenMissing_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = owner(ownerId);

        when(userService.getById(ownerId)).thenReturn(owner);
        when(carRepository.findByIdAndOwnerAndDeletedAtIsNull(carId, owner))
                .thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class,
                () -> carService.getMyCar(ownerId, carId));
    }

    @Test
    void deleteCar_whenFound_setsDeletedAt() {
        UUID ownerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = owner(ownerId);
        Car car = Car.builder().id(carId).vin("VIN12345678901234").owner(owner).build();

        when(userService.getById(ownerId)).thenReturn(owner);
        when(carRepository.findByIdAndOwnerAndDeletedAtIsNull(carId, owner))
                .thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        carService.deleteCar(ownerId, carId);

        assertNotNull(car.getDeletedAt());
        verify(carRepository).save(car);
    }

    @Test
    void deleteCar_whenMissing_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = owner(ownerId);

        when(userService.getById(ownerId)).thenReturn(owner);
        when(carRepository.findByIdAndOwnerAndDeletedAtIsNull(carId, owner))
                .thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class,
                () -> carService.deleteCar(ownerId, carId));
        verify(carRepository, never()).save(any());
    }

    private User owner(UUID id) {
        return User.builder()
                .id(id)
                .username("owner")
                .email("owner@mail.com")
                .password("encoded")
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    private AddCarRequest sampleRequest() {
        return AddCarRequest.builder()
                .vin("WBA3A5C50EK123456")
                .plateNumber("CB1234AB")
                .brand("BMW")
                .model("320d")
                .engineType("DIESEL")
                .transmission("AUTOMATIC")
                .year(2019)
                .mileage(120000)
                .build();
    }
}
