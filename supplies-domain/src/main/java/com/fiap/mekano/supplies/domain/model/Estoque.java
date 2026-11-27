package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Estoque {

    private final UUID id;
    private final UUID pecaUuid;
    private final long saldoAtual;
    private final long saldoReservado;
    private final Long estoqueMinimo;
    private final long version;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Estoque criar(UUID pecaUuid) {
        validarPecaUuid(pecaUuid);
        LocalDateTime agora = LocalDateTime.now();
        return Estoque.builder()
                .id(UUID.randomUUID())
                .pecaUuid(pecaUuid)
                .saldoAtual(0)
                .saldoReservado(0)
                .estoqueMinimo(null)
                .version(0)
                .createdAt(agora)
                .build();
    }

    public static Estoque reconstitute(UUID id, UUID pecaUuid, long saldoAtual, long saldoReservado,
                                      Long estoqueMinimo, long version, LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {
        validarId(id);
        validarPecaUuid(pecaUuid);
        validarSaldo(saldoAtual);
        validarSaldo(saldoReservado);
        validarMinimo(estoqueMinimo);
        if (version < 0) {
            throw new AppException(400, Messages.get("estoque.version.invalida"));
        }
        return Estoque.builder()
                .id(id)
                .pecaUuid(pecaUuid)
                .saldoAtual(saldoAtual)
                .saldoReservado(saldoReservado)
                .estoqueMinimo(estoqueMinimo)
                .version(version)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public long disponivel() {
        return saldoAtual - saldoReservado;
    }

    public boolean abaixoDoMinimo() {
        return estoqueMinimo != null && disponivel() <= estoqueMinimo;
    }

    public Estoque aplicar(TipoMovimentoEstoque tipo, long quantidade) {
        if (tipo == null) {
            throw new AppException(400, Messages.get("estoque.tipo.required"));
        }
        if (quantidade <= 0) {
            throw new AppException(400, Messages.get("estoque.quantidade.invalida"));
        }

        long novoSaldoAtual = saldoAtual;
        long novoSaldoReservado = saldoReservado;
        switch (tipo) {
            case CREDITO -> novoSaldoAtual = somar(saldoAtual, quantidade);
            case DEBITO -> novoSaldoAtual = subtrair(saldoAtual, quantidade, "estoque.saldo.insuficiente");
            case RESERVA -> novoSaldoReservado = somar(saldoReservado, quantidade);
            case LIBERACAO -> novoSaldoReservado = subtrair(
                    saldoReservado, quantidade, "estoque.reserva.insuficiente");
            case CONSUMO -> {
                novoSaldoAtual = subtrair(saldoAtual, quantidade, "estoque.saldo.insuficiente");
                novoSaldoReservado = subtrair(
                        saldoReservado, quantidade, "estoque.reserva.insuficiente");
            }
        }

        long novaVersao;
        try {
            novaVersao = Math.incrementExact(version);
        } catch (ArithmeticException ex) {
            throw new AppException(409, Messages.get("estoque.version.limite"));
        }

        return Estoque.builder()
                .id(id)
                .pecaUuid(pecaUuid)
                .saldoAtual(novoSaldoAtual)
                .saldoReservado(novoSaldoReservado)
                .estoqueMinimo(estoqueMinimo)
                .version(novaVersao)
                .createdAt(createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static long somar(long saldo, long quantidade) {
        try {
            return Math.addExact(saldo, quantidade);
        } catch (ArithmeticException ex) {
            throw new AppException(409, Messages.get("estoque.saldo.limite"));
        }
    }

    private static long subtrair(long saldo, long quantidade, String mensagem) {
        if (saldo < quantidade) {
            throw new AppException(409, Messages.get(mensagem));
        }
        return saldo - quantidade;
    }

    private static void validarId(UUID id) {
        if (id == null) {
            throw new AppException(400, Messages.get("estoque.uuid.required"));
        }
    }

    private static void validarPecaUuid(UUID pecaUuid) {
        if (pecaUuid == null) {
            throw new AppException(400, Messages.get("estoque.peca.required"));
        }
    }

    private static void validarSaldo(long saldo) {
        if (saldo < 0) {
            throw new AppException(400, Messages.get("estoque.saldo.invalido"));
        }
    }

    private static void validarMinimo(Long estoqueMinimo) {
        if (estoqueMinimo != null && estoqueMinimo < 0) {
            throw new AppException(400, Messages.get("estoque.minimo.invalido"));
        }
    }
}
