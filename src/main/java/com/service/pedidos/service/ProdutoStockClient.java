package com.service.pedidos.service;

import com.service.pedidos.dto.CreateItemPedidoDTO;
import com.service.pedidos.exceptions.ErroPedidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ProdutoStockClient {

    private final RestClient restClient;

    public ProdutoStockClient(@Value("${produto.service.url:http://localhost:9002/produtos}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarEstoque(List<CreateItemPedidoDTO> itens) {
        for (CreateItemPedidoDTO item : itens) {
            var produto = buscarProduto(item.idProduto());
            int estoque = ((Number) produto.get("quantidadeEmEstoque")).intValue();
            if (estoque < item.quantidadeProduto()) {
                String nome = String.valueOf(produto.getOrDefault("nomeProduto", "Produto"));
                throw new ErroPedidoException(
                        "Estoque insuficiente para \"" + nome + "\". Disponível: " + estoque);
            }
        }
    }

    public void debitarEstoque(List<CreateItemPedidoDTO> itens) {
        for (CreateItemPedidoDTO item : itens) {
            restClient.post()
                    .uri("/estoque/debitar")
                    .body(Map.of("id", item.idProduto(), "quantidade", item.quantidadeProduto()))
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buscarProduto(long id) {
        ResponseEntity<Map> response = restClient.get()
                .uri("/buscar/{id}", id)
                .retrieve()
                .toEntity(Map.class);
        if (response.getBody() == null) {
            throw new ErroPedidoException("Produto não encontrado: " + id);
        }
        return response.getBody();
    }
}
