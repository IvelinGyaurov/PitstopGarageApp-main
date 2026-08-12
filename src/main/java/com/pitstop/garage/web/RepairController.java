package com.pitstop.garage.web;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.web.dto.RequestRepairRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/repairs")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class RepairController {

    private final CarService carService;
    private final RepairService repairService;
    private final MessageHelper messages;

    public RepairController(CarService carService, RepairService repairService, MessageHelper messages) {
        this.carService = carService;
        this.repairService = repairService;
        this.messages = messages;
    }

    @GetMapping
    public ModelAndView repairsPreview(@AuthenticationPrincipal PitstopUserDetails userData) {

        UUID clientId = userData.getUserId();
        ModelAndView modelAndView = new ModelAndView("repairs");
        modelAndView.addObject("inProgressRepairs", repairService.getInProgressRepairsForClient(clientId));
        modelAndView.addObject("acceptedRepairs", repairService.getWaitingAcceptedRepairsForClient(clientId));
        modelAndView.addObject("pendingRepairs", repairService.getPendingRepairsForClient(clientId));
        return modelAndView;
    }

    @GetMapping("/request")
    public ModelAndView repairRequestForm(@RequestParam(required = false) UUID carId,
                                          @AuthenticationPrincipal PitstopUserDetails userData) {

        ModelAndView modelAndView = new ModelAndView("repair-request");
        modelAndView.addObject("requestRepairRequest", new RequestRepairRequest());

        if (carId != null) {
            Car car = carService.getMyCar(userData.getUserId(), carId);
            modelAndView.addObject("car", car);
        }

        return modelAndView;
    }

    @PostMapping("/{id}/cancel")
    public ModelAndView cancelRepair(@PathVariable UUID id,
                                     @AuthenticationPrincipal PitstopUserDetails userData,
                                     RedirectAttributes redirectAttributes) {

        repairService.cancelRepairByClient(userData.getUserId(), id);

        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.repair.cancelled"));
        return new ModelAndView("redirect:/repairs");
    }

    @PostMapping("/request")
    public ModelAndView submitRepairRequest(@RequestParam UUID carId,
                                            @Valid @ModelAttribute("requestRepairRequest")
                                            RequestRepairRequest requestRepairRequest,
                                            BindingResult bindingResult,
                                            @AuthenticationPrincipal PitstopUserDetails userData,
                                            RedirectAttributes redirectAttributes) {

        UUID userId = userData.getUserId();

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("repair-request", bindingResult.getModel());
            modelAndView.addObject("car", carService.getMyCar(userId, carId));
            return modelAndView;
        }

        repairService.requestRepair(userId, carId, requestRepairRequest);

        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.repair.submitted"));
        return new ModelAndView("redirect:/repairs");
    }

    @GetMapping("/history")
    public ModelAndView repairHistory(@AuthenticationPrincipal PitstopUserDetails userData) {
        UUID clientId = userData.getUserId();
        ModelAndView modelAndView = new ModelAndView("repairs-history");
        modelAndView.addObject("completedRepairs", repairService.getCompletedRepairsForClient(clientId));
        modelAndView.addObject("rejectedRepairs", repairService.getRejectedRepairsForClient(clientId));
        modelAndView.addObject("expiredRepairs", repairService.getExpiredRepairsForClient(clientId));
        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView viewRepairDetails(@PathVariable UUID id,
                                          @AuthenticationPrincipal PitstopUserDetails userData) {

        ServiceRepair repair = repairService.getRepairForClient(userData.getUserId(), id);

        ModelAndView modelAndView = new ModelAndView("repair-details");
        modelAndView.addObject("repair", repair);
        modelAndView.addObject("detailsAudience", "CLIENT");
        return modelAndView;
    }
}