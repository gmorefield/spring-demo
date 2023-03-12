package com.example.springdemo.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles({"jwt","test"})
@SpringBootTest(properties = {})
public class ActuatorTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .alwaysDo(MockMvcResultHandlers.print())
                .apply(SecurityMockMvcConfigurers.springSecurity(springSecurityFilterChain))
                .build();
    }

    @Test
    public void testHealth() throws Exception {
        mvc.perform(get("/actuator/health")
                .with(anonymous()))
                .andExpect(status().isOk());
    }

    @Test
    public void testInfo() throws Exception {
        mvc.perform(get("/actuator/info")
                .with(anonymous()))
                .andExpect(status().isOk());
    }

    @Test
    public void testRefreshAsAnonymousIsUnauthorized() throws Exception {
        mvc.perform(post("/actuator/refresh")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRefreshAsUserIsUnauthorized() throws Exception {
        mvc.perform(post("/actuator/refresh")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_execute"))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRefreshAsAdminIsAllowed() throws Exception {
        mvc.perform(post("/actuator/refresh")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_admin"))))
                .andExpect(status().isOk());
    }

}
