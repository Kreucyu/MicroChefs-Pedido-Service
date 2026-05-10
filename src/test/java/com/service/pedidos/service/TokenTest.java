package com.service.pedidos.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TokenTest {

    @Autowired
    private TokenService tokenService;

    @Test
    public void testToken() {
        // Generate a token exactly how .NET would generate it, using Auth0 JWT to simulate it.
        // Actually .NET uses System.IdentityModel.Tokens.Jwt
        String token = JWT.create()
                .withIssuer("ClienteService")
                .withAudience("ClienteServiceUsers")
                .withClaim("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier", "1")
                .withClaim("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress", "test@test.com")
                .sign(Algorithm.HMAC256("CHAVE_SUPER_SECRETA_BPUI_123456789"));

        System.out.println("Generated token: " + token);

        String subject = tokenService.validateToken(token);
        System.out.println("Subject from tokenService: '" + subject + "'");
        
        // Also let's print the exception if it fails
        try {
            Algorithm algorithm = Algorithm.HMAC256("CHAVE_SUPER_SECRETA_BPUI_123456789");
            var verificador = JWT.require(algorithm).withIssuer("ClienteService").build().verify(token);
            System.out.println("Subject direct: " + verificador.getSubject());
            System.out.println("Email claim: " + verificador.getClaim("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress").asString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
