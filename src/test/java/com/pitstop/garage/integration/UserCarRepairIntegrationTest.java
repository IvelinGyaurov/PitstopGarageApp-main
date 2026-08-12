package com.pitstop.garage.integration;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.repository.CarRepository;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.exceptions.CarHasActiveRepairException;
import com.pitstop.garage.exceptions.VinAlreadyExistsException;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.repository.UserRepository;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.AddCarRequest;
import com.pitstop.garage.web.dto.RegisterRequest;
import com.pitstop.garage.web.dto.RequestRepairRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserCarRepairIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CarService carService;

    @Autowired
    private RepairService repairService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ServiceRepairRepository serviceRepairRepository;

    @Test
    void registerAddCarAndRequestRepair_persistsAcrossServices() {
        userService.registerUser(RegisterRequest.builder()
                .username("client1")
                .password("pass")
                .email("client1@mail.com")
                .build());

        User client = userRepository.findByUsername("client1").orElseThrow();
        assertEquals(UserRole.ADMIN, client.getRole());

        userService.registerUser(RegisterRequest.builder()
                .username("client2")
                .password("pass")
                .email("client2@mail.com")
                .build());

        User second = userRepository.findByUsername("client2").orElseThrow();
        assertEquals(UserRole.USER, second.getRole());

        AddCarRequest carRequest = AddCarRequest.builder()
                .vin("WBA3A5C50EK123456")
                .plateNumber("CB1234AB")
                .brand("BMW")
                .model("320d")
                .engineType("DIESEL")
                .transmission("AUTOMATIC")
                .year(2019)
                .mileage(90000)
                .build();

        carService.addCar(second.getId(), carRequest);

        List<Car> cars = carService.getMyCars(second.getId());
        assertEquals(1, cars.size());
        assertEquals("BMW", cars.get(0).getBrand());
        assertTrue(carRepository.existsByVinAndDeletedAtIsNull("WBA3A5C50EK123456"));

        assertThrows(VinAlreadyExistsException.class,
                () -> carService.addCar(second.getId(), carRequest));

        Car saved = cars.get(0);
        repairService.requestRepair(second.getId(), saved.getId(),
                RequestRepairRequest.builder()
                        .problemDescription("Strange knocking noise from engine")
                        .build());

        List<ServiceRepair> repairs = repairService.getMyRepairs(second.getId());
        assertEquals(1, repairs.size());
        assertEquals(RepairStatus.PENDING, repairs.get(0).getStatus());
        assertEquals(1, repairService.getPendingRepairsForClient(second.getId()).size());
        assertTrue(repairService.getWaitingAcceptedRepairsForClient(second.getId()).isEmpty());
        assertTrue(repairService.getInProgressRepairsForClient(second.getId()).isEmpty());
        assertEquals(1, serviceRepairRepository.count());

        CarHasActiveRepairException activeRepairEx = assertThrows(CarHasActiveRepairException.class,
                () -> carService.deleteCar(second.getId(), saved.getId()));
        assertEquals("error.carHasActiveRepair", activeRepairEx.getMessage());
        assertTrue(carRepository.existsByVinAndDeletedAtIsNull("WBA3A5C50EK123456"));

        UUID repairId = repairs.get(0).getId();
        repairService.cancelRepairByClient(second.getId(), repairId);

        carService.deleteCar(second.getId(), saved.getId());
        assertFalse(carRepository.existsByVinAndDeletedAtIsNull("WBA3A5C50EK123456"));
        assertTrue(carRepository.existsByVin("WBA3A5C50EK123456"));

        VinAlreadyExistsException deletedVinEx = assertThrows(VinAlreadyExistsException.class,
                () -> carService.addCar(second.getId(), carRequest));
        assertEquals("error.vinBelongsToDeletedCar", deletedVinEx.getMessage());
    }
}
