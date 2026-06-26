package com.service.pedidos.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErroPedidoException.class)
    public ResponseEntity<String> handleErroPedido(ErroPedidoException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Erro no pedido";
        if (message.contains("TOKEN_STALE") || message.contains("Token desatualizado")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sessão desatualizada. Faça login novamente.");
        }
        if (message.contains("Acesso negado")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
        }
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(InfraException.class)
    public ResponseEntity<String> handleInfra(InfraException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ex.getMessage() != null ? ex.getMessage() : "Erro temporário. Tente novamente.");
    }
}
