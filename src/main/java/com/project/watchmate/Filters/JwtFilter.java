package com.project.watchmate.Filters;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.watchmate.Services.JwtService;
import com.project.watchmate.Services.WmUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter{

    private final JwtService jwtService; 

    final WmUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtService.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.validateToken(token, userDetails)){
                        UsernamePasswordAuthenticationToken token2 = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        token2.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(token2);
                        log.info("Authenticated request via JWT username={} path={}", username, request.getRequestURI());
                    } else {
                        log.warn("Rejected JWT for username={} path={}", username, request.getRequestURI());
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("JWT authentication failed path={} reason={} correlationId={}",
                    request.getRequestURI(),
                    ex.getClass().getSimpleName(),
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY));
            }
        }
        filterChain.doFilter(request, response);
    }
}
