package com.example.testapp.controller;

import com.example.testapp.model.Address;
import com.example.testapp.model.User;
import com.example.testapp.repository.AddressRepository;
import com.example.testapp.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public UserController(UserRepository userRepository, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < 25; i++) {
            User user = new User();
            user.setName("User " + i);
            userRepository.save(user);

            Address address = new Address();
            address.setCity("City " + i);
            address.setUser(user);
            addressRepository.save(address);
        }
    }

    // This endpoint triggers N+1 because fetch type is EAGER or if we access lazily in a loop
    @GetMapping("/users")
    public List<String> getUsers() {
        List<User> users = userRepository.findAll();
        List<String> names = new ArrayList<>();
        // Trigger lazy loading if it was lazy, but with EAGER it happens at findAll
        for (User user : users) {
             names.add(user.getName() + " - " + user.getAddresses().size() + " addresses");
        }
        return names;
    }
}
