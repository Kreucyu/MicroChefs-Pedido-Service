package com.service.pedidos.dto;

import com.service.pedidos.entities.FormaDePagamento;

import java.util.List;

public record CreatePedidoDTO(
        Long clienteId,
        FormaDePagamento formaDePagamento,
        List<CreateItemPedidoDTO> itens
) {

}
