package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.MotivoRequisicao;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraFiltro;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("RequisicaoCompraRepositoryImpl")
class RequisicaoCompraRepositoryImplTest {

    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Inject
    RequisicaoCompraRepositoryImpl repository;

    @Test
    @TestTransaction
    @DisplayName("save persiste nova requisicao com itens e snapshots")
    void savePersisteNovaRequisicao() {
        RequisicaoCompra requisicao = RequisicaoCompra.criar(
                List.of(item(PECA_A, 10L), item(PECA_B, 3L)), "Reposicao de estoque");

        RequisicaoCompra salva = repository.save(requisicao);

        assertThat(salva.getId()).isEqualTo(requisicao.getId());
        assertThat(salva.getStatus()).isEqualTo(StatusRequisicao.ABERTA);
        assertThat(salva.getMotivo()).isEqualTo(MotivoRequisicao.ESTOQUE_MINIMO);
        assertThat(salva.getObservacao()).isEqualTo("Reposicao de estoque");
        assertThat(salva.getItens()).hasSize(2);
        assertThat(salva.getItens())
                .extracting(ItemRequisicaoCompra::getPecaUuid)
                .containsExactlyInAnyOrder(PECA_A, PECA_B);
        assertThat(salva.getItens())
                .allSatisfy(item -> {
                    assertThat(item.getPecaCodigo()).isNotBlank();
                    assertThat(item.getPecaDescricao()).isNotBlank();
                    assertThat(item.getValorUnitario()).isPositive();
                });
    }

