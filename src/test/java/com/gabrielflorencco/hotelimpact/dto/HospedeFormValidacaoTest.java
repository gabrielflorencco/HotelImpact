package com.gabrielflorencco.hotelimpact.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validações de formulário do hóspede executadas sem subir o Spring:
 * Bean Validation puro (mesmo mecanismo usado pelo @Valid no controller).
 */
class HospedeFormValidacaoTest {

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

    private HospedeForm formularioValido() {
        HospedeForm form = new HospedeForm();
        form.setNome("Ana Souza");
        form.setCpf("529.982.247-25");
        form.setEmail("ana@example.com");
        form.setTelefone("(11) 98888-0001");
        form.setDataNascimento(LocalDate.of(1990, 3, 14));
        return form;
    }

    @Test
    void formularioCompletoValido() {
        assertThat(validator.validate(formularioValido())).isEmpty();
    }

    @Test
    void camposOpcionaisVaziosSaoAceitos() {
        HospedeForm form = new HospedeForm();
        form.setNome("Bruno Lima");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void nomeObrigatorio() {
        HospedeForm form = formularioValido();
        form.setNome("   ");
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("nome"));
    }

    @Test
    void cpfInvalidoRejeitado() {
        HospedeForm form = formularioValido();
        form.setCpf("529.982.247-26");
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("cpf"));
    }

    @Test
    void cpfComLetrasRejeitado() {
        HospedeForm form = formularioValido();
        form.setCpf("abc123def00");
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("cpf"));
    }

    @Test
    void emailInvalidoRejeitado() {
        HospedeForm form = formularioValido();
        form.setEmail("sem-arroba");
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void telefoneInvalidoRejeitado() {
        HospedeForm form = formularioValido();
        form.setTelefone("1234");
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("telefone"));
    }

    @Test
    void telefoneSemDddEComNonoDigitoAceito() {
        HospedeForm form = formularioValido();
        form.setTelefone("98888-0001");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void dataNascimentoFuturaRejeitada() {
        HospedeForm form = formularioValido();
        form.setDataNascimento(LocalDate.now().plusDays(1));
        assertThat(validator.validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("dataNascimento"));
    }
}
