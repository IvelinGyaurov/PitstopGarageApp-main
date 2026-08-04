package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.web.dto.CompleteRepairRequest;
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
@RequestMapping("/mechanic/repairs")
public class MechanicRepairController {

    private final RepairService repairService;
    private final MessageHelper messages;

    public MechanicRepairController(RepairService repairService, MessageHelper messages) {
        this.repairService = repairService;
        this.messages = messages;
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping
    public ModelAndView repairQueue() {
        ModelAndView modelAndView = new ModelAndView("mechanic-repairs");
        modelAndView.addObject("repairs", repairService.getPendingRepairsForMechanics());
        return modelAndView;
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping("/accepted")
    public ModelAndView acceptedRepairs(@AuthenticationPrincipal PitstopUserDetails userData) {
        UUID mechanicId = userData.getUserId();
        ModelAndView modelAndView = new ModelAndView("mechanic-repairs-accepted");
        modelAndView.addObject("inProgressRepairs", repairService.getInProgressRepairsForMechanic(mechanicId));
        modelAndView.addObject("acceptedRepairs", repairService.getWaitingAcceptedRepairsForMechanic(mechanicId));
        return modelAndView;
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping("/history")
    public ModelAndView repairHistory(@AuthenticationPrincipal PitstopUserDetails userData) {
        UUID mechanicId = userData.getUserId();
        ModelAndView modelAndView = new ModelAndView("mechanic-repairs-history");
        modelAndView.addObject("rejectedRepairs", repairService.getRejectedRepairsForMechanic(mechanicId));
        modelAndView.addObject("completedRepairs", repairService.getCompletedRepairsForMechanic(mechanicId));
        modelAndView.addObject("expiredRepairs", repairService.getExpiredRepairsForMechanic());
        return modelAndView;
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @PostMapping("/{id}/accept")
    public ModelAndView acceptRepair(@PathVariable UUID id,
                                     @AuthenticationPrincipal PitstopUserDetails userData,
                                     RedirectAttributes redirectAttributes) {
        repairService.acceptRepairByMechanic(userData.getUserId(), id);
        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.repair.accepted"));
        return new ModelAndView("redirect:/mechanic/repairs/accepted");    }

    @PreAuthorize("hasRole('MECHANIC')")
    @PostMapping("/{id}/reject")
    public ModelAndView rejectRepair(@PathVariable UUID id,
                                     @AuthenticationPrincipal PitstopUserDetails userData,
                                     RedirectAttributes redirectAttributes) {
        repairService.rejectRepairByMechanic(userData.getUserId(), id);
        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.repair.rejected"));
        return new ModelAndView("redirect:/mechanic/repairs");
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @PostMapping("/{id}/start")
    public ModelAndView startRepair(@PathVariable UUID id,
                                    @AuthenticationPrincipal PitstopUserDetails userData,
                                    RedirectAttributes redirectAttributes) {
        repairService.startRepairByMechanic(userData.getUserId(), id);
        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.repair.started"));
        return new ModelAndView("redirect:/mechanic/repairs/accepted");
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping("/{id}/details")
    public ModelAndView repairDetails(@PathVariable UUID id,
                                      @AuthenticationPrincipal PitstopUserDetails userData) {
        ServiceRepair repair = repairService.getRepairForMechanic(userData.getUserId(), id);

        ModelAndView mav = new ModelAndView("repair-details");
        mav.addObject("repair", repair);
        mav.addObject("detailsAudience", "MECHANIC");
        return mav;
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @PostMapping("/{id}/complete")
    public ModelAndView completeRepair(@PathVariable UUID id,
                                       @AuthenticationPrincipal PitstopUserDetails userData,
                                       @Valid @ModelAttribute("completeRepairRequest") CompleteRepairRequest completeRepairRequest,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("mechanic-complete-repair");
            modelAndView.addObject("repair", repairService.getInProgressRepairForMechanic(userData.getUserId(), id));
            modelAndView.addObject("catalogParts", repairService.getCatalogParts());
            modelAndView.addObject("completeRepairRequest", completeRepairRequest);
            return modelAndView;
        }

        repairService.completeRepairByMechanic(
                userData.getUserId(), id,
                completeRepairRequest.getLaborCost(),
                completeRepairRequest);
        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.repair.completed"));
        return new ModelAndView("redirect:/mechanic/repairs/accepted");
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping("/{id}/complete")
    public ModelAndView completeRepairForm(@PathVariable UUID id,
                                           @AuthenticationPrincipal PitstopUserDetails userData) {
        ModelAndView modelAndView = new ModelAndView("mechanic-complete-repair");
        modelAndView.addObject("repair", repairService.getInProgressRepairForMechanic(userData.getUserId(), id));
        modelAndView.addObject("catalogParts", repairService.getCatalogParts());
        modelAndView.addObject("completeRepairRequest", repairService.buildCompleteRepairForm());
        return modelAndView;
    }
}