package com.baomidou.lock.model;

import lombok.Data;

@Data
public class User {

    private Long id;

    private String name;

    private String address;

    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
