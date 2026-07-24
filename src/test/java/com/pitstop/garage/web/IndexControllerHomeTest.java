package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexControllerHomeTest {

    @Mock
    private UserService userService;

    @Mock
    private CarService carService;

    @Mock
    private RepairService repairService;

    @InjectMocks
    private IndexController indexController;

    @Test
    void getHomePage_forMechanic_addsBayAndQueueCounts() {
        UUID id = UUID.randomUUID();
        User user = user(id, UserRole.MECHANIC);
        when(userService.getById(id)).thenReturn(user);
        when(repairService.getAcceptedRepairsForMechanic(id)).thenReturn(List.of());
        when(repairService.getPendingRepairsForMechanics()).thenReturn(List.of());

        ModelAndView mav = indexController.getHomePage(
                new PitstopUserDetails(id, "mech", "pass", UserRole.MECHANIC, true));

        assertEquals("home", mav.getViewName());
        assertEquals(0, mav.getModel().get("bayCount"));
        assertEquals(0, mav.getModel().get("queueCount"));
    }

    @Test
    void getHomePage_forAdmin_addsActiveRepairsAndUsersCount() {
        UUID id = UUID.randomUUID();
        User user = user(id, UserRole.ADMIN);
        when(userService.getById(id)).thenReturn(user);
        when(repairService.getPendingRepairsForAdmin()).thenReturn(List.of());
        when(repairService.getAcceptedRepairsForAdmin()).thenReturn(List.of());
        when(repairService.getInProgressRepairsForAdmin()).thenReturn(List.of());
        when(userService.countUsers()).thenReturn(5L);

        ModelAndView mav = indexController.getHomePage(
                new PitstopUserDetails(id, "admin", "pass", UserRole.ADMIN, true));

        assertEquals("home", mav.getViewName());
        assertEquals(0, mav.getModel().get("activeRepairsCount"));
        assertEquals(5L, mav.getModel().get("usersCount"));
    }

    @Test
    void getLoginPage_withInactiveFlag_setsMessage() {
        ModelAndView mav = indexController.getLoginPage(null, "1");
        assertEquals("login", mav.getViewName());
        assertEquals("Your account is inactive.", mav.getModel().get("errorMessage"));
    }

    private User user(UUID id, UserRole role) {
        return User.builder()
                .id(id)
                .username("u")
                .email("u@mail.com")
                .password("p")
                .role(role)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }
}