    @Test
    @TestTransaction
    @DisplayName("save atualiza status, motivo e itens de requisicao existente")
    void saveAtualizaRequisicaoExistente() {
        RequisicaoCompra salva = repository.save(RequisicaoCompra.criar(
                List.of(item(PECA_A, 5L)), "inicial"));

        UUID itemId = salva.getItens().get(0).getId();
        ItemRequisicaoCompra itemAtualizado = ItemRequisicaoCompra.reconstitute(
                itemId, PECA_A, "PEA-001", "Filtro de Oleo", 20L, new BigDecimal("49.90"));

        RequisicaoCompra atualizada = RequisicaoCompra.reconstitute(
                salva.getId(), List.of(itemAtualizado), StatusRequisicao.ENVIADA,
                MotivoRequisicao.ORDEM_SERVICO, "atualizada", salva.getCreatedAt());

        RequisicaoCompra resultado = repository.save(atualizada);

        assertThat(resultado.getId()).isEqualTo(salva.getId());
        assertThat(resultado.getStatus()).isEqualTo(StatusRequisicao.ENVIADA);
        assertThat(resultado.getMotivo()).isEqualTo(MotivoRequisicao.ORDEM_SERVICO);
        assertThat(resultado.getObservacao()).isEqualTo("atualizada");
        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getQuantidade()).isEqualTo(20L);
        assertThat(resultado.getItens().get(0).getValorUnitario()).isEqualByComparingTo("49.90");
    }

    @Test
    @TestTransaction
    @DisplayName("save remove itens ausentes na atualizacao")
    void saveRemoveItensAusentes() {
        RequisicaoCompra salva = repository.save(RequisicaoCompra.criar(
                List.of(item(PECA_A, 5L), item(PECA_B, 2L)), null));

        ItemRequisicaoCompra itemMantido = salva.getItens().stream()
                .filter(item -> item.getPecaUuid().equals(PECA_A))
                .findFirst()
                .orElseThrow();

        RequisicaoCompra atualizada = RequisicaoCompra.reconstitute(
                salva.getId(), List.of(itemMantido), StatusRequisicao.ABERTA,
                MotivoRequisicao.ESTOQUE_MINIMO, null, salva.getCreatedAt());

        RequisicaoCompra resultado = repository.save(atualizada);

        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getPecaUuid()).isEqualTo(PECA_A);
    }

    @Test
    @TestTransaction
    @DisplayName("findById retorna requisicao existente com itens")
    void findByIdRetornaRequisicao() {
        RequisicaoCompra salva = repository.save(RequisicaoCompra.criar(
                List.of(item(PECA_A, 10L)), null));

        Optional<RequisicaoCompra> encontrada = repository.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getItens()).hasSize(1);
        assertThat(encontrada.get().getItens().get(0).getQuantidade()).isEqualTo(10L);
    }

    @Test
    @TestTransaction
    @DisplayName("findById retorna vazio para UUID inexistente ou nulo")
    void findByIdRetornaVazio() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findAll filtra por status, motivo e peca")
    void findAllFiltra() {
        repository.save(RequisicaoCompra.criar(List.of(item(PECA_A, 1L)), null));
        repository.save(RequisicaoCompra.reconstitute(
                UUID.randomUUID(), List.of(item(PECA_B, 2L)), StatusRequisicao.ENVIADA,
                MotivoRequisicao.ORDEM_SERVICO, null, LocalDateTime.now()));

        var porStatus = repository.findAll(
                new RequisicaoCompraFiltro(StatusRequisicao.ENVIADA, null, null), 0, 10);
        var porMotivo = repository.findAll(
                new RequisicaoCompraFiltro(null, MotivoRequisicao.ESTOQUE_MINIMO, null), 0, 10);
        var porPeca = repository.findAll(
                new RequisicaoCompraFiltro(null, null, PECA_B), 0, 10);

        assertThat(porStatus).hasSize(1);
        assertThat(porStatus.get(0).getStatus()).isEqualTo(StatusRequisicao.ENVIADA);
        assertThat(porMotivo).hasSize(1);
        assertThat(porMotivo.get(0).getMotivo()).isEqualTo(MotivoRequisicao.ESTOQUE_MINIMO);
        assertThat(porPeca).hasSize(1);
        assertThat(porPeca.get(0).getItens())
                .extracting(ItemRequisicaoCompra::getPecaUuid)
                .containsExactly(PECA_B);
    }

    @Test
    @TestTransaction
    @DisplayName("findAll pagina e ordena por id decrescente")
    void findAllPagina() {
        for (int i = 0; i < 3; i++) {
            repository.save(RequisicaoCompra.criar(List.of(item(PECA_A, 1L + i)), null));
        }

        var primeiraPagina = repository.findAll(RequisicaoCompraFiltro.vazio(), 0, 2);
        var segundaPagina = repository.findAll(RequisicaoCompraFiltro.vazio(), 1, 2);

        assertThat(primeiraPagina).hasSize(2);
        assertThat(segundaPagina).hasSize(1);
        assertThat(primeiraPagina.get(0).getId()).isNotEqualTo(primeiraPagina.get(1).getId());
    }

    @Test
    @TestTransaction
    @DisplayName("count respeita os filtros informados")
    void countRespeitaFiltros() {
        repository.save(RequisicaoCompra.criar(List.of(item(PECA_A, 1L)), null));
        repository.save(RequisicaoCompra.reconstitute(
                UUID.randomUUID(), List.of(item(PECA_B, 2L)), StatusRequisicao.CANCELADA,
                MotivoRequisicao.ORDEM_SERVICO, null, LocalDateTime.now()));

        long total = repository.count(RequisicaoCompraFiltro.vazio());
        long abertas = repository.count(
                new RequisicaoCompraFiltro(StatusRequisicao.ABERTA, null, null));
        long porPeca = repository.count(
                new RequisicaoCompraFiltro(null, null, PECA_B));

        assertThat(total).isEqualTo(2);
        assertThat(abertas).isEqualTo(1);
        assertThat(porPeca).isEqualTo(1);
    }

    private static ItemRequisicaoCompra item(UUID pecaUuid, long quantidade) {
        return ItemRequisicaoCompra.criar(
                pecaUuid, "PEA-001", "Filtro de Oleo", quantidade, new BigDecimal("45.90"));
    }
}
