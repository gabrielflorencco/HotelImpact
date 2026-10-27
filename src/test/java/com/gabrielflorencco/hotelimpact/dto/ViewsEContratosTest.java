package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.validation.Cpf;
import com.gabrielflorencco.hotelimpact.validation.CpfValidator;
import jakarta.validation.ConstraintValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura direta das projeções de leitura (classes final) e do contrato
 * polimórfico do validador de CPF.
 *
 * Dois pontos de POO exercitados aqui:
 * 1. Views imutáveis construídas por fábrica estática (de) — o mapeamento e
 *    a máscara de privacidade eram cobertos apenas indiretamente pelos
 *    testes web;
 * 2. Upcasting + polimorfismo de subtipos: o validador é exercido por uma
 *    referência ao contrato ConstraintValidator — o chamador não conhece a
 *    classe concreta, e o despacho dinâmico decide a implementação.
 */
class ViewsEContratosTest {

    @Test
    @DisplayName("HospedeResumoView de(hóspede) mapeia os campos e mascara o CPF")
    void hospedeResumoMapeiaCamposEMascaraCpf() {
        Hospede hospede = new Hospede();
        hospede.setNome("Ana Souza");
        hospede.setCpf("52998224725");
        hospede.setEmail("ana.souza@example.com");
        hospede.setTelefone("(11) 98888-0001");
        hospede.setDataNascimento(LocalDate.of(1990, 3, 14));

        HospedeResumoView view = HospedeResumoView.de(hospede);

        assertThat(view.getId()).isNull();
        assertThat(view.getNome()).isEqualTo("Ana Souza");
        assertThat(view.getCpfMascarado()).isEqualTo("•••.•••.•••-725");
        assertThat(view.getEmail()).isEqualTo("ana.souza@example.com");
        assertThat(view.getTelefone()).isEqualTo("(11) 98888-0001");
        assertThat(view.getDataNascimento()).isEqualTo(LocalDate.of(1990, 3, 14));
    }

    @Test
    @DisplayName("Máscara preserva a privacidade: somente os três dígitos finais aparecem")
    void mascaraExpoeSomenteTresDigitosFinais() {
        assertThat(HospedeResumoView.mascararCpf("52998224725")).isEqualTo("•••.•••.•••-725");
        assertThat(HospedeResumoView.mascararCpf("12345678909")).isEqualTo("•••.•••.•••-909");
        // CPF ausente ou em formato inesperado não gera máscara parcial
        assertThat(HospedeResumoView.mascararCpf(null)).isNull();
        assertThat(HospedeResumoView.mascararCpf("123")).isNull();
    }

    @Test
    @DisplayName("Polimorfismo de subtipos: o contrato ConstraintValidator despacha para a implementação concreta")
    void validadorExercidoPelaReferenciaDoContrato() {
        // Upcasting: a instância concreta é atribuída à referência da interface.
        ConstraintValidator<Cpf, String> contrato = new CpfValidator();

        // O chamador (aqui, o teste; no sistema, o Bean Validation) só conhece
        // o contrato — o despacho dinâmico invoca a implementação real.
        assertThat(contrato.isValid("529.982.247-25", null)).isTrue();
        assertThat(contrato.isValid("529.982.247-26", null)).isFalse();
        assertThat(contrato.isValid(null, null)).isTrue(); // opcional: @NotBlank cuida da obrigatoriedade
    }
}
