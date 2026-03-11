package com.infy.tmwa.config;

import com.infy.tmwa.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1️⃣ Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2️⃣ If header is missing or invalid → continue filter chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3️⃣ Extract token
        final String token = authHeader.substring(7);

        String username;

        try {
            // 4️⃣ Extract username (email) from token
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5️⃣ If username exists and authentication not already set
        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6️⃣ Load full User entity from DB — gives us role + isActive (F-W2-01)
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            // 7️⃣ Validate token AND check isActive flag (F-W2-01)
            // isEnabled() maps to User.isActive — deactivated users are blocked
            // on every request, not just at login
            if (jwtService.isTokenValid(token) && userDetails.isEnabled()) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities() // ROLE_ADMIN / ROLE_MANAGER etc.
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // 8️⃣ Set authentication in SecurityContext
                // getAuthorities() returns ROLE_ADMIN etc. so
                // @PreAuthorize("hasRole('ADMIN')") works on service methods
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        // 9️⃣ Continue filter chain
        filterChain.doFilter(request, response);
    }
}