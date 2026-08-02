package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.parts.PartsAdminService;
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

    private final PartsAdminService partsAdminService;
    private final MessageHelper messages;

    public PartsAdminController(PartsAdminService partsAdminService, MessageHelper messages) {
        this.partsAdminService = partsAdminService;
        this.messages = messages;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ModelAndView listParts() {
        ModelAndView modelAndView = new ModelAndView("admin-parts");
        modelAndView.addObject("parts", partsAdminService.getAllParts());
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

        partsAdminService.createPart(addPartRequest);
        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.part.added"));
        return new ModelAndView("redirect:/admin/parts");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public ModelAndView deletePart(@PathVariable UUID id,
                                   RedirectAttributes redirectAttributes) {
        partsAdminService.deletePart(id);
        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.part.removed"));
        return new ModelAndView("redirect:/admin/parts");
    }
}