package com.pitstop.garage.api;

import com.pitstop.garage.car.repository.CarRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    private PitstopUserDetails principal;
    private User owner;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        userService.registerUser(RegisterRequest.builder()
                .username("carowner")
                .password("pass")
                .email("carowner@mail.com")
                .build());

        owner = userRepository.findByUsername("carowner").orElseThrow();
        principal = new PitstopUserDetails(
                owner.getId(),
                owner.getUsername(),
                owner.getPassword(),
                owner.getRole(),
                owner.isActive()
        );
    }

    @Test
    void getCars_whenAuthenticated_returnsCarsView() throws Exception {
        mockMvc.perform(get("/cars").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("cars"))
                .andExpect(model().attributeExists("cars"));
    }

    @Test
    void getCars_whenMechanic_isForbiddenAsNotFound() throws Exception {
        userService.registerUser(RegisterRequest.builder()
                .username("mech1")
                .password("pass")
                .email("mech1@mail.com")
                .build());
        User mechanic = userRepository.findByUsername("mech1").orElseThrow();
        userService.changeRole(mechanic.getId(), UserRole.MECHANIC);
        mechanic = userRepository.findByUsername("mech1").orElseThrow();

        PitstopUserDetails mechanicPrincipal = new PitstopUserDetails(
                mechanic.getId(),
                mechanic.getUsername(),
                mechanic.getPassword(),
                mechanic.getRole(),
                mechanic.isActive()
        );

        mockMvc.perform(get("/cars").with(user(mechanicPrincipal)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void getAddCarForm_returnsForm() throws Exception {
        mockMvc.perform(get("/cars/add").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("car-add"))
                .andExpect(model().attributeExists("addCarRequest"));
    }

    @Test
    void postAddCar_withValidData_redirectsToCars() throws Exception {
        mockMvc.perform(post("/cars/add")
                        .with(user(principal))
                        .with(csrf())
                        .param("vin", "WBA3A5C50EK123456")
                        .param("plateNumber", "CB1234AB")
                        .param("brand", "BMW")
                        .param("model", "320d")
                        .param("engineType", "DIESEL")
                        .param("transmission", "AUTOMATIC")
                        .param("year", "2019")
                        .param("mileage", "50000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"));

        assertEquals(1, carRepository.findAllByOwnerAndDeletedAtIsNull(owner).size());
        assertTrue(carRepository.existsByVinAndDeletedAtIsNull("WBA3A5C50EK123456"));
    }

    @Test
    void postAddCar_withInvalidVin_staysOnForm() throws Exception {
        mockMvc.perform(post("/cars/add")
                        .with(user(principal))
                        .with(csrf())
                        .param("vin", "SHORT")
                        .param("plateNumber", "CB1234AB")
                        .param("brand", "BMW")
                        .param("model", "320d")
                        .param("engineType", "DIESEL")
                        .param("transmission", "AUTOMATIC")
                        .param("year", "2019")
                        .param("mileage", "50000"))
                .andExpect(status().isOk())
                .andExpect(view().name("car-add"));
    }
}
