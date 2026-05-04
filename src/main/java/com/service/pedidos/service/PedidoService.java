package com.service.pedidos.service;

import com.service.pedidos.dto.*;
import com.service.pedidos.entities.ItemPedido;
import com.service.pedidos.entities.Pedido;
import com.service.pedidos.entities.StatusPedido;
import com.service.pedidos.exceptions.ErroPedidoException;
import com.service.pedidos.producer.PedidoProducer;
import com.service.pedidos.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        pedido.setStatusDoPedido(StatusPedido.CRIADO);
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

    public UpdatePedidoDTO atualizarStatusPedido(UpdatePedidoDTO updatePedidoDto) {
        Pedido pedido = this.pedidoRepository.findById(updatePedidoDto.id()).orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));
        pedido.setStatusDoPedido(updatePedidoDto.statusPedido());
        if(pedido.getStatusDoPedido().equals(StatusPedido.PAGO)) {
            enviarPedidoParaCozinha(new CozinhaPedidoDTO(pedido.getId(),
                    LocalDate.parse("0001-01-01"),
                    pedido.getItens()
                            .stream()
                            .map(p -> new CozinhaItemPedidoDTO(
                                    p.getIdProduto(),
                                    p.getQuantidadeProduto()))
                            .toList()));
        }
        pedidoRepository.save(pedido);
        return updatePedidoDto;
    }

    private void enviarPedidoParaCozinha(CozinhaPedidoDTO pedido) {
        pedidoProducer.enviarParaCozinha(pedido);
    }
}
