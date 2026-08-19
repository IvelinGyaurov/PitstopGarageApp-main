package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.car.vin.VinDecodeOutcome;
import com.pitstop.garage.car.vin.VinDecodeService;
import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.web.dto.AddCarRequest;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarControllerTest {

    @Mock
    private CarService carService;

    @Mock
    private VinDecodeService vinDecodeService;

    @Mock
    private MessageHelper messages;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CarController controller;

    @BeforeEach
    void stubMessages() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

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
        verify(redirectAttributes).addFlashAttribute("successMessage", "flash.car.removed");
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

    @Test
    void decodeVin_delegatesToServiceAndRedirects() {
        AddCarRequest request = new AddCarRequest();
        request.setVin("1HGCM82633A004352");
        when(vinDecodeService.applyToAddCarRequest(request)).thenReturn(VinDecodeOutcome.success());

        ModelAndView mav = controller.decodeVin(request, redirectAttributes);

        verify(vinDecodeService).applyToAddCarRequest(request);
        verify(redirectAttributes).addFlashAttribute("successMessage", "flash.car.vinDecoded");
        verify(redirectAttributes).addFlashAttribute("addCarRequest", request);
        assertEquals("redirect:/cars/add", mav.getViewName());
    }
}
