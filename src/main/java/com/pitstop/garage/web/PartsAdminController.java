package com.pitstop.garage.web;

import com.pitstop.garage.client.PartsClient;
import com.pitstop.garage.client.dto.CreatePartRequest;
import com.pitstop.garage.web.dto.AddPartRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/parts")
public class PartsAdminController {

    private final PartsClient partsClient;

    public PartsAdminController(PartsClient partsClient) {
        this.partsClient = partsClient;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ModelAndView listParts() {
        ModelAndView modelAndView = new ModelAndView("admin-parts");
        modelAndView.addObject("parts", partsClient.getAllParts());
        return modelAndView;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/add")
    public ModelAndView addPartForm() {
        ModelAndView modelAndView = new ModelAndView("admin-parts-add");
        modelAndView.addObject("addPartRequest", new AddPartRequest());
        return modelAndView;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ModelAndView createPart(@Valid @ModelAttribute("addPartRequest") AddPartRequest addPartRequest,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("admin-parts-add");
            modelAndView.addObject("addPartRequest", addPartRequest);
            return modelAndView;
        }
        CreatePartRequest request = new CreatePartRequest();
        request.setName(addPartRequest.getName().trim());
        request.setSku(addPartRequest.getSku().trim());
        request.setUnitPrice(addPartRequest.getUnitPrice());
        request.setQuantityInStock(addPartRequest.getQuantityInStock());
        partsClient.createPart(request);
        redirectAttributes.addFlashAttribute("successMessage", "Part added successfully.");
        return new ModelAndView("redirect:/admin/parts");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public ModelAndView deletePart(@PathVariable UUID id,
                                   RedirectAttributes redirectAttributes) {
        partsClient.deletePart(id);
        redirectAttributes.addFlashAttribute("successMessage", "Part removed.");
        return new ModelAndView("redirect:/admin/parts");
    }
}