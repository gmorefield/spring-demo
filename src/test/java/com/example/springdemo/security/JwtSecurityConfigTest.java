package com.example.springdemo.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springdemo.config.security.JwtSecurityConfig;
import com.example.springdemo.config.security.TokenConfig;
import com.example.springdemo.security.JwtSecurityConfigTest.MockPersonController;

@WebMvcTest({ JwtSecurityConfig.class, TokenConfig.class })
@Import(MockPersonController.class)
@ActiveProfiles("jwt")
// @EnableAutoConfiguration
public class JwtSecurityConfigTest {
    @Autowired
    MockMvc mvc;

    @Test
    void rootWhenAnonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/person"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rootWhenUserIsSuccessful() throws Exception {
        mvc.perform(get("/person")
                .with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_execute"))))
                .andExpect(status().isOk())
                .andExpect(content().string("person"));
    }

    @RestController
    @RequestMapping("/person")
    static class MockPersonController {
        @GetMapping()
        public String getInfo() {
            return "person";
        }
    }

}
