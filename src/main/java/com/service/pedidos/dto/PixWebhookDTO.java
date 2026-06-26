package com.service.pedidos.dto;

public record PixWebhookDTO(
        Long orderId,
        String txId,
        String status,
        String gatewayPaymentId
) {
}
