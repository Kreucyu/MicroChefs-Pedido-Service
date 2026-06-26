package com.service.pedidos.security;

public class UserPrincipal {
    private final String userId;
    private final String role;
    private final Long clienteId;

    public UserPrincipal(String userId, String role, Long clienteId) {
        this.userId = userId;
        this.role = role;
        this.clienteId = clienteId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public Long getClienteId() {
        return clienteId;
    }
}
