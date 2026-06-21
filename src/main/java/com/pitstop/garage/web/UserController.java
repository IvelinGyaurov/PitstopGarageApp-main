package com.pitstop.garage.web;

import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.EditProfileRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/profile")
    public ModelAndView getProfilePage(@PathVariable UUID id, Model model) {

        User user = userService.getById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("profile-menu");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @PutMapping("/{id}/profile")
    public ModelAndView updateProfile(@Valid EditProfileRequest editProfileRequest, BindingResult bindingResult, @PathVariable UUID id) {

        if(bindingResult.hasErrors()) {
            User user = userService.getById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("profile-menu");
            modelAndView.addObject("user", user);
            modelAndView.addObject("editProfileRequest", editProfileRequest);
            return modelAndView;
        }

        userService.updateProfile(id, editProfileRequest);

        return new ModelAndView("redirect:/home");

    }

    @PostMapping("/{id}/role")
    public ModelAndView changeRole(@PathVariable UUID id,
                                   @RequestParam UserRole role,
                                   RedirectAttributes redirectAttributes) {

        userService.changeRole(id, role);
        redirectAttributes.addFlashAttribute("successMessage", "Role updated successfully.");
        return new ModelAndView("redirect:/users");
    }

    @PostMapping("/{id}/status")
    public ModelAndView changeStatus(@PathVariable UUID id,
                                     @RequestParam boolean active,
                                     RedirectAttributes redirectAttributes) {
        userService.changeActiveStatus(id, active);
        redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
        return new ModelAndView("redirect:/users");
    }
    @GetMapping
    public ModelAndView getUsers() {
        List<User> users = userService.getAll();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("users");
        modelAndView.addObject("users", users);
        modelAndView.addObject("firstUserId",
                userService.getFirstRegisteredUserId().orElse(null));
        return modelAndView;
    }


}
