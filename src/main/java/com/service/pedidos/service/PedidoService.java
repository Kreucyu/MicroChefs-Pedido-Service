package com.service.pedidos.service;

import com.service.pedidos.dto.*;
import com.service.pedidos.entities.ItemPedido;
import com.service.pedidos.entities.Pedido;
import com.service.pedidos.entities.StatusPedido;
import com.service.pedidos.exceptions.ErroPedidoException;
import com.service.pedidos.producer.PedidoProducer;
import com.service.pedidos.repository.PedidoRepository;
import org.hibernate.QueryTimeoutException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoProducer pedidoProducer;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public CreatePedidoDTO criarPedido(CreatePedidoDTO createPedidoDto) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(createPedidoDto.clienteId());
        pedido.setDataDoPedido(LocalDate.now());
        pedido.setStatusDoPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setFormaDePagamento(createPedidoDto.formaDePagamento());

        for(CreateItemPedidoDTO itensDto : createPedidoDto.itens()) {
            ItemPedido itemPedido = new ItemPedido(
                    itensDto.quantidadeProduto(),
                    itensDto.precoProduto(),
                    itensDto.idProduto(),
                    pedido
            );
            pedido.adicionarItem(itemPedido);
        }
        pedidoRepository.save(pedido);
        return createPedidoDto;
    }

    public RecoveryPedidoDTO exibirPedidoId(Long id) {
        Pedido pedido = this.pedidoRepository.findById(id).orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));
        return new RecoveryPedidoDTO(pedido.getId(),
                pedido.getStatusDoPedido(),
                pedido.getDataDoPedido(),
                pedido.getFormaDePagamento(),
                pedido.getItens().stream().map(u -> new RecoveryItemPedidoDTO(
                        u.getIdProduto(),
                        u.getQuantidadeProduto(),
                        u.getPrecoProduto())).toList(),
                pedido.getValorTotal());
    }

    public List<RecoveryPedidoDTO> exibirTodosPedidos() {
        List<Pedido> pedidos = this.pedidoRepository.findAll();
        return pedidos.stream().map(pedido -> new RecoveryPedidoDTO(pedido.getId(),
                pedido.getStatusDoPedido(),
                pedido.getDataDoPedido(),
                pedido.getFormaDePagamento(),
                pedido.getItens().stream().map(u -> new RecoveryItemPedidoDTO(
                        u.getIdProduto(),
                        u.getQuantidadeProduto(),
                        u.getPrecoProduto())).toList(),
                pedido.getValorTotal())).toList();
    }

    public void deletarPedidoId(Long id) {
        Pedido pedido = this.pedidoRepository.findById(id).orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));
        this.pedidoRepository.delete(pedido);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 5000), value = {
            ErroPedidoException.class
    })
    public UpdatePedidoDTO atualizarStatusPedido(UpdatePedidoDTO updatePedidoDto) {
        try {
            Pedido pedido = this.pedidoRepository.findById(updatePedidoDto.id()).orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));
            if (!pedido.getStatusDoPedido().proximosEstados().contains(updatePedidoDto.statusPedido())
                    && updatePedidoDto.statusPedido() != null) {
                throw new ErroPedidoException("Status inválido");
            }
            pedido.setStatusDoPedido(updatePedidoDto.statusPedido());

            if(pedido.getStatusDoPedido().equals(StatusPedido.PAGO)) {
                enviarPedidoParaCozinha(new CozinhaPedidoDTO(pedido.getId(),
                        pedido.getDataDoPedido(),
                        pedido.getItens()
                                .stream()
                                .map(p -> new CozinhaItemPedidoDTO(
                                        p.getIdProduto(),
                                        p.getQuantidadeProduto()))
                                .toList()));
            }

            pedidoRepository.save(pedido);
            pedidoProducer.enviarParaServicos(updatePedidoDto);
            return updatePedidoDto;
        } catch (CannotCreateTransactionException | QueryTimeoutException | TransientDataAccessException | AmqpException | AmqpTimeoutException | AmqpConnectException e)

    }

    private void enviarPedidoParaCozinha(CozinhaPedidoDTO pedido) {
        pedidoProducer.enviarParaCozinha(pedido);
    }

    public void processarErro(Exception e, String json) {
        DLQSupportDTO dlqSupportDTO = new DLQSupportDTO(
                "PEDIDO_STATUS_UPDATE",
                "pedido-queue",
                "DATA_ERROR",
                e.getMessage(),
                json,
                LocalDateTime.now()
        );

        System.out.println(dlqSupportDTO);
    }
}
