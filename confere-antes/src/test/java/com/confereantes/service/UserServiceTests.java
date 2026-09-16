package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.confereantes.dto.UserRequest;
import com.confereantes.dto.UserResponse;
import com.confereantes.exception.DuplicateUserException;
import com.confereantes.model.User;
import com.confereantes.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private UserService userService;

        @Test
        void shouldCreateUserWithEncodedPassword() {
                UserRequest request = new UserRequest();
                request.setUsername("Giovanna");
                request.setEmail("giovanna@example.com");
                request.setPhone("11999999999");
                request.setPassword("plain-password");

                when(userRepository.existsByUsername("Giovanna")).thenReturn(false);
                when(userRepository.existsByEmail("giovanna@example.com")).thenReturn(false);
                when(passwordEncoder.encode("plain-password"))
                                .thenReturn("encoded-password");

                when(userRepository.save(any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserResponse savedUser = userService.save(request);

                assertEquals("Giovanna", savedUser.getUsername());
                assertEquals("giovanna@example.com", savedUser.getEmail());

                verify(passwordEncoder).encode("plain-password");
        }

        @Test
        void shouldRejectDuplicatedUsername() {
                UserRequest request = new UserRequest();
                request.setUsername("Giovanna");
                request.setEmail("another@example.com");
                request.setPassword("plain-password");

                when(userRepository.existsByUsername("Giovanna"))
                                .thenReturn(true);

                assertThrows(DuplicateUserException.class, () -> userService.save(request));
        }

        @Test
        void shouldRejectDuplicatedEmail() {
                UserRequest request = new UserRequest();
                request.setUsername("AnotherUser");
                request.setEmail("giovanna@example.com");
                request.setPassword("plain-password");

                when(userRepository.existsByUsername("AnotherUser"))
                                .thenReturn(false);

                when(userRepository.existsByEmail("giovanna@example.com"))
                                .thenReturn(true);

                assertThrows(DuplicateUserException.class, () -> userService.save(request));

        }

}
