package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nota fiscal de entrada de mercadorias.
 *
 * <p>Regras da Sprint 3:</p>
 * <ul>
 *   <li>chave de acesso com exatamente 44 digitos;</li>
 *   <li>valor total maior que zero;</li>
 *   <li>vinculada a uma requisicao de compra;</li>
 *   <li>status {@link StatusNfEntrada#ATIVA} ou {@link StatusNfEntrada#CANCELADA};</li>
 *   <li>cancelamento somente a partir de {@link StatusNfEntrada#ATIVA}.</li>
 * </ul>
 *
 * <p>Os itens da NF sao derivados da requisicao de compra; por isso nao existem
 * itens persistidos neste agregado.</p>
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class NfEntrada {

    private static final int CHAVE_ACESSO_LENGTH = 44;
    private static final int MOTIVO_CANCELAMENTO_MAX_LENGTH = 255;

    private final UUID id;
    private final String chaveAcesso;
    private final BigDecimal valorTotal;
    private final UUID requisicaoUuid;
    private final StatusNfEntrada status;
    private final String motivoCancelamento;
    private final LocalDateTime dataCancelamento;
    private final LocalDateTime createdAt;

    public static NfEntrada criar(String chaveAcesso, BigDecimal valorTotal, UUID requisicaoUuid) {
        String chaveNormalizada = validarChaveAcesso(chaveAcesso);
        BigDecimal valorNormalizado = validarValorTotal(valorTotal);
        validarRequisicaoUuid(requisicaoUuid);

        return NfEntrada.builder()
                .id(UUID.randomUUID())
                .chaveAcesso(chaveNormalizada)
                .valorTotal(valorNormalizado)
                .requisicaoUuid(requisicaoUuid)
                .status(StatusNfEntrada.ATIVA)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static NfEntrada reconstitute(UUID id, String chaveAcesso, BigDecimal valorTotal,
                                         UUID requisicaoUuid, StatusNfEntrada status,
                                         String motivoCancelamento, LocalDateTime dataCancelamento,
                                         LocalDateTime createdAt) {
        validarId(id);
        String chaveNormalizada = validarChaveAcesso(chaveAcesso);
        BigDecimal valorNormalizado = validarValorTotal(valorTotal);
        validarRequisicaoUuid(requisicaoUuid);
        validarStatus(status);
        validarMotivoCancelamentoOpcional(motivoCancelamento);

        return NfEntrada.builder()
                .id(id)
                .chaveAcesso(chaveNormalizada)
                .valorTotal(valorNormalizado)
                .requisicaoUuid(requisicaoUuid)
                .status(status)
                .motivoCancelamento(motivoCancelamento)
                .dataCancelamento(dataCancelamento)
                .createdAt(createdAt)
                .build();
    }

    public NfEntrada cancelar(String motivo) {
        if (status != StatusNfEntrada.ATIVA) {
            throw new AppException(409, Messages.get("nf_entrada.cancelamento.invalido", status.name()));
        }
        return NfEntrada.builder()
                .id(id)
                .chaveAcesso(chaveAcesso)
                .valorTotal(valorTotal)
                .requisicaoUuid(requisicaoUuid)
                .status(StatusNfEntrada.CANCELADA)
                .motivoCancelamento(validarMotivoCancelamento(motivo))
                .dataCancelamento(LocalDateTime.now())
                .createdAt(createdAt)
                .build();
    }

    private static String validarChaveAcesso(String chaveAcesso) {
        if (chaveAcesso == null || chaveAcesso.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.chave_acesso.required"));
        }
        String chave = chaveAcesso.trim();
        if (!chave.matches("\\d{" + CHAVE_ACESSO_LENGTH + "}")) {
            throw new AppException(400, Messages.get("nf_entrada.chave_acesso.invalid", chaveAcesso));
        }
        return chave;
    }

    private static BigDecimal validarValorTotal(BigDecimal valorTotal) {
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, Messages.get("nf_entrada.valor_total.invalido"));
        }
        return valorTotal.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validarRequisicaoUuid(UUID requisicaoUuid) {
        if (requisicaoUuid == null) {
            throw new AppException(400, Messages.get("nf_entrada.requisicao.required"));
        }
    }

    private static void validarId(UUID id) {
        if (id == null) {
            throw new AppException(400, Messages.get("nf_entrada.id.required"));
        }
    }

    private static void validarStatus(StatusNfEntrada status) {
        if (status == null) {
            throw new AppException(400, Messages.get("nf_entrada.status.required"));
        }
    }

    private static String validarMotivoCancelamento(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.motivo_cancelamento.required"));
        }
        String motivoNormalizado = motivo.trim();
        validarTamanhoMotivo(motivoNormalizado);
        return motivoNormalizado;
    }

    private static void validarMotivoCancelamentoOpcional(String motivo) {
        if (motivo != null) {
            validarTamanhoMotivo(motivo);
        }
    }

    private static void validarTamanhoMotivo(String motivo) {
        if (motivo.length() > MOTIVO_CANCELAMENTO_MAX_LENGTH) {
            throw new AppException(400, Messages.get("nf_entrada.motivo_cancelamento.invalido"));
        }
    }
}
