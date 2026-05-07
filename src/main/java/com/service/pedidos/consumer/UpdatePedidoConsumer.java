package com.service.pedidos.consumer;

import com.service.pedidos.dto.DLQSupportDTO;
import com.service.pedidos.dto.UpdatePedidoDTO;
import com.service.pedidos.exceptions.ErroPedidoException;
import com.service.pedidos.exceptions.InfraException;
import com.service.pedidos.producer.PedidoProducer;
import com.service.pedidos.service.PedidoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

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

    @RabbitListener(queues = "pedido-queue")
    public void receberAtualizacao(@Payload String updateJson) {
        try {
            UpdatePedidoDTO update = converterMensagemJSON(updateJson);
            pedidoService.atualizarStatusPedido(update);
        } catch (ErroPedidoException | InfraException e) {
            pedidoService.processarErro(e, updateJson);
        }
    }

    private UpdatePedidoDTO converterMensagemJSON(String updateJson) {
        try {
            return objectMapper.readValue(updateJson, UpdatePedidoDTO.class);
        } catch (InvalidFormatException | StreamReadException e) {
            throw new ErroPedidoException("JSON contém dados inválidos");
        }
    }
}
