package com.service.pedidos.producer;

import com.service.pedidos.dto.CozinhaPedidoDTO;
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
}
