package com.spaceora.auth.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.spaceora.auth.service.JwtService;
import com.spaceora.user.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter that validates the Bearer token on every request
 * and populates the SecurityContext from the token's own claims (email subject,
 * userId, role) — without issuing a database query. The UserDetailsService is
 * therefore not called in the hot request path; it is used only by
 * DaoAuthenticationProvider during the login flow.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        DecodedJWT decoded;
        try {
            decoded = jwtService.validateToken(token);
        } catch (JWTVerificationException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Build Authentication entirely from JWT claims — no DB round-trip.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractEmail(decoded);
            String roleName = decoded.getClaim("role").asString();

            // Guard against missing role claim (malformed token that still verified).
            if (email == null || roleName == null) {
                log.debug("JWT missing subject or role claim");
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + roleName));

            // Use the email (== getUsername()) as the principal so that
            // authentication.getName() returns the email in controllers.
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
