package com.pitstop.garage.web;

import com.pitstop.garage.exceptions.CarNotFoundException;
import com.pitstop.garage.exceptions.IncorrectUsernameOrPasswordException;
import com.pitstop.garage.exceptions.RepairNotFoundException;
import com.pitstop.garage.exceptions.RepairStatusException;
import com.pitstop.garage.exceptions.UserAlreadyExistException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(IncorrectUsernameOrPasswordException.class)
    public String handleIncorrectUsernameOrPassword(RedirectAttributes redirectAttributes,
                                                   IncorrectUsernameOrPasswordException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/login";
    }

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
}
