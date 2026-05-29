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
import org.springframework.web.servlet.ModelAndView;

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
    public ModelAndView getLoginPage() {
        ModelAndView modelAndView = new ModelAndView("login");
        modelAndView.addObject("loginRequest", new LoginRequest());

        return modelAndView;
    }

    @PostMapping("/login")
    public ModelAndView loginUser(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                                  BindingResult bindingResult,
                                  HttpSession session) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("login");
            modelAndView.addAllObjects(bindingResult.getModel());
            return modelAndView;
        }

        User user = userService.login(loginRequest);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        return new ModelAndView("redirect:/home");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView modelAndView = new ModelAndView("home");
        User user = userService.getById((UUID) session.getAttribute("userId"));
        modelAndView.addObject("userId", user.getId());
        modelAndView.addObject("username", user.getUsername());
        modelAndView.addObject("role", user.getRole());
        modelAndView.addObject("profilePicture", user.getProfilePicture());

        return modelAndView;
    }

    @GetMapping("/logout")
    public ModelAndView logoutUser(HttpSession session) {
        session.invalidate();

        return new ModelAndView("redirect:/index");
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                        BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("register");
            modelAndView.addAllObjects(bindingResult.getModel());
            return modelAndView;
        }

        userService.registerUser(registerRequest);
        return new ModelAndView("redirect:/login");
    }
}
