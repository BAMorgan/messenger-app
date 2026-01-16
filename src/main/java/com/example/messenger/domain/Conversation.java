package com.example.messenger.domain;

import jakarta.persistence.*;

@Entity
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) //Many conversations can reference the same user!
                                 //Creates user_a_id foreign key column
                                 //Each conversation *must* have many users
    private AppUser userA;

    @ManyToOne(optional = false) //Creates user_b_id foreign key column
    private AppUser userB;

    protected Conversation() {}

    public Conversation(AppUser userA, AppUser userB) {
        this.userA = userA;
        this.userB = userB;
    }

    public Long getId() {
        return id;
    }
}
