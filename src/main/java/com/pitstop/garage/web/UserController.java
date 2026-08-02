package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.EditProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
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
    private final MessageHelper messages;

    @Autowired
    public UserController(UserService userService, MessageHelper messages) {
        this.userService = userService;
        this.messages = messages;
    }

    @PreAuthorize("#id == authentication.principal.userId")
    @GetMapping("/{id}/profile")
    public ModelAndView getProfilePage(@PathVariable UUID id) {

        User user = userService.getById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("profile-menu");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @PreAuthorize("#id == authentication.principal.userId")
    @PutMapping("/{id}/profile")
    public ModelAndView updateProfile(@Valid EditProfileRequest editProfileRequest,
                                      BindingResult bindingResult,
                                      @PathVariable UUID id) {

        if (bindingResult.hasErrors()) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView changeRole(@PathVariable UUID id,
                                   @RequestParam UserRole role,
                                   @AuthenticationPrincipal PitstopUserDetails currentUser,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {

        userService.changeRole(id, role);

        if (currentUser.getUserId().equals(id)) {
            refreshCurrentAuthentication(currentUser, role, currentUser.isActive(), request);
            redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.user.roleUpdated"));
            if (role != UserRole.ADMIN) {
                return new ModelAndView("redirect:/home");
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.user.roleUpdated"));
        return new ModelAndView("redirect:/users");
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView changeStatus(@PathVariable UUID id,
                                     @RequestParam boolean active,
                                     @AuthenticationPrincipal PitstopUserDetails currentUser,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     RedirectAttributes redirectAttributes) {
        userService.changeActiveStatus(id, active);

        if (currentUser.getUserId().equals(id) && !active) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return new ModelAndView("redirect:/login");
        }

        if (currentUser.getUserId().equals(id)) {
            refreshCurrentAuthentication(currentUser, currentUser.getRole(), active, request);
        }

        redirectAttributes.addFlashAttribute("successMessage", messages.get("flash.user.statusUpdated"));
        return new ModelAndView("redirect:/users");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getUsers() {
        List<User> users = userService.getAll();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("users");
        modelAndView.addObject("users", users);
        modelAndView.addObject("soleAdminId",
                userService.getSoleActiveAdminId().orElse(null));
        return modelAndView;
    }

    private void refreshCurrentAuthentication(PitstopUserDetails currentUser,
                                              UserRole role,
                                              boolean active,
                                              HttpServletRequest request) {
        PitstopUserDetails updatedUser = new PitstopUserDetails(
                currentUser.getUserId(),
                currentUser.getUsername(),
                currentUser.getPassword(),
                role,
                active
        );
        Authentication refreshed = new UsernamePasswordAuthenticationToken(
                updatedUser,
                updatedUser.getPassword(),
                updatedUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(refreshed);
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
    }
}
