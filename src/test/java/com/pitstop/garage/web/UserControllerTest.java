package com.pitstop.garage.web;

import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.EditProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserController controller;

    @Test
    void getProfilePage_returnsProfile() {
        UUID id = UUID.randomUUID();
        User user = user(id, UserRole.USER);
        when(userService.getById(id)).thenReturn(user);

        ModelAndView mav = controller.getProfilePage(id);

        assertEquals("profile-menu", mav.getViewName());
        assertEquals(user, mav.getModel().get("user"));
    }

    @Test
    void updateProfile_whenValid_redirectsHome() {
        UUID id = UUID.randomUUID();
        EditProfileRequest edit = EditProfileRequest.builder().firstName("John").build();
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = controller.updateProfile(edit, bindingResult, id);

        verify(userService).updateProfile(id, edit);
        assertEquals("redirect:/home", mav.getViewName());
    }

    @Test
    void updateProfile_whenInvalid_staysOnProfile() {
        UUID id = UUID.randomUUID();
        EditProfileRequest edit = new EditProfileRequest();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(userService.getById(id)).thenReturn(user(id, UserRole.USER));

        ModelAndView mav = controller.updateProfile(edit, bindingResult, id);

        assertEquals("profile-menu", mav.getViewName());
        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    void getUsers_returnsUsersView() {
        when(userService.getAll()).thenReturn(List.of());
        when(userService.getSoleActiveAdminId()).thenReturn(Optional.empty());

        ModelAndView mav = controller.getUsers();

        assertEquals("users", mav.getViewName());
    }

    @Test
    void changeRole_forOtherUser_redirectsUsers() {
        UUID targetId = UUID.randomUUID();
        PitstopUserDetails current = new PitstopUserDetails(
                UUID.randomUUID(), "admin", "pass", UserRole.ADMIN, true);

        ModelAndView mav = controller.changeRole(
                targetId, UserRole.MECHANIC, current, request, redirectAttributes);

        verify(userService).changeRole(targetId, UserRole.MECHANIC);
        assertEquals("redirect:/users", mav.getViewName());
    }

    @Test
    void changeRole_selfDemotion_redirectsHome() {
        UUID id = UUID.randomUUID();
        PitstopUserDetails current = new PitstopUserDetails(id, "admin", "pass", UserRole.ADMIN, true);
        when(request.getSession()).thenReturn(session);

        ModelAndView mav = controller.changeRole(
                id, UserRole.USER, current, request, redirectAttributes);

        verify(userService).changeRole(id, UserRole.USER);
        assertEquals("redirect:/home", mav.getViewName());
        SecurityContextHolder.clearContext();
    }

    @Test
    void changeRole_selfStillAdmin_redirectsUsers() {
        UUID id = UUID.randomUUID();
        PitstopUserDetails current = new PitstopUserDetails(id, "admin", "pass", UserRole.ADMIN, true);
        when(request.getSession()).thenReturn(session);

        ModelAndView mav = controller.changeRole(
                id, UserRole.ADMIN, current, request, redirectAttributes);

        verify(userService).changeRole(id, UserRole.ADMIN);
        assertEquals("redirect:/users", mav.getViewName());
        SecurityContextHolder.clearContext();
    }

    @Test
    void changeStatus_forOtherUser_redirectsUsers() {
        UUID targetId = UUID.randomUUID();
        PitstopUserDetails current = new PitstopUserDetails(
                UUID.randomUUID(), "admin", "pass", UserRole.ADMIN, true);

        ModelAndView mav = controller.changeStatus(
                targetId, false, current, request, response, redirectAttributes);

        verify(userService).changeActiveStatus(targetId, false);
        assertEquals("redirect:/users", mav.getViewName());
    }

    @Test
    void changeStatus_selfDeactivate_logsOutAndRedirectsLogin() {
        UUID id = UUID.randomUUID();
        PitstopUserDetails current = new PitstopUserDetails(id, "admin", "pass", UserRole.ADMIN, true);
        when(request.getSession(false)).thenReturn(session);

        ModelAndView mav = controller.changeStatus(
                id, false, current, request, response, redirectAttributes);

        verify(userService).changeActiveStatus(id, false);
        assertEquals("redirect:/login", mav.getViewName());
        SecurityContextHolder.clearContext();
    }

    @Test
    void changeStatus_selfStillActive_refreshesAuthAndRedirectsUsers() {
        UUID id = UUID.randomUUID();
        PitstopUserDetails current = new PitstopUserDetails(id, "admin", "pass", UserRole.ADMIN, true);
        when(request.getSession()).thenReturn(session);

        ModelAndView mav = controller.changeStatus(
                id, true, current, request, response, redirectAttributes);

        verify(userService).changeActiveStatus(id, true);
        assertEquals("redirect:/users", mav.getViewName());
        SecurityContextHolder.clearContext();
    }

    private User user(UUID id, UserRole role) {
        return User.builder()
                .id(id)
                .username("user")
                .email("user@mail.com")
                .password("encoded")
                .role(role)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }
}
