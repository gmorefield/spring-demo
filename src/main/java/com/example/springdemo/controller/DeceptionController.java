package com.example.springdemo.controller;

import com.example.springdemo.model.Person;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequestMapping("/deception")
@RestController
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
@Slf4j
public class DeceptionController {

    private final WebClient webClient;

    public DeceptionController(WebClient webClient) {
        this.webClient = webClient;
    }

    @RequestMapping(value = "/runAround", method = RequestMethod.GET)
    public ModelAndView runAround() {
        // validate API calls can be made before redirect
        webClient.get()
                .uri("/data/xml")
                .retrieve()
                .toEntity(Person.class)
                .block();

        return new ModelAndView("redirect:" + "/deception/landing");
    }

    @RequestMapping(value = "/landing", method = RequestMethod.GET)
    public ResponseEntity<String> entryPoint() {

        return ResponseEntity.ok("Made it");
    }

    @RequestMapping(value="/waitForIt", method=RequestMethod.GET)
    public void waitForIt(@RequestParam(required = false) Optional<Long> delay,
                          @RequestParam(required = false) Optional<Long> increment,
                          HttpServletResponse response) throws IOException {
        long sleepTime = delay.orElse(15L);
        long start = System.currentTimeMillis();

        response.setStatus(200);
        while (System.currentTimeMillis() < (start + sleepTime*1000)) {
            try {
                TimeUnit.SECONDS.sleep(increment.orElse(5L));

                response.getWriter().println("Completed " + (System.currentTimeMillis()-start)/1000 + " secs of processing");
                response.getWriter().flush();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                response.setStatus(500);
                log.error("Failed to process", e);
                return;
            }
        }
        log.info("waitForIt finished in {}s", (System.currentTimeMillis()-start)/1000);
        response.getWriter().println("waitForIt finished in " + (System.currentTimeMillis()-start)/1000 + "s");
    }
}
