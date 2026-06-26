package com.service.pedidos.entities;

import java.util.List;

public enum StatusPedido {

    AGUARDANDO_PAGAMENTO {
        public List<StatusPedido> proximosEstados() {
            return List.of(StatusPedido.CANCELADO, StatusPedido.PAGO);
        }
    },
    CANCELADO {
        public List<StatusPedido> proximosEstados() {
            return List.of();
        }
    },
    PAGO {
        public List<StatusPedido> proximosEstados() {
            return List.of(StatusPedido.EM_PREPARO);
        }
    },
    EM_PREPARO {
        public List<StatusPedido> proximosEstados() {
            return List.of(StatusPedido.PRONTO);
        }
    },
    PRONTO {
        public List<StatusPedido> proximosEstados() {
            return List.of(StatusPedido.AGUARDANDO_ENTREGADOR);
        }
    },
    AGUARDANDO_ENTREGADOR {
        public List<StatusPedido> proximosEstados() {
            return List.of(StatusPedido.EM_ENTREGA);
        }
    },
    EM_ENTREGA {
        public List<StatusPedido> proximosEstados() {
            return List.of(StatusPedido.ENTREGUE);
        }
    },
    ENTREGUE {
        public List<StatusPedido> proximosEstados() {
            return List.of();
        }
    };

    public abstract List<StatusPedido> proximosEstados();
}
