package com.pitstop.garage.security;

import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

@Component
public class SessionCheckInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Autowired
    public SessionCheckInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handle) throws Exception {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("/login");
            return false;
        }
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        User user = userService.getById((UUID) userId);
        if (!user.isActive()){
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
