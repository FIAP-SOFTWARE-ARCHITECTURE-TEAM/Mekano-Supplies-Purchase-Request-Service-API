package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.StatusNfEntrada;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;
import com.fiap.mekano.supplies.domain.port.out.MovimentoEstoqueRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaFiltro;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.supplies.infrastructure.entity.RequisicaoCompraEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@QuarkusTest
@TestTransaction
@DisplayName("NfEntradaRepositoryImpl")
class NfEntradaRepositoryImplTest {

    private static final String CHAVE_A = "35200612345678000190550000001234567890123456";
    private static final String CHAVE_B = "35200612345678000190550000001234567890123457";

    @Inject
    NfEntradaRepositoryPort repository;

    @Inject
    MovimentoEstoqueRepositoryPort movimentoRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @DisplayName("salva, busca por UUID/chave e atualiza status para CANCELADA")
    void salvaBuscaEAtualiza() {
        UUID requisicaoUuid = criarRequisicao();
        NfEntrada nf = NfEntrada.criar(CHAVE_A, new BigDecimal("459.00"), requisicaoUuid);

        NfEntrada salva = repository.save(nf);

        assertThat(salva.getId()).isEqualTo(nf.getId());
        assertThat(salva.getStatus()).isEqualTo(StatusNfEntrada.ATIVA);
        assertThat(repository.findById(nf.getId())).isPresent()
                .get().extracting(NfEntrada::getId).isEqualTo(nf.getId());
        assertThat(repository.findByChaveAcesso(CHAVE_A)).isPresent()
                .get().extracting(NfEntrada::getChaveAcesso).isEqualTo(CHAVE_A);

        NfEntrada cancelada = repository.save(salva.cancelar("Duplicidade"));

        assertThat(cancelada.getStatus()).isEqualTo(StatusNfEntrada.CANCELADA);
        assertThat(cancelada.getMotivoCancelamento()).isEqualTo("Duplicidade");
        assertThat(cancelada.getDataCancelamento()).isNotNull();
        assertThat(repository.findById(nf.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusNfEntrada.CANCELADA);
    }

    @Test
    @DisplayName("filtra por status, requisicao e chave e conta os resultados")
    void filtraEConta() {
        UUID requisicaoA = criarRequisicao();
        UUID requisicaoB = criarRequisicao();
        NfEntrada ativa = repository.save(NfEntrada.criar(CHAVE_A, new BigDecimal("100.00"), requisicaoA));
        NfEntrada cancelada = repository.save(
                NfEntrada.criar(CHAVE_B, new BigDecimal("200.00"), requisicaoB).cancelar("Erro"));

        assertThat(repository.findAll(NfEntradaFiltro.vazio(), 0, 10)).hasSize(2);
        assertThat(repository.count(NfEntradaFiltro.vazio())).isEqualTo(2);
        assertThat(repository.findAll(new NfEntradaFiltro(StatusNfEntrada.ATIVA, null, null), 0, 10))
                .extracting(NfEntrada::getId).containsExactly(ativa.getId());
        assertThat(repository.findAll(new NfEntradaFiltro(null, requisicaoB, null), 0, 10))
                .extracting(NfEntrada::getId).containsExactly(cancelada.getId());
        assertThat(repository.findAll(new NfEntradaFiltro(null, null, CHAVE_A), 0, 10))
                .extracting(NfEntrada::getId).containsExactly(ativa.getId());
    }

    @Test
    @DisplayName("rejeita chave de acesso duplicada com 409")
    void rejeitaChaveDuplicada() {
        UUID requisicaoA = criarRequisicao();
        UUID requisicaoB = criarRequisicao();
        repository.save(NfEntrada.criar(CHAVE_A, new BigDecimal("100.00"), requisicaoA));
        entityManager.flush();

        NfEntrada duplicada = NfEntrada.criar(CHAVE_A, new BigDecimal("200.00"), requisicaoB);

        AppException ex = catchThrowableOfType(() -> repository.save(duplicada), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("busca movimentos pela UUID de referencia da NF")
    void buscaMovimentosPorReferencia() {
        UUID referencia = UUID.randomUUID();
        MovimentoEstoque movimento = movimentoRepository.save(MovimentoEstoque.criar(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                TipoMovimentoEstoque.CREDITO, 10L, 10L, 0L,
                OrigemMovimentoEstoque.NF_ENTRADA, referencia,
                "nf:%s:teste:CREDITO".formatted(referencia)));

        assertThat(movimentoRepository.findByReferenciaUuid(referencia))
                .extracting(MovimentoEstoque::getId).containsExactly(movimento.getId());
        assertThat(movimentoRepository.findByReferenciaUuid(UUID.randomUUID())).isEmpty();
    }

    private UUID criarRequisicao() {
        RequisicaoCompraEntity requisicao = new RequisicaoCompraEntity();
        requisicao.setUuid(UUID.randomUUID());
        requisicao.setStatus("PRODUTO_RECEBIDO");
        requisicao.setMotivo("ESTOQUE_MINIMO");
        entityManager.persist(requisicao);
        entityManager.flush();
        return requisicao.getUuid();
    }
}
