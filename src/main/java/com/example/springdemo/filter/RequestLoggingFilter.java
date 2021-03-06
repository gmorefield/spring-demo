package com.example.springdemo.filter;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/** 
 * Sample filter registered via RegistrationBean in AppConfig.
 * Alternatively, @Component could be added to this Filter to register as a bean
 */
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String id = UUID.randomUUID().toString();
        logger.info("Processing Request {}: {} - {}", id, req.getMethod(), req.getRequestURI());
        chain.doFilter(request, response);
        logger.info("Request Complete {}: {} ", id, res.getContentType());
    }
}
