package com.service.pedidos.dto;

import com.service.pedidos.entities.StatusPedido;

public record UpdatePedidoDTO(
        long id,
        StatusPedido statusPedido
) {
}
