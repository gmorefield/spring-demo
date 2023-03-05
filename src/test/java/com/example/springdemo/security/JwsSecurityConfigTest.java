package com.example.springdemo.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springdemo.config.security.JwtProperties;
import com.example.springdemo.config.security.JwtSecurityConfig;
import com.example.springdemo.config.security.TokenConfig;
import com.example.springdemo.controller.TokenController;
import com.example.springdemo.security.JwtSecurityConfigTest.MockPersonController;

@WebMvcTest({ JwtSecurityConfig.class, TokenConfig.class, TokenController.class, JwtProperties.class })
@TestPropertySource(properties = { "basic.user=testUser", "basic.admin=testAdmin", "basic.password=pass" })
@Import(MockPersonController.class)
@ActiveProfiles("jws")
// @EnableAutoConfiguration
public class JwsSecurityConfigTest {
    @Autowired
    MockMvc mvc;

    @Test
    void whenAnonymous_personRequestUnauthorized() throws Exception {
        mvc.perform(get("/person"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenScopeExecute_personRequestGranted() throws Exception {
        mvc.perform(get("/person")
                .with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_execute"))))
                .andExpect(status().isOk())
                .andExpect(content().string("person"));
    }

    @Test
    void whenUser_personRequestGranted() throws Exception {
        MvcResult result = this.mvc.perform(post("/token/jws")
                .with(httpBasic("testUser", "pass")))
                .andExpect(status().isOk())
                .andReturn();

        String token = result.getResponse().getContentAsString();

        this.mvc.perform(get("/person")
                .header("Authorization", "Bearer " + token))
                .andExpect(content().string("person"));
    }

    @Test
    void whenAdmin_personRequestGranted() throws Exception {
        MvcResult result = this.mvc.perform(post("/token/jws")
                .with(httpBasic("testAdmin", "pass")))
                .andExpect(status().isOk())
                .andReturn();

        String token = result.getResponse().getContentAsString();

        this.mvc.perform(get("/person")
                .header("Authorization", "Bearer " + token))
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
