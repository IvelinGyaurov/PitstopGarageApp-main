package com.pitstop.garage.web;

import com.pitstop.garage.car.service.CarService;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.LoginRequest;
import com.pitstop.garage.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class IndexController {

    private final UserService userService;
    private final CarService carService;
    private final RepairService repairService;

    public IndexController(UserService userService,
                           CarService carService,
                           RepairService repairService) {
        this.userService = userService;
        this.carService = carService;
        this.repairService = repairService;
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
    public ModelAndView getHomePage(@AuthenticationPrincipal PitstopUserDetails userData) {

        ModelAndView modelAndView = new ModelAndView("home");
        User user = userService.getById(userData.getUserId());
        modelAndView.addObject("userId", user.getId());
        modelAndView.addObject("username", user.getUsername());
        modelAndView.addObject("role", user.getRole());
        modelAndView.addObject("profilePicture", user.getProfilePicture());

        if (user.getRole() == UserRole.USER) {
            modelAndView.addObject("carsCount", carService.getMyCars(user.getId()).size());
            modelAndView.addObject("activeRepairsCount", repairService.getMyRepairs(user.getId()).size());
        } else if (user.getRole() == UserRole.MECHANIC) {
            modelAndView.addObject("bayCount", repairService.getAcceptedRepairsForMechanic(user.getId()).size());
            modelAndView.addObject("queueCount", repairService.getPendingRepairsForMechanics().size());
        } else if (user.getRole() == UserRole.ADMIN) {
            int activeRepairs = repairService.getPendingRepairsForAdmin().size()
                    + repairService.getAcceptedRepairsForAdmin().size()
                    + repairService.getInProgressRepairsForAdmin().size();
            modelAndView.addObject("activeRepairsCount", activeRepairs);
            modelAndView.addObject("usersCount", userService.countUsers());
        }

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
