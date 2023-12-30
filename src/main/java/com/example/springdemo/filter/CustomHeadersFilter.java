package com.example.springdemo.filter;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
