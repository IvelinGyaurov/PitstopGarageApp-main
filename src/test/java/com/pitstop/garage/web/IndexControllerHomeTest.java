package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexControllerHomeTest {

    @Mock
    private UserService userService;

    @Mock
    private CarService carService;

    @Mock
    private RepairService repairService;

    @Mock
    private MessageHelper messages;

    @InjectMocks
    private IndexController indexController;

    @BeforeEach
    void stubMessages() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

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
        assertEquals("flash.login.inactive", mav.getModel().get("errorMessage"));
    }

    @Test
    void getLoginPage_withErrorFlag_setsIncorrectCredentialsMessage() {
        ModelAndView mav = indexController.getLoginPage("true", null);
        assertEquals("login", mav.getViewName());
        assertEquals("flash.login.badCredentials", mav.getModel().get("errorMessage"));
    }

    @Test
    void getHomePage_forUser_addsCarsAndActiveRepairsCounts() {
        UUID id = UUID.randomUUID();
        User user = user(id, UserRole.USER);
        when(userService.getById(id)).thenReturn(user);
        when(carService.getMyCars(id)).thenReturn(List.of());
        when(repairService.getMyRepairs(id)).thenReturn(List.of());

        ModelAndView mav = indexController.getHomePage(
                new PitstopUserDetails(id, "client", "pass", UserRole.USER, true));

        assertEquals("home", mav.getViewName());
        assertEquals(0, mav.getModel().get("carsCount"));
        assertEquals(0, mav.getModel().get("activeRepairsCount"));
    }

    @Test
    void getHomePage_whenRoleIsNull_skipsRoleSpecificCounts() {
        UUID id = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn("unknown");
        when(user.getRole()).thenReturn(null);
        when(user.getProfilePicture()).thenReturn(null);
        when(userService.getById(id)).thenReturn(user);

        ModelAndView mav = indexController.getHomePage(
                new PitstopUserDetails(id, "unknown", "pass", UserRole.USER, true));

        assertEquals("home", mav.getViewName());
        assertFalse(mav.getModel().containsKey("carsCount"));
        assertFalse(mav.getModel().containsKey("bayCount"));
        assertFalse(mav.getModel().containsKey("usersCount"));
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
