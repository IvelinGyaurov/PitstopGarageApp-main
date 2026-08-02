package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
    void handleDataIntegrityViolation_redirectsToRegister() {
        assertEquals("redirect:/register",
                advice.handleDataIntegrityViolation(redirectAttributes));
        verify(redirectAttributes).addFlashAttribute("errorMessage", "error.usernameOrEmailExists");
    }

    @Test
    void handleCarNotFound_redirectsToCars() {
        assertEquals("redirect:/cars",
                advice.handleCarNotFound(redirectAttributes, new CarNotFoundException("missing")));
    }

    @Test
    void handlePartSkuAlreadyExists_redirectsToPartsAdd() {
        assertEquals("redirect:/admin/parts/add",
                advice.handlePartSkuAlreadyExists(redirectAttributes, new PartSkuAlreadyExistsException("sku")));
    }

    @Test
    void handleRepairNotFound_redirectsToRepairs() {
        assertEquals("redirect:/repairs",
                advice.handleRepairNotFound(redirectAttributes, new RepairNotFoundException("missing")));
    }

    @Test
    void handleRepairStatus_redirectsToRepairs() {
        assertEquals("redirect:/repairs",
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
    void handleNotFound_returnsError404() {
        ModelAndView mav = advice.handleNotFound(new RuntimeException("nf"));
        assertEquals("error", mav.getViewName());
        assertEquals(HttpStatus.NOT_FOUND, mav.getStatus());
        assertEquals(404, mav.getModel().get("status"));
    }

    @Test
    void handleAccessDenied_returnsErrorWith500StatusObject() {
        ModelAndView mav = advice.handleAccessDenied(new AccessDeniedException("denied"));
        assertEquals("error", mav.getViewName());
        assertEquals(HttpStatus.FORBIDDEN, mav.getStatus());
        assertEquals(500, mav.getModel().get("status"));
    }

    @Test
    void handleUnexpected_returnsError500() {
        ModelAndView mav = advice.handleUnexpected(new RuntimeException("boom"));
        assertEquals("error", mav.getViewName());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, mav.getStatus());
        assertEquals(500, mav.getModel().get("status"));
    }
}
