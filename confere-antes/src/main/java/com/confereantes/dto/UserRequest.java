package com.confereantes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserRequest {
    @NotBlank(message = "Nome de usuário é obrigatório")
    private String username;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    private String email;

    private String phone;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}