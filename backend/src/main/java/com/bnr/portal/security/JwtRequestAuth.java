package com.bnr.portal.security;

import com.bnr.portal.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestAuth extends OncePerRequestFilter {

    private final JwtIssuer jwtIssuer;
    private final UserRepository users;

    public JwtRequestAuth(JwtIssuer jwtIssuer, UserRepository users) {
        this.jwtIssuer = jwtIssuer;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String raw = header.substring(7).trim();
            if (!raw.isEmpty()) {
                try {
                    Long userId = jwtIssuer.userIdFromToken(raw);
                    users.findById(userId).ifPresent(row -> {
                        var signedIn = new SignedInUser(row);
                        var auth = new UsernamePasswordAuthenticationToken(
                                signedIn,
                                null,
                                signedIn.getAuthorities()
                        );
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        chain.doFilter(request, response);
    }
}
