package com.fiap.mekano.supplies.application.service;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.Estoque;
import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.port.in.EstoqueServicePort;
import com.fiap.mekano.supplies.domain.port.in.MovimentarEstoqueCommand;
import com.fiap.mekano.supplies.domain.port.in.MovimentoEstoqueResult;
import com.fiap.mekano.supplies.domain.port.out.EstoqueRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.MovimentoEstoqueRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class EstoqueService implements EstoqueServicePort {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EstoqueRepositoryPort estoqueRepository;
    private final MovimentoEstoqueRepositoryPort movimentoRepository;

    public EstoqueService(EstoqueRepositoryPort estoqueRepository,
                         MovimentoEstoqueRepositoryPort movimentoRepository) {
        this.estoqueRepository = estoqueRepository;
        this.movimentoRepository = movimentoRepository;
    }

    @Override
    @Transactional
    public Estoque obterOuCriar(UUID pecaUuid) {
        validarPecaUuid(pecaUuid);
        return estoqueRepository.findOrCreateAndLock(pecaUuid);
    }

    @Override
    @Transactional
    public Estoque buscar(UUID pecaUuid) {
        validarPecaUuid(pecaUuid);
        return estoqueRepository.findOrCreateAndLock(pecaUuid);
    }

    @Override
    public PageResult<Estoque> listarAbaixoMinimo(int page, int size) {
        int pageNormalizada = Math.max(page, 0);
        int sizeNormalizado = normalizarTamanho(size);
        var content = estoqueRepository.findAbaixoMinimo(pageNormalizada, sizeNormalizado);
        long total = estoqueRepository.countAbaixoMinimo();
        return PageResult.of(content, pageNormalizada, sizeNormalizado, total);
    }

    @Override
    @Transactional
    public MovimentoEstoqueResult movimentar(MovimentarEstoqueCommand command) {
        validarCommand(command);
        String idempotencyKey = command.idempotencyKey().trim();

        var existente = movimentoRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return duplicata(existente.get(), command);
        }

        Estoque estoque = estoqueRepository.findOrCreateAndLock(command.pecaUuid());

        existente = movimentoRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return duplicata(existente.get(), command);
        }

        Estoque atualizado = estoque.aplicar(command.tipo(), command.quantidade());
        MovimentoEstoque movimento = MovimentoEstoque.criar(
                command.pecaUuid(),
                command.tipo(),
                command.quantidade(),
                atualizado.getSaldoAtual(),
                atualizado.getSaldoReservado(),
                command.origem(),
                command.referenciaUuid(),
                idempotencyKey);

        estoqueRepository.save(atualizado);
        MovimentoEstoque salvo = movimentoRepository.save(movimento);
        Log.infof("Movimento de estoque registrado: pecaUuid=%s, tipo=%s, quantidade=%s, saldoAtual=%s, saldoReservado=%s",
                salvo.getPecaUuid(), salvo.getTipo(), salvo.getQuantidade(),
                salvo.getSaldoAtualApos(), salvo.getSaldoReservadoApos());
        return new MovimentoEstoqueResult(salvo, false);
    }

    private MovimentoEstoqueResult duplicata(MovimentoEstoque existente, MovimentarEstoqueCommand command) {
        boolean mesmaOperacao = existente.getPecaUuid().equals(command.pecaUuid())
                && existente.getTipo() == command.tipo()
                && existente.getQuantidade() == command.quantidade()
                && existente.getOrigem() == command.origem()
                && Objects.equals(existente.getReferenciaUuid(), command.referenciaUuid());
        if (!mesmaOperacao) {
            Log.warn("Chave de idempotencia reutilizada com dados diferentes");
            throw new AppException(409, Messages.get("estoque.idempotency.conflito"));
        }
        Log.infof("Movimento de estoque repetido: movimentoUuid=%s, pecaUuid=%s",
                existente.getId(), existente.getPecaUuid());
        return new MovimentoEstoqueResult(existente, true);
    }

    private static void validarCommand(MovimentarEstoqueCommand command) {
        if (command == null) {
            throw new AppException(400, Messages.get("estoque.movimento.required"));
        }
        validarPecaUuid(command.pecaUuid());
        if (command.tipo() == null) {
            throw new AppException(400, Messages.get("estoque.tipo.required"));
        }
        if (command.quantidade() == null || command.quantidade() <= 0) {
            throw new AppException(400, Messages.get("estoque.quantidade.invalida"));
        }
        if (command.origem() == null) {
            throw new AppException(400, Messages.get("estoque.origem.required"));
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                || command.idempotencyKey().length() > 200) {
            throw new AppException(400, Messages.get("estoque.idempotency.required"));
        }
    }

    private static void validarPecaUuid(UUID pecaUuid) {
        if (pecaUuid == null) {
            throw new AppException(400, Messages.get("estoque.peca.required"));
        }
    }

    private static int normalizarTamanho(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
