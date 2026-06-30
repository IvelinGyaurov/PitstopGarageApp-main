package com.pitstop.garage.web;

import com.pitstop.garage.exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ExceptionAdvice {


    @ExceptionHandler(UserAlreadyExistException.class)
    public String handleUserAlreadyExist(RedirectAttributes redirectAttributes,
                                         UserAlreadyExistException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", "Username or email already exists.");
        return "redirect:/register";
    }

    @ExceptionHandler(CarNotFoundException.class)
    public String handleCarNotFound(RedirectAttributes redirectAttributes,
                                    CarNotFoundException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/cars";
    }

    @ExceptionHandler(RepairNotFoundException.class)
    public String handleRepairNotFound(RedirectAttributes redirectAttributes,
                                       RepairNotFoundException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/repairs";
    }

    @ExceptionHandler(RepairStatusException.class)
    public String handleRepairStatus(RedirectAttributes redirectAttributes,
                                     RepairStatusException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/repairs";
    }

    @ExceptionHandler(UserInactiveException.class)
    public String handleUserInactive(RedirectAttributes redirectAttributes,
                                     UserInactiveException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/users";
    }

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFound(RedirectAttributes redirectAttributes,
                                     UserNotFoundException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/home";
    }

    @ExceptionHandler(PrimaryUserException.class)
    public String handlePrimaryUser(RedirectAttributes redirectAttributes,
                                    PrimaryUserException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/users";
    }
}
