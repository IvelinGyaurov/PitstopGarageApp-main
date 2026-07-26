package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.web.dto.AddCarRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarControllerTest {

    @Mock
    private CarService carService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CarController controller;

    @Test
    void myCars_returnsCarsView() {
        UUID userId = UUID.randomUUID();
        PitstopUserDetails principal = new PitstopUserDetails(
                userId, "owner", "pass", UserRole.USER, true);
        when(carService.getMyCars(userId)).thenReturn(List.of());

        ModelAndView mav = controller.myCars(principal);

        assertEquals("cars", mav.getViewName());
        assertEquals(List.of(), mav.getModel().get("cars"));
    }

    @Test
    void deleteCar_removesAndRedirects() {
        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        PitstopUserDetails principal = new PitstopUserDetails(
                userId, "owner", "pass", UserRole.USER, true);

        ModelAndView mav = controller.deleteCar(carId, principal, redirectAttributes);

        verify(carService).deleteCar(userId, carId);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Car removed.");
        assertEquals("redirect:/cars", mav.getViewName());
    }

    @Test
    void addCar_whenValid_redirects() {
        UUID userId = UUID.randomUUID();
        PitstopUserDetails principal = new PitstopUserDetails(
                userId, "owner", "pass", UserRole.USER, true);
        AddCarRequest request = new AddCarRequest();
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = controller.addCar(request, bindingResult, principal, redirectAttributes);

        verify(carService).addCar(userId, request);
        assertEquals("redirect:/cars", mav.getViewName());
    }
}
