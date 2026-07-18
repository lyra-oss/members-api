package edu.lyra.members.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Lyra Members API service.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@SpringBootApplication
public class MembersApiApplication {

    static void main(final String[] args) {
        SpringApplication.run(MembersApiApplication.class, args);
    }

}
