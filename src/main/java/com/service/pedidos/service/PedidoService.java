package com.service.pedidos.service;



import com.service.pedidos.dto.*;

import com.service.pedidos.entities.FormaDePagamento;

import com.service.pedidos.entities.ItemPedido;

import com.service.pedidos.entities.Pedido;

import com.service.pedidos.entities.StatusPedido;

import com.service.pedidos.exceptions.ErroPedidoException;

import com.service.pedidos.exceptions.InfraException;

import com.service.pedidos.producer.PedidoProducer;

import com.service.pedidos.repository.PedidoRepository;

import org.hibernate.QueryTimeoutException;

import org.springframework.amqp.AmqpException;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cache.CacheManager;

import org.springframework.cache.annotation.CacheEvict;

import org.springframework.cache.annotation.Cacheable;

import org.springframework.cache.annotation.Caching;

import org.springframework.dao.TransientDataAccessException;

import org.springframework.retry.annotation.Backoff;

import org.springframework.retry.annotation.Retryable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.CannotCreateTransactionException;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestClientException;



import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.List;



@Service

public class PedidoService {



    @Autowired

    private PedidoRepository pedidoRepository;



    @Autowired

    private PedidoProducer pedidoProducer;



    @Autowired

    private ProdutoStockClient produtoStockClient;



    @Autowired

    private org.springframework.context.ApplicationEventPublisher eventPublisher;



    @Autowired

    CacheManager cacheManager;



    public PedidoService(PedidoRepository pedidoRepository) {

        this.pedidoRepository = pedidoRepository;

    }



    @CacheEvict(value = "pedidos", allEntries = true)

    public RecoveryPedidoDTO criarPedido(CreatePedidoDTO createPedidoDto) {

        validarAcessoCliente(createPedidoDto.clienteId());



        if (createPedidoDto.itens() == null || createPedidoDto.itens().isEmpty()) {

            throw new ErroPedidoException("O pedido deve conter pelo menos um item");

        }



        try {

            produtoStockClient.validarEstoque(createPedidoDto.itens());

        } catch (RestClientException e) {

            throw new ErroPedidoException("Não foi possível validar o estoque. Tente novamente.");

        }



        Pedido pedido = new Pedido();

        pedido.setClienteId(createPedidoDto.clienteId());

        pedido.setEnderecoId(createPedidoDto.enderecoId());

        pedido.setEntregaLatitude(createPedidoDto.entregaLatitude());

        pedido.setEntregaLongitude(createPedidoDto.entregaLongitude());

        pedido.setRestauranteLatitude(createPedidoDto.restauranteLatitude());

        pedido.setRestauranteLongitude(createPedidoDto.restauranteLongitude());

        pedido.setDataDoPedido(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")));

        if (createPedidoDto.distanciaKm() != null) {
            pedido.setDistanciaKm(createPedidoDto.distanciaKm());
        } else if (createPedidoDto.entregaLatitude() != null && createPedidoDto.restauranteLatitude() != null) {
            double latDiff = createPedidoDto.entregaLatitude() - createPedidoDto.restauranteLatitude();
            double lngDiff = createPedidoDto.entregaLongitude() - createPedidoDto.restauranteLongitude();
            double distance = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111.0;
            pedido.setDistanciaKm(Math.round(distance * 10.0) / 10.0);
        }

        if (createPedidoDto.etaMinutos() != null) {
            pedido.setEtaMinutos(createPedidoDto.etaMinutos());
        } else if (pedido.getDistanciaKm() != null) {
            pedido.setEtaMinutos((int) Math.round(pedido.getDistanciaKm() * 4.0) + 10);
        }

        pedido.setStatusDoPedido(StatusPedido.AGUARDANDO_PAGAMENTO);

        pedido.setFormaDePagamento(createPedidoDto.formaDePagamento());



        for (CreateItemPedidoDTO itensDto : createPedidoDto.itens()) {

            ItemPedido itemPedido = new ItemPedido(

                    itensDto.quantidadeProduto(),

                    itensDto.precoProduto(),

                    itensDto.idProduto(),

                    pedido

            );

            pedido.adicionarItem(itemPedido);

        }

        pedidoRepository.save(pedido);



        if (createPedidoDto.formaDePagamento().equals(FormaDePagamento.DINHEIRO)
                || createPedidoDto.formaDePagamento().equals(FormaDePagamento.CARTAO_DE_CREDITO)
                || createPedidoDto.formaDePagamento().equals(FormaDePagamento.CARTAO_DE_DEBITO)) {

            debitarEstoquePedido(pedido);

            atualizarStatusPedido(new UpdatePedidoDTO(pedido.getId(), StatusPedido.PAGO, null, null, null, null, null, null, null));

        } else if (createPedidoDto.formaDePagamento().equals(FormaDePagamento.PIX)) {

            String txId = String.format("MC%08d", pedido.getId());

            String pixCode = PixUtils.gerarPixCopiaECola(pedido.getId(), pedido.getValorTotal());

            pedido.setPixCopiaECola(pixCode);

            pedido.setPixTransactionId(txId);

            pedido.setPaymentStatus("PENDENTE");

            pedidoRepository.save(pedido);

        }



        return exibirPedidoId(pedido.getId());

    }



    @Transactional

    public void confirmarPagamentoViaWebhook(PixWebhookDTO webhook) {

        if (webhook.orderId() == null) {

            throw new ErroPedidoException("orderId é obrigatório");

        }

        if (!"CONFIRMED".equalsIgnoreCase(webhook.status()) && !"PAGO".equalsIgnoreCase(webhook.status())) {

            throw new ErroPedidoException("Status de pagamento inválido");

        }



        Pedido pedido = pedidoRepository.findById(webhook.orderId())

                .orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));



