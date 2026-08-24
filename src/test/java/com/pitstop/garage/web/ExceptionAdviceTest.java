package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.exceptions.*;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionAdviceTest {

    @Mock
    private MessageHelper messages;

    @Mock
    private RedirectAttributes redirectAttributes;

    private ExceptionAdvice advice;

    @BeforeEach
    void setUp() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        advice = new ExceptionAdvice(messages);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleUserAlreadyExist_redirectsToRegister() {
        assertEquals("redirect:/register",
                advice.handleUserAlreadyExist(redirectAttributes, new UserAlreadyExistException("dup")));
        verify(redirectAttributes).addFlashAttribute("errorMessage", "dup");
    }

    @Test
    void handleVinAlreadyExists_redirectsToCarAdd() {
        assertEquals("redirect:/cars/add",
                advice.handleVinAlreadyExists(redirectAttributes, new VinAlreadyExistsException("vin")));
    }

    @Test
    void handleDataIntegrityViolation_redirectsToHome() {
        assertEquals("redirect:/home",
                advice.handleDataIntegrityViolation(redirectAttributes));
        verify(redirectAttributes).addFlashAttribute("errorMessage", "error.dataIntegrity");
    }

    @Test
    void handleCarNotFound_redirectsToCars() {
        assertEquals("redirect:/cars",
                advice.handleCarNotFound(redirectAttributes, new CarNotFoundException("missing")));
    }

    @Test
    void handleCarHasActiveRepair_redirectsToCars() {
        assertEquals("redirect:/cars",
                advice.handleCarHasActiveRepair(
                        redirectAttributes, new CarHasActiveRepairException("error.carHasActiveRepair")));
        verify(redirectAttributes).addFlashAttribute("errorMessage", "error.carHasActiveRepair");
    }

    @Test
    void handlePartSkuAlreadyExists_redirectsToPartsAdd() {
        assertEquals("redirect:/admin/parts/add",
                advice.handlePartSkuAlreadyExists(redirectAttributes, new PartSkuAlreadyExistsException("sku")));
    }

    @Test
    void handleInsufficientPartStock_redirectsToCompleteForm() {
        UUID repairId = UUID.randomUUID();
        assertEquals("redirect:/mechanic/repairs/" + repairId + "/complete",
                advice.handleInsufficientPartStock(
                        redirectAttributes,
                        new InsufficientPartStockException("flash.repair.insufficientStock", repairId)));
        verify(redirectAttributes).addFlashAttribute("errorMessage", "flash.repair.insufficientStock");
    }

    @Test
    void handleRepairNotFound_whenClient_redirectsToRepairs() {
        authenticateAs(UserRole.USER);
        assertEquals("redirect:/repairs",
                advice.handleRepairNotFound(redirectAttributes, new RepairNotFoundException("missing")));
    }

    @Test
    void handleRepairNotFound_whenMechanic_redirectsToMechanicQueue() {
        authenticateAs(UserRole.MECHANIC);
        assertEquals("redirect:/mechanic/repairs",
                advice.handleRepairNotFound(redirectAttributes, new RepairNotFoundException("missing")));
    }

    @Test
    void handleRepairNotFound_whenAdmin_redirectsToAdminRepairs() {
        authenticateAs(UserRole.ADMIN);
        assertEquals("redirect:/admin/repairs",
                advice.handleRepairNotFound(redirectAttributes, new RepairNotFoundException("missing")));
    }

    @Test
    void handleRepairStatus_whenClient_redirectsToRepairs() {
        authenticateAs(UserRole.USER);
        assertEquals("redirect:/repairs",
                advice.handleRepairStatus(redirectAttributes, new RepairStatusException("bad")));
    }

    @Test
    void handleRepairStatus_whenMechanic_redirectsToMechanicQueue() {
        authenticateAs(UserRole.MECHANIC);
        assertEquals("redirect:/mechanic/repairs",
                advice.handleRepairStatus(redirectAttributes, new RepairStatusException("bad")));
    }

    @Test
    void handleRepairStatus_whenAdmin_redirectsToAdminRepairs() {
        authenticateAs(UserRole.ADMIN);
        assertEquals("redirect:/admin/repairs",
                advice.handleRepairStatus(redirectAttributes, new RepairStatusException("bad")));
    }

    @Test
    void handleUserInactive_redirectsToUsers() {
        assertEquals("redirect:/users",
                advice.handleUserInactive(redirectAttributes, new UserInactiveException("inactive")));
    }

    @Test
    void handleUserNotFound_redirectsToHome() {
        assertEquals("redirect:/home",
                advice.handleUserNotFound(redirectAttributes, new UserNotFoundException("missing")));
    }

    @Test
    void handlePrimaryUser_redirectsToUsers() {
        assertEquals("redirect:/users",
                advice.handlePrimaryUser(redirectAttributes, new PrimaryUserException("last admin")));
    }

    @Test
    void handleRepairNotFound_whenNoAuthentication_redirectsToRepairs() {
        SecurityContextHolder.clearContext();
        assertEquals("redirect:/repairs",
                advice.handleRepairNotFound(redirectAttributes, new RepairNotFoundException("missing")));
    }

    @Test
    void handleRepairNotFound_whenAuthoritiesNull_redirectsToRepairs() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("redirect:/repairs",
                advice.handleRepairNotFound(redirectAttributes, new RepairNotFoundException("missing")));
    }

    @Test
    void handleNotFound_returnsError404() {
        ModelAndView mav = advice.handleNotFound(new RuntimeException("nf"));
        assertEquals("error", mav.getViewName());
        assertEquals(HttpStatus.NOT_FOUND, mav.getStatus());
        assertEquals(404, mav.getModel().get("status"));
    }

    @Test
    void handleAccessDenied_returnsErrorWith404Status() {
        ModelAndView mav = advice.handleAccessDenied(new AccessDeniedException("denied"));
        assertEquals("error", mav.getViewName());
        assertEquals(HttpStatus.NOT_FOUND, mav.getStatus());
        assertEquals(404, mav.getModel().get("status"));
    }

    @Test
    void handleUnexpected_returnsError500() {
        ModelAndView mav = advice.handleUnexpected(new RuntimeException("boom"));
        assertEquals("error", mav.getViewName());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, mav.getStatus());
        assertEquals(500, mav.getModel().get("status"));
    }

    private void authenticateAs(UserRole role) {
        PitstopUserDetails principal = new PitstopUserDetails(
                UUID.randomUUID(), "user", "pass", role, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
