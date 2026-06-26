package com.service.pedidos.dto;

import com.service.pedidos.entities.StatusPedido;

public record UpdatePedidoDTO(
        long id,
        StatusPedido statusPedido,
        Long motoboyId,
        Double distanciaKm,
        Integer etaMinutos,
        String rotaCoords,
        String pixTransactionId,
        String gatewayPaymentId,
        String paymentStatus
) {
}
