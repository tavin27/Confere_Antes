package com.confereantes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.confereantes.model.User;
import com.confereantes.repository.UserRepository;
import com.confereantes.dto.UserRequest;


@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(UserRequest request) {
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCpf(request.getCpf());

        return userRepository.save(user);
    }

}
