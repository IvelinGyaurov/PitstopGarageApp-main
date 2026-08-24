package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class ExceptionAdvice {

    private final MessageHelper messages;

    public ExceptionAdvice(MessageHelper messages) {
        this.messages = messages;
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public String handleUserAlreadyExist(RedirectAttributes redirectAttributes,
                                         UserAlreadyExistException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/register";
    }

    @ExceptionHandler(VinAlreadyExistsException.class)
    public String handleVinAlreadyExists(RedirectAttributes redirectAttributes,
                                         VinAlreadyExistsException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/cars/add";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", messages.get("error.dataIntegrity"));
        return "redirect:/home";
    }

    @ExceptionHandler(CarNotFoundException.class)
    public String handleCarNotFound(RedirectAttributes redirectAttributes,
                                    CarNotFoundException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/cars";
    }

    @ExceptionHandler(CarHasActiveRepairException.class)
    public String handleCarHasActiveRepair(RedirectAttributes redirectAttributes,
                                           CarHasActiveRepairException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/cars";
    }

    @ExceptionHandler(PartSkuAlreadyExistsException.class)
    public String handlePartSkuAlreadyExists(RedirectAttributes redirectAttributes,
                                             PartSkuAlreadyExistsException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/admin/parts/add";
    }

    @ExceptionHandler(InsufficientPartStockException.class)
    public String handleInsufficientPartStock(RedirectAttributes redirectAttributes,
                                              InsufficientPartStockException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/mechanic/repairs/" + exception.getRepairId() + "/complete";
    }

    @ExceptionHandler(RepairNotFoundException.class)
    public String handleRepairNotFound(RedirectAttributes redirectAttributes,
                                       RepairNotFoundException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return redirectAfterRepairError();
    }

    @ExceptionHandler(RepairStatusException.class)
    public String handleRepairStatus(RedirectAttributes redirectAttributes,
                                     RepairStatusException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return redirectAfterRepairError();
    }

    @ExceptionHandler(UserInactiveException.class)
    public String handleUserInactive(RedirectAttributes redirectAttributes,
                                     UserInactiveException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/users";
    }

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFound(RedirectAttributes redirectAttributes,
                                     UserNotFoundException exception) {
        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
        return "redirect:/home";
    }

    @ExceptionHandler(PrimaryUserException.class)
    public String handlePrimaryUser(RedirectAttributes redirectAttributes,
                                    PrimaryUserException exception) {

        redirectAttributes.addFlashAttribute("errorMessage", messages.get(exception.getMessage()));
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
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject("status", 404);
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        modelAndView.addObject("status", 500);
        return modelAndView;
    }

    private String redirectAfterRepairError() {
        if (hasRole("ROLE_ADMIN")) {
            return "redirect:/admin/repairs";
        }
        if (hasRole("ROLE_MECHANIC")) {
            return "redirect:/mechanic/repairs";
        }
        return "redirect:/repairs";
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
