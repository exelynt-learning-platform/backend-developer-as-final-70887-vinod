package com.example.booking.config;

import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seed(UserRepository users, ResourceRepository resources, PasswordEncoder encoder) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                users.save(admin);
            }
            if (users.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(encoder.encode("User@123"));
                user.setRole(Role.USER);
                users.save(user);
            }
            if (resources.count() == 0) {
                Resource room = new Resource();
                room.setName("Conference Room A");
                room.setDescription("10-seat conference room with projector");
                room.setType("ROOM");
                room.setPrice(new BigDecimal("500.00"));
                room.setAvailable(true);
                resources.save(room);

                Resource vehicle = new Resource();
                vehicle.setName("Company Car");
                vehicle.setDescription("Sedan available for business travel");
                vehicle.setType("VEHICLE");
                vehicle.setPrice(new BigDecimal("1500.00"));
                vehicle.setAvailable(true);
                resources.save(vehicle);
            }
        };
    }
}
