package com.confereantes.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.confereantes.config.JwtConfig;
import com.confereantes.config.SecurityConfig;
import com.confereantes.service.UserService;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtConfig.class })
@TestPropertySource(properties = { "jwt.secret=test-only-jwt-secret-not-for-production-2026" })
public class JwtSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldRejectInvalidJwt() throws Exception {
        mockMvc.perform(
                get("/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void shouldRejectExpiredJwt() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("confere-antes")
                .subject("giovanna@example.com")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .build();

        String expiredToken = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                claims))
                .getTokenValue();

        mockMvc.perform(
                get("/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void shouldRejectJwtWithInvalidIssuer() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("outro-servico")
                .subject("giovanna@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        String tokenWithInvalidIssuer = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();

        mockMvc.perform(
                get("/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenWithInvalidIssuer)

        ).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTamperedJwt() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("confere-antes")
                .subject("giovanna@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        String validToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();

        String[] tokenParts = validToken.split("\\.");

        assertEquals(3, tokenParts.length);

        String header = tokenParts[0];
        String payload = tokenParts[1];
        String signature = tokenParts[2];

        char originalCharacter = signature.charAt(0);
        char replacementCharacter = originalCharacter == 'a' ? 'b' : 'a';

        String tamperedSignature = replacementCharacter + signature.substring(1);

        String tamperedToken = header + "." + payload + "." + tamperedSignature;

        mockMvc.perform(
                get("/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowProtectedEndpointWithRealValidJwt() throws Exception {
        when(userService.findAll())
                .thenReturn(List.of());

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("confere-antes")
                .subject("giovanna@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        mockMvc.perform(
                get("/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token))
                .andExpect(status().isOk());
    }
}
