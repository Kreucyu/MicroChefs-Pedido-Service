package com.service.pedidos.dto;

import com.service.pedidos.entities.FormaDePagamento;
import com.service.pedidos.entities.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecoveryPedidoDTO(
        Long id,
        StatusPedido statusDoPedido,
        java.time.LocalDateTime dataDoPedido,
        FormaDePagamento formaDePagamento,
        List<RecoveryItemPedidoDTO> itens,
        BigDecimal valorTotal,
        Long motoboyId,
        Double distanciaKm,
        Integer etaMinutos,
        String rotaCoords,
        String pixCopiaECola,
        String pixTransactionId,
        String gatewayPaymentId,
        String paymentStatus,
        Long enderecoId,
        Double entregaLatitude,
        Double entregaLongitude,
        Double restauranteLatitude,
        Double restauranteLongitude
) {

}
