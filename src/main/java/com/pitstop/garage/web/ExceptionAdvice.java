package com.pitstop.garage.web;

import com.pitstop.garage.exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class ExceptionAdvice {


    @ExceptionHandler(UserAlreadyExistException.class)
    public String handleUserAlreadyExist(RedirectAttributes redirectAttributes,
                                         UserAlreadyExistException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler(VinAlreadyExistsException.class)
    public String handleVinAlreadyExists(RedirectAttributes redirectAttributes,
                                         VinAlreadyExistsException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/cars/add";
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

    @ExceptionHandler(PartSkuAlreadyExistsException.class)
    public String handlePartSkuAlreadyExists(RedirectAttributes redirectAttributes,
                                             PartSkuAlreadyExistsException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/admin/parts/add";
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

    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ModelAndView handleNotFound(Exception exception) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject("status", 404);
        return modelAndView;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException exception) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(HttpStatus.FORBIDDEN);
        modelAndView.addObject("status", 500);
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception exception) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        modelAndView.addObject("status", 500);
        return modelAndView;
    }
}
