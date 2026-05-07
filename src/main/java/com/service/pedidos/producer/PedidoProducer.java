package com.service.pedidos.producer;

import com.service.pedidos.dto.CozinhaPedidoDTO;
import com.service.pedidos.dto.DLQSupportDTO;
import com.service.pedidos.dto.UpdatePedidoDTO;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

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

    public void enviarParaServicos(UpdatePedidoDTO updatePedidoDTO) {
        amqpTemplate.convertAndSend(
                "pedido-exchange",
                "pedido-key.status",
                objectMapper.writeValueAsString(updatePedidoDTO)
        );
    }

    public void dlqSender(DLQSupportDTO dlqSupportDTO) {
        amqpTemplate.convertAndSend(
                "dead-letter-exchange",
                "dead-message",
                objectMapper.writeValueAsString(dlqSupportDTO)
        );
    }
}
