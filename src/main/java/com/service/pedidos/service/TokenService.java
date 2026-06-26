package com.service.pedidos.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var verificador = JWT
                    .require(algorithm)
                    .withIssuer("ClienteService")
                    .build()
                    .verify(token);
            String subject = verificador.getSubject();
            if(subject == null || subject.isEmpty()) {
                subject = verificador.getClaim("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress").asString();
            }
            return subject;
        } catch (JWTVerificationException e) {
            System.err.println("Erro ao validar token JWT: " + e.getMessage());
            return "";
        }
    }

    public String getRole(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var verificador = JWT.require(algorithm).withIssuer("ClienteService").build().verify(token);
            var roleClaim = verificador.getClaim("role");
            if (roleClaim.isMissing() || roleClaim.isNull()) {
                roleClaim = verificador.getClaim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role");
            }
            if (roleClaim.isMissing() || roleClaim.isNull()) {
                roleClaim = verificador.getClaim("Role");
            }
            if (roleClaim.isMissing() || roleClaim.isNull()) {
                return "CLIENT";
            }
            String r = roleClaim.asString();
            if (r == null) {
                try {
                    var list = roleClaim.asList(String.class);
                    if (list != null && !list.isEmpty()) {
                        r = list.get(0);
                    }
                } catch (Exception ignored) {}
            }
            return r != null ? r : "CLIENT";
        } catch (Exception e) {
            System.err.println("Erro ao obter role: " + e.getMessage());
            e.printStackTrace();
            return "CLIENT";
        }
    }

    public Long getClienteId(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var verificador = JWT.require(algorithm).withIssuer("ClienteService").build().verify(token);
            var claim = verificador.getClaim("ClienteId");
            if (claim.isMissing() || claim.isNull()) {
                claim = verificador.getClaim("clienteId");
            }
            if (claim.isMissing() || claim.isNull()) return null;
            Long valLong = claim.asLong();
            if (valLong != null) {
                return valLong;
            }
            String valStr = claim.asString();
            if (valStr != null) {
                return Long.parseLong(valStr);
            }
            return claim.asDouble().longValue();
        } catch (Exception e) {
            return null;
        }
    }
}
