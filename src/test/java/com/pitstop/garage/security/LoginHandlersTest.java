package com.pitstop.garage.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginHandlersTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Test
    void loginSuccess_redirectsToHome() throws Exception {
        new LoginSuccessHandler().onAuthenticationSuccess(request, response, authentication);
        verify(response).sendRedirect("/home");
    }

    @Test
    void loginFailure_whenDisabled_redirectsInactive() throws Exception {
        new LoginFailureHandler().onAuthenticationFailure(request, response, new DisabledException("off"));
        verify(response).sendRedirect("/login?inactive");
    }

    @Test
    void loginFailure_whenLocked_redirectsInactive() throws Exception {
        new LoginFailureHandler().onAuthenticationFailure(request, response, new LockedException("locked"));
        verify(response).sendRedirect("/login?inactive");
    }

    @Test
    void loginFailure_whenBadCredentials_redirectsError() throws Exception {
        new LoginFailureHandler().onAuthenticationFailure(request, response, new BadCredentialsException("bad"));
        verify(response).sendRedirect("/login?error");
    }
}
