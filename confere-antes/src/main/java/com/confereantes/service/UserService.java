package com.confereantes.service;

import java.util.List;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.confereantes.model.User;
import com.confereantes.repository.UserRepository;
import com.confereantes.dto.UserRequest;
import com.confereantes.dto.UserResponse;
import com.confereantes.exception.DuplicateUserException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());

    }

    public UserResponse save(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Nome de usuário já cadastrado");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("E-mail já cadastrado");
        }
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser);
    }

}
