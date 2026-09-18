package com.confereantes.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;


import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration 
public class JwtConfig {

    private SecretKey secretKey(String secret) {
        return new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${jwt.secret}") String secret) {

        SecretKey key = secretKey(secret);

        return new NimbusJwtEncoder(new ImmutableSecret<>(key));

    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {

        SecretKey key = secretKey(secret);

        return NimbusJwtDecoder
                .withSecretKey(key)
                .build();
    }
    
}
