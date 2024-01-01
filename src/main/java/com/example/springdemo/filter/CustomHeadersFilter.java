package com.example.springdemo.filter;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class CustomHeadersFilter extends OncePerRequestFilter {

    @Override
    public void doFilterInternal(@NotNull HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        response.addDateHeader("sample-timestamp", System.currentTimeMillis());
        chain.doFilter(request, response);
    }
}
