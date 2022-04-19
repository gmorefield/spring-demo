package com.example.springdemo.controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

import com.example.springdemo.data.PersonRepository;
import com.example.springdemo.model.Person;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController()
public class PersonController {
    private PersonRepository personRepository;

    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping(path = "/person/{id}")
    public Person getOne(@PathVariable("id") long id) {
        Person p = personRepository.findById(id);
        if (p == null) {
            throw new ResponseStatusException(NOT_FOUND, "Person " + id + " not found");
        }
        return p;
    }

    @GetMapping(path = "/person")
    public List<Person> getAll() {
        try {
            return personRepository.findAll();
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to load persons");
        }
    }

    @PostMapping(path = "/person")
    public @ResponseBody Person save(@RequestBody Person newPerson) {
        return personRepository.save(newPerson);
    }
}
