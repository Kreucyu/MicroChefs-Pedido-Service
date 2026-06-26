package com.service.pedidos.controller;

import com.service.pedidos.dto.CreatePedidoDTO;
import com.service.pedidos.dto.RecoveryPedidoDTO;
import com.service.pedidos.dto.UpdatePedidoDTO;
import com.service.pedidos.dto.PedidoAtualizadoEvent;
import com.service.pedidos.exceptions.ErroPedidoException;
import com.service.pedidos.producer.PedidoProducer;
import com.service.pedidos.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Autowired
    private PedidoProducer pedidoProducer;

    @Autowired
    private PedidoService pedidoService;

    @Value("${pix.webhook.secret:CHAVE_WEBHOOK_PIX_MICROCHEFS}")
    private String pixWebhookSecret;

    @GetMapping(value = "/{id}/fluxo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter assinarPedido(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 mins
        emitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> {
            List<SseEmitter> list = emitters.get(id);
            if (list != null) list.remove(emitter);
        });
        emitter.onTimeout(() -> {
            List<SseEmitter> list = emitters.get(id);
            if (list != null) list.remove(emitter);
        });
        emitter.onError((e) -> {
            List<SseEmitter> list = emitters.get(id);
            if (list != null) list.remove(emitter);
        });

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Conectado ao fluxo do pedido: " + id));
        } catch (IOException e) {
            List<SseEmitter> list = emitters.get(id);
            if (list != null) list.remove(emitter);
        }

        return emitter;
    }

    @EventListener
    public void notificarClientesPedido(PedidoAtualizadoEvent event) {
        List<SseEmitter> list = emitters.get(event.getOrderId());
        if (list != null && !list.isEmpty()) {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("pedido-atualizacao").data(event.getDto()));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            list.removeAll(deadEmitters);
        }
    }

    @PostMapping("/criar")
    public ResponseEntity<RecoveryPedidoDTO> criarPedido(@RequestBody CreatePedidoDTO createPedidoDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(createPedidoDto));
    }

    @PostMapping("/pix/webhook")
    public ResponseEntity<String> confirmarPixWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody com.service.pedidos.dto.PixWebhookDTO webhook) {
        if (secret == null || !secret.equals(pixWebhookSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Webhook não autorizado");
        }
        try {
            pedidoService.confirmarPagamentoViaWebhook(webhook);
            return ResponseEntity.ok("Pagamento confirmado");
        } catch (ErroPedidoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

    @PostMapping("/pix/simular/{id}")
    public ResponseEntity<String> simularPagamentoPix(@PathVariable Long id) {
        try {
            pedidoService.simularPagamentoPix(id);
            return ResponseEntity.ok("Pagamento simulado com sucesso");
        } catch (ErroPedidoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
