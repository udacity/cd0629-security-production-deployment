package com.encoretickets.authdemo.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * TODO: inspect authentication.getAuthorities() and redirect:
 *         - a user with ROLE_ADMIN -> "/admin"
 *         - everyone else          -> "/dashboard"
 *
 * Hint: authentication.getAuthorities() is a Collection<? extends GrantedAuthority>;
 * each authority's getAuthority() returns a String like "ROLE_ADMIN".
 */
@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        // TODO: replace this with the role-based redirect described above
        response.sendRedirect("/dashboard");
    }
}