        if (pedido.getStatusDoPedido() == StatusPedido.PAGO) {

            return;

        }



        if (webhook.txId() != null && pedido.getPixTransactionId() != null

                && !webhook.txId().equalsIgnoreCase(pedido.getPixTransactionId())) {

            throw new ErroPedidoException("Transação PIX não corresponde ao pedido");

        }



        pedido.setPaymentStatus("CONFIRMED");

        if (webhook.gatewayPaymentId() != null) {

            pedido.setGatewayPaymentId(webhook.gatewayPaymentId());

        }

        pedidoRepository.save(pedido);



        debitarEstoquePedido(pedido);

        confirmarPagamentoPix(pedido.getId());

    }

    @Transactional
    public void simularPagamentoPix(Long orderId) {
        Pedido pedido = pedidoRepository.findById(orderId)
                .orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));

        if (pedido.getStatusDoPedido() == StatusPedido.PAGO) {
            return;
        }

        pedido.setPaymentStatus("CONFIRMED");
        pedidoRepository.save(pedido);

        debitarEstoquePedido(pedido);
        confirmarPagamentoPix(pedido.getId());
    }



    @Cacheable(value = "pedido", key = "#id")

    public RecoveryPedidoDTO exibirPedidoId(Long id) {

        Pedido pedido = this.pedidoRepository.findById(id)

                .orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));

        validarAcessoPedido(pedido);

        return toDto(pedido);

    }



    @Cacheable(value = "pedidos")

    public List<RecoveryPedidoDTO> exibirTodosPedidos() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        List<Pedido> pedidos;
        if (authentication != null && authentication.getPrincipal() instanceof com.service.pedidos.security.UserPrincipal principal) {
            System.out.println("DEBUG: Principal Role = " + principal.getRole() + ", ClienteId = " + principal.getClienteId());
            if ("CLIENT".equalsIgnoreCase(principal.getRole())) {
                pedidos = this.pedidoRepository.findByClienteId(principal.getClienteId());
            } else {
                pedidos = this.pedidoRepository.findAll();
            }
        } else {
            System.out.println("DEBUG: Principal not found in authentication");
            pedidos = this.pedidoRepository.findAll();
        }
        return pedidos.stream().map(this::toDto).toList();
    }



    @Transactional

    public void confirmarPagamentoPix(Long orderId) {

        Pedido pedido = this.pedidoRepository.findById(orderId).orElse(null);

        if (pedido != null && pedido.getStatusDoPedido() == StatusPedido.AGUARDANDO_PAGAMENTO) {

            pedido.setStatusDoPedido(StatusPedido.PAGO);

            pedido.setPaymentStatus("CONFIRMED");

            pedidoRepository.save(pedido);



            UpdatePedidoDTO updateDto = new UpdatePedidoDTO(

                    pedido.getId(),

                    StatusPedido.PAGO,

                    pedido.getMotoboyId(),

                    pedido.getDistanciaKm(),

                    pedido.getEtaMinutos(),

                    pedido.getRotaCoords(),

                    pedido.getPixTransactionId(),

                    pedido.getGatewayPaymentId(),

                    pedido.getPaymentStatus()

            );



            enviarPedidoParaCozinha(new CozinhaPedidoDTO(pedido.getId(),

                    pedido.getDataDoPedido(),

                    pedido.getItens()

                            .stream()

                            .map(p -> new CozinhaItemPedidoDTO(

                                    p.getIdProduto(),

                                    p.getQuantidadeProduto()))

                            .toList()));

            pedidoProducer.enviarParaServicos(updateDto);



            // Invalidar caches após pagamento confirmado
            invalidarCachesPedido(pedido.getId());

            RecoveryPedidoDTO recoveryDto = toDto(pedido);

            eventPublisher.publishEvent(new PedidoAtualizadoEvent(pedido.getId(), recoveryDto));

        }

    }



    @Caching(evict = {
        @CacheEvict(value = "pedido", key = "#id"),
        @CacheEvict(value = "pedidos", allEntries = true)
    })
    public void deletarPedidoId(Long id) {

        Pedido pedido = this.pedidoRepository.findById(id)

                .orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));

        this.pedidoRepository.delete(pedido);

    }



    @Transactional

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 5000), retryFor = {InfraException.class})

    public UpdatePedidoDTO atualizarStatusPedido(UpdatePedidoDTO updatePedidoDto) {

        try {

            Pedido pedido = this.pedidoRepository.findById(updatePedidoDto.id())

                    .orElseThrow(() -> new ErroPedidoException("Pedido não encontrado"));



            boolean statusMudou = false;

            if (updatePedidoDto.statusPedido() != null

                    && !updatePedidoDto.statusPedido().equals(pedido.getStatusDoPedido())) {

                if (pedido.getStatusDoPedido() == StatusPedido.PAGO

                        && updatePedidoDto.statusPedido() == StatusPedido.PAGO) {

                    return updatePedidoDto;

                }

                if (!pedido.getStatusDoPedido().proximosEstados().contains(updatePedidoDto.statusPedido())) {

                    throw new ErroPedidoException("Status inválido");

                }

                pedido.setStatusDoPedido(updatePedidoDto.statusPedido());

                statusMudou = true;

            }



            if (updatePedidoDto.motoboyId() != null) {

                pedido.setMotoboyId(updatePedidoDto.motoboyId());

            }

            if (updatePedidoDto.distanciaKm() != null) {

                pedido.setDistanciaKm(updatePedidoDto.distanciaKm());

            }

            if (updatePedidoDto.etaMinutos() != null) {

                pedido.setEtaMinutos(updatePedidoDto.etaMinutos());

            }

            if (updatePedidoDto.rotaCoords() != null) {

                pedido.setRotaCoords(updatePedidoDto.rotaCoords());

            }

            if (updatePedidoDto.pixTransactionId() != null) {

                pedido.setPixTransactionId(updatePedidoDto.pixTransactionId());

            }

            if (updatePedidoDto.gatewayPaymentId() != null) {

                pedido.setGatewayPaymentId(updatePedidoDto.gatewayPaymentId());

            }

            if (updatePedidoDto.paymentStatus() != null) {

                pedido.setPaymentStatus(updatePedidoDto.paymentStatus());

            }



            if (statusMudou && pedido.getStatusDoPedido().equals(StatusPedido.PAGO)) {

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



            // Atualizar cache do pedido individual e invalidar lista
            RecoveryPedidoDTO pedidoAtualizado = toDto(pedido);
            var cachePedido = cacheManager.getCache("pedido");
            if (cachePedido != null) {
                cachePedido.put(updatePedidoDto.id(), pedidoAtualizado);
            }
            var cacheLista = cacheManager.getCache("pedidos");
            if (cacheLista != null) {
                cacheLista.clear();
            }

            eventPublisher.publishEvent(new PedidoAtualizadoEvent(pedido.getId(), pedidoAtualizado));



            return updatePedidoDto;

        } catch (CannotCreateTransactionException | QueryTimeoutException | TransientDataAccessException | AmqpException e) {

            throw new InfraException("Erro na conexão");

        }

    }



    private void debitarEstoquePedido(Pedido pedido) {

        try {

            produtoStockClient.debitarEstoque(pedido.getItens().stream()

                    .map(i -> new CreateItemPedidoDTO(i.getIdProduto(), i.getQuantidadeProduto(), i.getPrecoProduto()))

                    .toList());

        } catch (RestClientException e) {

            throw new ErroPedidoException("Erro ao atualizar estoque: " + e.getMessage());

        }

    }



    private RecoveryPedidoDTO toDto(Pedido pedido) {

        return new RecoveryPedidoDTO(

                pedido.getId(),

                pedido.getStatusDoPedido(),

                pedido.getDataDoPedido(),

                pedido.getFormaDePagamento(),

                pedido.getItens().stream().map(u -> new RecoveryItemPedidoDTO(

                        u.getIdProduto(),

                        u.getQuantidadeProduto(),

                        u.getPrecoProduto())).toList(),

                pedido.getValorTotal(),

                pedido.getMotoboyId(),

                pedido.getDistanciaKm(),

                pedido.getEtaMinutos(),

                pedido.getRotaCoords(),

                pedido.getPixCopiaECola(),

                pedido.getPixTransactionId(),

                pedido.getGatewayPaymentId(),

                pedido.getPaymentStatus(),

                pedido.getEnderecoId(),

                pedido.getEntregaLatitude(),

                pedido.getEntregaLongitude(),

                pedido.getRestauranteLatitude(),

                pedido.getRestauranteLongitude()

        );

    }



    private void validarAcessoCliente(Long clienteId) {

        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof com.service.pedidos.security.UserPrincipal principal) {

            if ("CLIENT".equalsIgnoreCase(principal.getRole())) {

                if (principal.getClienteId() == null) {

                    throw new ErroPedidoException("Token desatualizado. Faça login novamente.");

                }

                if (!principal.getClienteId().equals(clienteId)) {

                    throw new ErroPedidoException("Acesso negado: ID do cliente inválido");

                }

            }

        }

    }



    private void validarAcessoPedido(Pedido pedido) {

        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof com.service.pedidos.security.UserPrincipal principal) {

            if ("CLIENT".equalsIgnoreCase(principal.getRole())) {

                if (principal.getClienteId() == null || !principal.getClienteId().equals(pedido.getClienteId())) {

                    throw new ErroPedidoException("Acesso negado: você não tem permissão para visualizar este pedido");

                }

            }

        }

    }



    private void enviarPedidoParaCozinha(CozinhaPedidoDTO pedido) {

        pedidoProducer.enviarParaCozinha(pedido);

    }



    private void invalidarCachesPedido(Long pedidoId) {
        var cachePedido = cacheManager.getCache("pedido");
        if (cachePedido != null) {
            cachePedido.evict(pedidoId);
        }
        var cacheLista = cacheManager.getCache("pedidos");
        if (cacheLista != null) {
            cacheLista.clear();
        }
    }



    public void processarErro(Exception e, String JSON) {

        String tipo = "DATA_ERROR";

        if (e instanceof InfraException) {

            tipo = "INFRA_ERROR";

        }

        DLQSupportDTO dlqSupportDTO = new DLQSupportDTO(

                "PEDIDO_STATUS_UPDATE",

                "pedido-queue",

                tipo,

                e.getMessage(),

                JSON,

                LocalDateTime.now()

        );

        pedidoProducer.dlqSender(dlqSupportDTO);

        System.out.println(dlqSupportDTO);

    }

}


