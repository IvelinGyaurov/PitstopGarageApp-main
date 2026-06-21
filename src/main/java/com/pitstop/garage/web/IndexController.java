package com.pitstop.garage.web;

import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.LoginRequest;
import com.pitstop.garage.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class IndexController {

    private final UserService userService;

    public IndexController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/index"})
    public ModelAndView getIndexPage() {
        return new ModelAndView("index");
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(value = "error", required = false) String error,
                                     @RequestParam(value = "inactive", required = false) String inactive) {
        ModelAndView modelAndView = new ModelAndView("login");
        modelAndView.addObject("loginRequest", new LoginRequest());

        if (inactive != null) {
            modelAndView.addObject("errorMessage", "Your account is inactive.");
        } else if (error != null) {
            modelAndView.addObject("errorMessage", "Incorrect username or password.");
        }

        return modelAndView;
    }


    @GetMapping("/home")
    public ModelAndView getHomePage(HttpSession session) {

        ModelAndView modelAndView = new ModelAndView("home");
        User user = userService.getById((UUID) session.getAttribute("userId"));
        modelAndView.addObject("userId", user.getId());
        modelAndView.addObject("username", user.getUsername());
        modelAndView.addObject("role", user.getRole());
        modelAndView.addObject("profilePicture", user.getProfilePicture());

        return modelAndView;
    }


    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("register", bindingResult.getModel());
            return modelAndView;
        }

        userService.registerUser(registerRequest);
        redirectAttributes.addFlashAttribute("successMessage", "You have registered successfully!");
        return new ModelAndView("redirect:/login");
    }
}
