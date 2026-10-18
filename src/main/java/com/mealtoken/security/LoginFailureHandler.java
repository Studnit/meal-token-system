package com.mealtoken.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Wrong username/password and a suspended account are told apart, and a server-side problem is logged with its real cause
 * instead of being shown as "invalid credentials". The page never reveals whether the username exists.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        String target;
        if (ex instanceof BadCredentialsException || ex instanceof UsernameNotFoundException) {
            target = "/login?error";
            log.info("Sign-in rejected: wrong username or password");
        } else if (ex instanceof LockedException || ex instanceof DisabledException) {
            target = "/login?locked";
            log.info("Sign-in rejected: account is not active ({})", ex.getClass().getSimpleName());
        } else {
            target = "/login?failure";
            log.error("Sign-in failed because of a server problem, not the credentials", ex.getCause() != null ? ex.getCause() : ex);
        }
        res.sendRedirect(req.getContextPath() + target);
    }
}
