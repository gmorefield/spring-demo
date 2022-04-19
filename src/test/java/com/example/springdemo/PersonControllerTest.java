package com.example.springdemo;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import com.example.springdemo.model.Person;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class PersonControllerTest extends BaseControllerTest {

    @Test
    public void getAll_emptyTable_shouldReturnEmptyArray() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/person")
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(personRepository, times(1)).findAll();
    }

    @Test
    public void getAll_withException_shouldReturnErrorMessage() throws Exception {
        // given(personService.loadAll()).willThrow(new RuntimeException());
        when(personRepository.findAll()).thenThrow(new RuntimeException("Failed to load persons"));
        mvc.perform(MockMvcRequestBuilders.get("/person")
                .accept(APPLICATION_JSON))
                .andExpect(status().is5xxServerError())
                .andExpect(status().reason("Failed to load persons"));
        verify(personRepository, times(1)).findAll();
    }

    @Test
    public void getAll_withMultipleResults_shouldReturnPopulatedArray() throws Exception {
        List<Person> expected = Arrays.asList(
                new Person(1, "John", "Doe"),
                new Person(2, "Jane", "Doe"));
        when(personRepository.findAll()).thenReturn(expected);
        mvc.perform(MockMvcRequestBuilders.get("/person")
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(expected)));
        verify(personRepository, times(1)).findAll();
    }

}
