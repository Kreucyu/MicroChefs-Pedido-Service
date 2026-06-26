package com.service.pedidos.dto;

import com.service.pedidos.entities.FormaDePagamento;

import java.util.List;

public record CreatePedidoDTO(
        Long clienteId,
        Long enderecoId,
        FormaDePagamento formaDePagamento,
        List<CreateItemPedidoDTO> itens,
        Double entregaLatitude,
        Double entregaLongitude,
        Double restauranteLatitude,
        Double restauranteLongitude,
        Double distanciaKm,
        Integer etaMinutos
) {
    public CreatePedidoDTO(Long clienteId, FormaDePagamento formaDePagamento, List<CreateItemPedidoDTO> itens) {
        this(clienteId, null, formaDePagamento, itens, null, null, null, null, null, null);
    }
}

