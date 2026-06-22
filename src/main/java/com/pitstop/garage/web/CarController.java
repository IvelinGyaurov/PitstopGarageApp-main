package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.web.dto.AddCarRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public ModelAndView myCars(@AuthenticationPrincipal PitstopUserDetails userData) {
        ModelAndView modelAndView = new ModelAndView("cars");
        modelAndView.addObject("cars", carService.getMyCars(userData.getUserId()));
        return modelAndView;
    }

    @GetMapping("/add")
    public ModelAndView addCarForm() {

        ModelAndView modelAndView = new ModelAndView("car-add");
        modelAndView.addObject("addCarRequest", new AddCarRequest());
        return modelAndView;
    }

    @PostMapping("/add")
    public ModelAndView addCar(@Valid @ModelAttribute("addCarRequest") AddCarRequest addCarRequest,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal PitstopUserDetails userData,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("car-add", bindingResult.getModel());
        }
        carService.addCar(userData.getUserId(), addCarRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Car added successfully.");
        return new ModelAndView("redirect:/cars");
    }
    @DeleteMapping("/{id}")
    public ModelAndView deleteCar(@PathVariable UUID id,
                                  @AuthenticationPrincipal PitstopUserDetails userData,
                                  RedirectAttributes redirectAttributes) {
        carService.deleteCar(userData.getUserId(), id);
        redirectAttributes.addFlashAttribute("successMessage", "Car removed.");
        return new ModelAndView("redirect:/cars");
    }

}
