package com.service.pedidos.consumer;

import com.service.pedidos.dto.DLQSupportDTO;
import com.service.pedidos.dto.UpdatePedidoDTO;
import com.service.pedidos.producer.PedidoProducer;
import com.service.pedidos.service.PedidoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@EnableRetry
public class UpdatePedidoConsumer {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PedidoProducer pedidoProducer;

    @RabbitListener(queues = "pedido-queue", ackMode = "MANUAL")
    public void receberAtualizacao(@Payload String updateJson) {
        UpdatePedidoDTO update = objectMapper.readValue(updateJson, UpdatePedidoDTO.class);
        if(update.statusPedido() == null) {
            DLQSupportDTO dlqSupportDTO = new DLQSupportDTO(
                    "PEDIDO_STATUS_UPDATE",
                    "pedido-queue",
                    "DATA_ERROR",
                    "Status inválido",
                    updateJson,
                    LocalDateTime.now()
            );
            pedidoProducer.dlqSender(dlqSupportDTO);
            System.out.println("sending");
            return;
        }
        pedidoService.atualizarStatusPedido(update);
    }

}
