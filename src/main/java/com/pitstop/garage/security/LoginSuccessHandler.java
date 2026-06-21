package com.pitstop.garage.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        PitstopUserDetails principal = (PitstopUserDetails) authentication.getPrincipal();

        HttpSession session = request.getSession();
        session.setAttribute("userId", principal.getUserId());
        session.setAttribute("username", principal.getUsername());
        session.setAttribute("role", principal.getRole());

        response.sendRedirect("/home");
    }
}