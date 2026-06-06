package com.pitstop.garage.web;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.web.dto.RequestRepairRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/repairs")
public class RepairController {

    private final CarService carService;
    private final RepairService repairService;

    public RepairController(CarService carService, RepairService repairService) {
        this.carService = carService;
        this.repairService = repairService;
    }

    @GetMapping
    public ModelAndView repairsPreview(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        UUID userId = (UUID) session.getAttribute("userId");

        ModelAndView modelAndView = new ModelAndView("repairs");
        modelAndView.addObject("repairs", repairService.getMyRepairs(userId));
        return modelAndView;
    }

    @GetMapping("/request")
    public ModelAndView repairRequestForm(@RequestParam(required = false)
                                              UUID carId,
                                              HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView modelAndView = new ModelAndView("repair-request");
        modelAndView.addObject("requestRepairRequest", new RequestRepairRequest());

        if (carId != null) {
            UUID userId = (UUID) session.getAttribute("userId");
            Car car = carService.getMyCar(userId, carId);
            modelAndView.addObject("car", car);
        }

        return modelAndView;
    }

    @PostMapping("/request")
    public ModelAndView submitRepairRequest(@RequestParam UUID carId,
                                            @Valid @ModelAttribute("requestRepairRequest")
                                            RequestRepairRequest requestRepairRequest,
                                            BindingResult bindingResult,
                                            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        UUID userId = (UUID) session.getAttribute("userId");

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("repair-request", bindingResult.getModel());
            modelAndView.addObject("car", carService.getMyCar(userId, carId));
            return modelAndView;
        }

        repairService.requestRepair(userId, carId, requestRepairRequest);

        return new ModelAndView("redirect:/repairs");
    }
}
