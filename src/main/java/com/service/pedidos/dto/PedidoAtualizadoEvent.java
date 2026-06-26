package com.service.pedidos.dto;

public class PedidoAtualizadoEvent {
    private final Long orderId;
    private final Object dto;

    public PedidoAtualizadoEvent(Long orderId, Object dto) {
        this.orderId = orderId;
        this.dto = dto;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Object getDto() {
        return dto;
    }
}
