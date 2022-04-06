package com.example.springdemo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController()
public class PersonController {
    private PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping(path = "/person/{id}")
    public Person get(@PathVariable("id") long id) {
        Person p = personService.get(id);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person " + id + " not found");
        }
        return p;
    }

    @GetMapping(path = "/person")
    public List<Person> getAll() {
        return personService.loadAll();
    }

    @PostMapping(path = "/person")
    public @ResponseBody Person save(@RequestBody Person newPerson) {
        return personService.save(newPerson);
    }
}
