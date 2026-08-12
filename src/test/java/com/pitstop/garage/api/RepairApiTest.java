package com.pitstop.garage.api;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.repository.CarRepository;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.repository.UserRepository;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepairApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ServiceRepairRepository serviceRepairRepository;

    private PitstopUserDetails clientPrincipal;
    private User client;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        carRepository.deleteAll();
        serviceRepairRepository.deleteAll();

        userService.registerUser(RegisterRequest.builder()
                .username("admin")
                .password("pass")
                .email("admin@mail.com")
                .build());

        userService.registerUser(RegisterRequest.builder()
                .username("client1")
                .password("pass")
                .email("client1@mail.com")
                .build());

        client = userRepository.findByUsername("client1").orElseThrow();
        clientPrincipal = new PitstopUserDetails(
                client.getId(),
                client.getUsername(),
                client.getPassword(),
                client.getRole(),
                client.isActive()
        );
    }

    @Test
    void getRepairs_whenAuthenticated_returnsSeparatedRepairLists() throws Exception {
        User mechanic = userRepository.findByUsername("admin").orElseThrow();

        Car car = carRepository.save(Car.builder()
                .vin("WBA3A5C50EK123456")
                .plateNumber("CB1234AB")
                .brand("BMW")
                .model("320d")
                .engineType("DIESEL")
                .transmission("AUTOMATIC")
                .year(2019)
                .mileage(90000)
                .owner(client)
                .build());

        serviceRepairRepository.save(ServiceRepair.builder()
                .problemDescription("Pending repair request")
                .status(RepairStatus.PENDING)
                .client(client)
                .car(car)
                .createdOn(LocalDateTime.now().minusDays(1))
                .build());

        serviceRepairRepository.save(ServiceRepair.builder()
                .problemDescription("Accepted repair request")
                .status(RepairStatus.ACCEPTED)
                .client(client)
                .car(car)
                .mechanic(mechanic)
                .createdOn(LocalDateTime.now().minusDays(2))
                .acceptedAt(LocalDateTime.now().minusHours(5))
                .build());

        serviceRepairRepository.save(ServiceRepair.builder()
                .problemDescription("In progress repair request")
                .status(RepairStatus.IN_PROGRESS)
                .client(client)
                .car(car)
                .mechanic(mechanic)
                .createdOn(LocalDateTime.now().minusDays(3))
                .acceptedAt(LocalDateTime.now().minusDays(1))
                .startedAt(LocalDateTime.now().minusHours(2))
                .build());

        mockMvc.perform(get("/repairs").with(user(clientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs"))
                .andExpect(model().attributeExists("inProgressRepairs"))
                .andExpect(model().attributeExists("acceptedRepairs"))
                .andExpect(model().attributeExists("pendingRepairs"))
                .andExpect(model().attribute("inProgressRepairs", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("acceptedRepairs", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("pendingRepairs", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void getRepairs_whenMechanic_isForbiddenAsNotFound() throws Exception {
        userService.registerUser(RegisterRequest.builder()
                .username("mech1")
                .password("pass")
                .email("mech1@mail.com")
                .build());

        User mechanic = userRepository.findByUsername("mech1").orElseThrow();
        mechanic.setRole(UserRole.MECHANIC);
        userRepository.save(mechanic);

        PitstopUserDetails mechanicPrincipal = new PitstopUserDetails(
                mechanic.getId(),
                mechanic.getUsername(),
                mechanic.getPassword(),
                mechanic.getRole(),
                mechanic.isActive()
        );

        mockMvc.perform(get("/repairs").with(user(mechanicPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRepairsHistory_whenAuthenticated_returnsHistoryView() throws Exception {
        mockMvc.perform(get("/repairs/history").with(user(clientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs-history"))
                .andExpect(model().attributeExists("completedRepairs"))
                .andExpect(model().attributeExists("rejectedRepairs"))
                .andExpect(model().attributeExists("expiredRepairs"));
    }

    @Test
    void getRepairDetails_whenAuthenticated_returnsDetailsView() throws Exception {
        Car car = carRepository.save(Car.builder()
                .vin("WBA3A5C50EK123457")
                .plateNumber("CB5678CD")
                .brand("Audi")
                .model("A4")
                .engineType("PETROL")
                .transmission("MANUAL")
                .year(2020)
                .mileage(50000)
                .owner(client)
                .build());

        ServiceRepair repair = serviceRepairRepository.save(ServiceRepair.builder()
                .problemDescription("Brake pads need replacement soon")
                .status(RepairStatus.PENDING)
                .client(client)
                .car(car)
                .createdOn(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/repairs/{id}", repair.getId()).with(user(clientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("repair-details"))
                .andExpect(model().attributeExists("repair"))
                .andExpect(model().attribute("detailsAudience", "CLIENT"));
    }

    @Test
    void cancelPendingRepair_whenAuthenticated_redirectsToRepairs() throws Exception {
        Car car = carRepository.save(Car.builder()
                .vin("WBA3A5C50EK123458")
                .plateNumber("CB9012EF")
                .brand("VW")
                .model("Golf")
                .engineType("PETROL")
                .transmission("MANUAL")
                .year(2018)
                .mileage(110000)
                .owner(client)
                .build());

        ServiceRepair repair = serviceRepairRepository.save(ServiceRepair.builder()
                .problemDescription("Check engine light is on again")
                .status(RepairStatus.PENDING)
                .client(client)
                .car(car)
                .createdOn(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/repairs/{id}/cancel", repair.getId())
                        .with(user(clientPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/repairs"));

        ServiceRepair updated = serviceRepairRepository.findById(repair.getId()).orElseThrow();
        assertEquals(RepairStatus.USER_CANCELLED, updated.getStatus());
    }
}
