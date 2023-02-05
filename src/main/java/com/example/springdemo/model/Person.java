package com.example.springdemo.model;

import javax.xml.bind.annotation.XmlRootElement;

// @jakarta.xml.bind.annotation.XmlRootElement(name="person")
@XmlRootElement(name="person")
public class Person {
    private long id;
    private String firstName;
    private String lastName;

    public Person() {}
    
    public Person(long id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
