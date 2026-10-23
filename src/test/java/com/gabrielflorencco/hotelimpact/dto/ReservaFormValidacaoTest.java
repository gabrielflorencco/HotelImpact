package com.gabrielflorencco.hotelimpact.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validações de formulário da reserva executadas sem subir o Spring:
 * Bean Validation puro (mesmo mecanismo usado pelo @Valid no controller).
 * As regras que dependem de contexto (período, capacidade, disponibilidade,
 * limite do desconto e exceção de datas passadas) ficam em ReservasIntegracaoTest.
 */
class ReservaFormValidacaoTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void fecharFabrica() {
        fabrica.close();
    }

    private ReservaForm formularioValido() {
        ReservaForm form = new ReservaForm();
        form.setHospedeId(1L);
        form.setQuartoId(1L);
        form.setDataCheckIn(LocalDate.now().plusDays(7));
        form.setDataCheckOut(LocalDate.now().plusDays(9));
        form.setQuantidadeHospedes(2);
        form.setValorDiaria(new BigDecimal("180.00"));
        return form;
    }

    @Test
    void formularioCompletoValido() {
        assertThat(validator.validate(formularioValido())).isEmpty();
    }

    @Test
    void descontoOpcionalAceitaNuloOuZero() {
        ReservaForm semDesconto = formularioValido();
        semDesconto.setDesconto(null);
        assertThat(validator.validate(semDesconto)).isEmpty();

        ReservaForm descontoZero = formularioValido();
        descontoZero.setDesconto(BigDecimal.ZERO);
        assertThat(validator.validate(descontoZero)).isEmpty();
    }

    @Test
    void hospedeObrigatorio() {
        ReservaForm form = formularioValido();
        form.setHospedeId(null);
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("hospedeId"));
    }

    @Test
    void quartoObrigatorio() {
        ReservaForm form = formularioValido();
        form.setQuartoId(null);
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("quartoId"));
    }

    @Test
    void datasObrigatorias() {
        ReservaForm form = formularioValido();
        form.setDataCheckIn(null);
        form.setDataCheckOut(null);
        assertThat(validator.validate(form))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("dataCheckIn", "dataCheckOut");
    }

    @Test
    void quantidadeDeHospedesEntreUmEVinte() {
        ReservaForm nula = formularioValido();
        nula.setQuantidadeHospedes(null);
        assertThat(validator.validate(nula)).anyMatch(v -> v.getPropertyPath().toString().equals("quantidadeHospedes"));

        ReservaForm zero = formularioValido();
        zero.setQuantidadeHospedes(0);
        assertThat(validator.validate(zero)).anyMatch(v -> v.getPropertyPath().toString().equals("quantidadeHospedes"));

        ReservaForm exagerada = formularioValido();
        exagerada.setQuantidadeHospedes(21);
        assertThat(validator.validate(exagerada)).anyMatch(v -> v.getPropertyPath().toString().equals("quantidadeHospedes"));
    }

    @Test
    void diariaDeveSerPositiva() {
        ReservaForm form = formularioValido();
        form.setValorDiaria(BigDecimal.ZERO);
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("valorDiaria"));
    }

    @Test
    void descontoNegativoRejeitado() {
        ReservaForm form = formularioValido();
        form.setDesconto(new BigDecimal("-0.01"));
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("desconto"));
    }

    @Test
    void camposTextoRespeitamTamanhoMaximo() {
        ReservaForm form = formularioValido();
        form.setMotivoExcecao("m".repeat(301));
        form.setObservacoes("o".repeat(501));
        assertThat(validator.validate(form))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("motivoExcecao", "observacoes");
    }
}
