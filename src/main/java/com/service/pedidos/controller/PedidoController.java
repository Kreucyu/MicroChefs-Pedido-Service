package com.service.pedidos.controller;

import com.service.pedidos.dto.CreatePedidoDTO;
import com.service.pedidos.dto.RecoveryPedidoDTO;
import com.service.pedidos.dto.UpdatePedidoDTO;
import com.service.pedidos.exceptions.ErroPedidoException;
import com.service.pedidos.producer.PedidoProducer;
import com.service.pedidos.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoProducer pedidoProducer;

    @Autowired
    private PedidoService pedidoService;

    @PostMapping("/criar")
    public ResponseEntity<CreatePedidoDTO> criarPedido(@RequestBody CreatePedidoDTO createPedidoDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(createPedidoDto));
    }

    @GetMapping("/exibir")
    public ResponseEntity<List<RecoveryPedidoDTO>> exibirTodosPedidos() {
        return ResponseEntity.status(HttpStatus.OK).body(pedidoService.exibirTodosPedidos());
    }

    @GetMapping("/exibir/{id}")
    public ResponseEntity<RecoveryPedidoDTO> exibirPedido(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(pedidoService.exibirPedidoId(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deletarPedido(@PathVariable Long id) {
        try {
            pedidoService.deletarPedidoId(id);
            return ResponseEntity.ok("Pedido deletado com sucesso");
        } catch (ErroPedidoException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/atualizar")
    public ResponseEntity<UpdatePedidoDTO> atualizarStatusPedido(@RequestBody UpdatePedidoDTO updatePedidoDto) {
        return ResponseEntity.status(HttpStatus.OK).body(pedidoService.atualizarStatusPedido(updatePedidoDto));
    }
}
