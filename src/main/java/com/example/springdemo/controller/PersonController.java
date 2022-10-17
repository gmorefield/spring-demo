package com.example.springdemo.controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.springdemo.data.PersonRepository;
import com.example.springdemo.model.Person;


@RestController()
public class PersonController {
    private PersonRepository personRepository;

    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping(path = "/person/{id}")
    public ResponseEntity<Person> getOne(@PathVariable("id") long id) {
        Person p = personRepository.findById(id);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(p);
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

    @RequestMapping(value="/person/echo", method={RequestMethod.GET})
    public Person getEcho(Person person) {
        return person;
    }

    @RequestMapping(value="/person/echo", method={RequestMethod.POST})
    public Person postEcho(@RequestBody Person person) {
        return person;
    }

}
