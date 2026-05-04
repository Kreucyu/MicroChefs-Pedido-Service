package com.service.pedidos.dto;


import java.math.BigDecimal;

public record RecoveryItemPedidoDTO(
         Long idProduto,
         Integer quantidadeProduto,
         BigDecimal precoProduto
) {

}
