package com.pitstop.garage.web;

import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/admin/repairs")
public class AdminRepairController {

    private final RepairService repairService;

    public AdminRepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ModelAndView activeRepairs() {
        ModelAndView modelAndView = new ModelAndView("admin-repairs");
        modelAndView.addObject("pendingRepairs", repairService.getPendingRepairsForAdmin());
        modelAndView.addObject("acceptedRepairs", repairService.getAcceptedRepairsForAdmin());
        modelAndView.addObject("inProgressRepairs", repairService.getInProgressRepairsForAdmin());
        return modelAndView;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/history")
    public ModelAndView repairHistory() {
        ModelAndView modelAndView = new ModelAndView("admin-repairs-history");
        modelAndView.addObject("completedRepairs", repairService.getCompletedRepairsForAdmin());
        modelAndView.addObject("rejectedRepairs", repairService.getRejectedRepairsForAdmin());
        modelAndView.addObject("expiredRepairs", repairService.getExpiredRepairsForAdmin());
        return modelAndView;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/details")
    public ModelAndView repairDetails(@PathVariable UUID id) {
        ServiceRepair repair = repairService.getRepairForAdmin(id);

        ModelAndView modelAndView = new ModelAndView("repair-details");
        modelAndView.addObject("repair", repair);
        modelAndView.addObject("detailsAudience", "ADMIN");
        return modelAndView;
    }
}