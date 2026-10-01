package com.confereantes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.confereantes.config.SecurityConfig;
import com.confereantes.dto.LoginRequest;
import com.confereantes.dto.LoginResponse;
import com.confereantes.dto.UserRequest;
import com.confereantes.dto.UserResponse;
import com.confereantes.exception.InvalidCredentialsException;
import com.confereantes.service.UserService;

import com.confereantes.config.JwtAuthenticationEntryPoint;
import com.confereantes.config.JwtConfig;

@WebMvcTest({
    UserController.class,
    AuthController.class
})
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    JwtAuthenticationEntryPoint.class
})
public class SecurityControllerTests {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private JwtDecoder jwtDecoder;

        @Test
        void shouldRejectProtectedEndpointWithoutToken() throws Exception {
                mockMvc.perform(get("/users"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldAllowProtectedEndpointWithValidToken() throws Exception {
                when(userService.findAll())
                                .thenReturn(List.<UserResponse>of());

                mockMvc.perform(
                                get("/users")
                                                .with(
                                                                SecurityMockMvcRequestPostProcessors.jwt()))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldAllowLoginWithoutToken() throws Exception {
                when(userService.login(any(LoginRequest.class)))
                                .thenReturn(
                                                new LoginResponse(
                                                                "fake-jwt-token",
                                                                "Bearer"));

                mockMvc.perform(
                                post("/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "email": "giovanna@example.com",
                                                                    "password": "SenhaSegura123"
                                                                }
                                                                """))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldAllowUserRegistrationWithoutToken() throws Exception {
                when(userService.save(any(UserRequest.class)))
                                .thenReturn(null);

                mockMvc.perform(
                                post("/users")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "username": "Camila",
                                                                    "email": "camila@example.com",
                                                                    "phone": "11944444444",
                                                                    "password": "SenhaSegura123"
                                                                }
                                                                """))
                                .andExpect(status().isCreated());
        }

        @Test
        void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
                when(userService.login(any(LoginRequest.class)))
                                .thenThrow(
                                                new InvalidCredentialsException("Credenciais inválidas"));
                mockMvc.perform(
                                post("/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                        "email": "giovanna@example.com",
                                                                        "password": "SenhaErrada123"
                                                                }

                                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message")
                                                .value("Credenciais inválidas"));
        }

        @Test
        void shouldReturnSameUnauthorizedResponseForUnknownEmail()
                        throws Exception {

                when(userService.login(any(LoginRequest.class)))
                                .thenThrow(
                                                new InvalidCredentialsException("Credenciais inválidas"));

                mockMvc.perform(
                                post("/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                        "email": "unknown@example.com",
                                                                        "password": "SenhaSegura123"
                                                                }
                                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message")
                                                .value("Credenciais inválidas"));

        }
}
