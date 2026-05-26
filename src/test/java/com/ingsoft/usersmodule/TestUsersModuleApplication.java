package com.ingsoft.usersmodule;

import org.springframework.boot.SpringApplication;

public class TestUsersModuleApplication {

    public static void main(String[] args) {
        SpringApplication.from(UsersModuleApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
