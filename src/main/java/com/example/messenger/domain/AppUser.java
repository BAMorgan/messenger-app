package com.example.messenger.domain;

import jakarta.persistence.*;


@Entity //tells JPA that this object should be mapped to a table in relational DB
@Table(name = "app_user")
public class AppUser {

    @Id //Id marks the primary key of the table
    @GeneratedValue(strategy = GenerationType.IDENTITY) //DB generates the ID (auto-increment)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    //JPA requires a no-arg constructor to instantiate objects when loading from DB
    protected AppUser() {}

    public AppUser(String username){
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
