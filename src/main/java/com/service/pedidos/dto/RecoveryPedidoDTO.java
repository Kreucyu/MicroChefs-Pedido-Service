package com.service.pedidos.dto;

import com.service.pedidos.entities.FormaDePagamento;
import com.service.pedidos.entities.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecoveryPedidoDTO(
        Long id,
        StatusPedido statusDoPedido,
        LocalDate dataDoPedido,
        FormaDePagamento formaDePagamento,
        List<RecoveryItemPedidoDTO>itens,
        BigDecimal valorTotal
) {

}
