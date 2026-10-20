package com.gabrielflorencco.hotelimpact.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void aceitaNuloOuVazioPorSerCampoOpcional() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void aceitaCpfValidoSomenteComDigitos() {
        assertThat(validator.isValid("52998224725", null)).isTrue();
    }

    @Test
    void aceitaCpfValidoComPontuacao() {
        assertThat(validator.isValid("529.982.247-25", null)).isTrue();
    }

    @Test
    void rejeitaCpfComTamanhoErrado() {
        assertThat(validator.isValid("5299822472", null)).isFalse();
        assertThat(validator.isValid("529982247255", null)).isFalse();
    }

    @Test
    void rejeitaCpfComTodosOsDigitosIguais() {
        assertThat(validator.isValid("11111111111", null)).isFalse();
        assertThat(validator.isValid("000.000.000-00", null)).isFalse();
    }

    @Test
    void rejeitaCpfComDigitoVerificadorInvalido() {
        assertThat(validator.isValid("52998224726", null)).isFalse();
        assertThat(validator.isValid("52998224735", null)).isFalse();
    }

    @Test
    void rejeitaCpfComLetrasOuSimbolosAMais() {
        assertThat(validator.isValid("529.982.247-2X", null)).isFalse();
        assertThat(validator.isValid("abc", null)).isFalse();
    }
}
