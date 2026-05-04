package com.service.pedidos.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateItemPedidoDTO(
        @NotNull Long idProduto,
        @NotNull Integer quantidadeProduto,
        @NotNull BigDecimal precoProduto
) {
}
