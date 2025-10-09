package com.sagittec.lyra.members.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
class MembersApiApplication {

    private MembersApiApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(MembersApiApplication.class, args);
    }

}
