package com.pitstop.garage.web;

import com.pitstop.garage.exceptions.IncorrectUsernameOrPasswordException;
import com.pitstop.garage.exceptions.UserAlreadyExistException;
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
}
