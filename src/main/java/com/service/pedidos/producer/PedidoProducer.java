package com.service.pedidos.producer;

import com.service.pedidos.dto.CozinhaPedidoDTO;
import com.service.pedidos.dto.DLQSupportDTO;
import com.service.pedidos.dto.UpdatePedidoDTO;
import com.service.pedidos.dto.UpdateProdutoDTO;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class PedidoProducer {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void enviarParaCozinha(CozinhaPedidoDTO pedido) {
        amqpTemplate.convertAndSend(
                "pedido-exchange",
                "pedido-key.pago",
                objectMapper.writeValueAsString(pedido)
        );
    }

    public void enviarParaServicos(UpdatePedidoDTO updates) {
        amqpTemplate.convertAndSend(
                "pedido-exchange",
                "pedido-key.status",
                objectMapper.writeValueAsString(updates)
        );
    }

    public void atualizarQuantidadeProduto(List<UpdateProdutoDTO> updateProdutoDTO) {
        for(UpdateProdutoDTO update : updateProdutoDTO) {
            amqpTemplate.convertAndSend(
                    "pedido-exchange",
                    "pedido-key.quantity",
                    objectMapper.writeValueAsString(update)
            );
        }
    }

    public void dlqSender(DLQSupportDTO dlqSupportDTO) {
        amqpTemplate.convertAndSend(
                "dead-letter-exchange",
                "dead-message",
                objectMapper.writeValueAsString(dlqSupportDTO)
        );
    }
}
