package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTests {

        @Mock
        private JwtEncoder jwtEncoder;

        @Test
        void shouldGenerateJwtToken() {
                JwtEncoder jwtEncoder = this.jwtEncoder;

                Jwt fakeJwt = Jwt.withTokenValue("fake-jwt-token")
                                .header("alg", "HS256")
                                .claim("iss", "confere-antes")
                                .build();

                when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                                .thenReturn(fakeJwt);

                JwtService jwtService = new JwtService(jwtEncoder, 3600L);

                String token = jwtService.generateToken("giovanna@example.com");

                assertEquals("fake-jwt-token", token);
        }

        @Test
        void shouldCreateTokenWithExpectedClaims() {
                Jwt fakeJwt = Jwt.withTokenValue("fake-jwt-token")
                                .header("alg", "HS256")
                                .claim("iss", "confere-antes")
                                .build();

                when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                                .thenReturn(fakeJwt);

                JwtService jwtService = new JwtService(jwtEncoder, 3600L);

                jwtService.generateToken("giovanna@example.com");

                ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

                verify(jwtEncoder).encode(captor.capture());

                JwtClaimsSet claims = captor.getValue().getClaims();

                assertEquals("confere-antes", claims.getClaimAsString("iss"));
                assertEquals("giovanna@example.com", claims.getSubject());
                assertNotNull(claims.getIssuedAt());
                assertNotNull(claims.getExpiresAt());

        }

        @Test
        void shouldUseConfiguredExpirationTime() {
                Jwt fakeJwt = Jwt.withTokenValue("fake-jwt-token")
                                .header("alg", "HS256")
                                .claim("iss", "confere-antes")
                                .build();

                when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                                .thenReturn(fakeJwt);

                JwtService jwtService = new JwtService(jwtEncoder, 60L);

                jwtService.generateToken("giovanna@example.com");

                ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

                verify(jwtEncoder).encode(captor.capture());

                JwtClaimsSet claims = captor.getValue().getClaims();

                long expirationSeconds = Duration.between(
                                claims.getIssuedAt(),
                                claims.getExpiresAt()

                ).getSeconds();

                assertEquals(60L, expirationSeconds);
        }

}
