package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.web.dto.AddCarRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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
    public ModelAndView myCars(HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        UUID userId = (UUID) session.getAttribute("userId");

        ModelAndView modelAndView = new ModelAndView("cars");
        modelAndView.addObject("cars", carService.getMyCars(userId));

        return modelAndView;
    }

    @GetMapping("/add")
    public ModelAndView addCarForm(HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView modelAndView = new ModelAndView("car-add");
        modelAndView.addObject("addCarRequest", new AddCarRequest());
        return modelAndView;
    }

    @PostMapping("/add")
    public ModelAndView addCar(@Valid @ModelAttribute("addCarRequest") AddCarRequest addCarRequest,
                               BindingResult bindingResult,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        if (bindingResult.hasErrors()) {
            return new ModelAndView("car-add", bindingResult.getModel());
        }

        UUID userId = (UUID) session.getAttribute("userId");
        carService.addCar(userId, addCarRequest);

        redirectAttributes.addFlashAttribute("successMessage", "Car added successfully.");
        return new ModelAndView("redirect:/cars");
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteCar(@PathVariable UUID id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        UUID userId = (UUID) session.getAttribute("userId");
        carService.deleteCar(userId, id);

        redirectAttributes.addFlashAttribute("successMessage", "Car removed.");
        return new ModelAndView("redirect:/cars");
    }

}
